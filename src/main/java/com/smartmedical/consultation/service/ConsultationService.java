package com.smartmedical.consultation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
import com.smartmedical.consultation.entity.Conversation;
import com.smartmedical.consultation.entity.Message;
import com.smartmedical.consultation.mapper.ConversationMapper;
import com.smartmedical.consultation.mapper.MessageMapper;
import com.smartmedical.health.entity.HealthRecord;
import com.smartmedical.health.mapper.HealthRecordMapper;
import com.smartmedical.llm.DeepSeekClient;
import com.smartmedical.medical.entity.MedicalRecord;
import com.smartmedical.medical.mapper.MedicalRecordMapper;
import com.smartmedical.medication.entity.Drug;
import com.smartmedical.medication.mapper.DrugMapper;
import com.smartmedical.family.entity.FamilyMember;
import com.smartmedical.family.mapper.FamilyMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConversationMapper convMapper;
    private final MessageMapper msgMapper;
    private final DeepSeekClient deepSeek;
    private final FamilyMemberMapper memberMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final DrugMapper drugMapper;

    private static final String SYSTEM_PROMPT = """
        你是一个专业的AI医疗问诊助手。请严格遵守以下规则：
        1. 你不是医生，不能给出确诊诊断
        2. 每次只问一个问题，逐步收集症状信息（部位、时长、程度、伴随症状、既往病史）
        3. 遇到紧急情况（胸痛、呼吸困难、大出血、意识模糊等）立即终止问诊，建议拨打120
        4. 不推荐具体药品名称，只说"可能需要XX类药物，请咨询医生后使用"
        5. 每次回复结尾追问一个澄清问题
        6. 每次回复末尾附加：\\n\\n---\\n*本回复仅为AI分析参考，不构成医疗诊断，请遵医嘱。*
        """;

    private static final Set<String> EMERGENCY_KEYWORDS = Set.of(
            "胸痛", "胸闷", "呼吸困难", "喘不上气", "大出血", "吐血", "咳血",
            "意识模糊", "昏迷", "晕倒", "抽搐", "剧烈头痛", "心慌", "心悸"
    );

    @Transactional
    public Map<String, Object> chat(Long userId, String message, Long conversationId, Long patientId) {
        for (String kw : EMERGENCY_KEYWORDS) {
            if (message.contains(kw)) {
                Map<String, Object> result = new HashMap<>();
                result.put("reply", "⚠️ 检测到您描述的" + kw + "属于紧急症状，请立即拨打120或前往最近的医院急诊科。AI问诊无法处理紧急情况。");
                result.put("conversationId", conversationId);
                return result;
            }
        }

        Conversation conv;
        if (conversationId != null) {
            conv = convMapper.selectById(conversationId);
            if (conv == null || !conv.getUserId().equals(userId))
                throw new BusinessException(ErrorCode.NOT_FOUND);
        } else {
            conv = new Conversation();
            conv.setUserId(userId);
            conv.setTitle(message.length() > 30 ? message.substring(0, 30) : message);
            conv.setStatus("active");
            convMapper.insert(conv);
            conversationId = conv.getId();
        }

        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        msgMapper.insert(userMsg);

        String medicalContext = buildMedicalContext(userId, patientId);

        List<Message> history = msgMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt));

        StringBuilder context = new StringBuilder();
        if (!medicalContext.isEmpty()) context.append(medicalContext).append("\n\n");
        for (Message m : history) {
            if (m.getId().equals(userMsg.getId())) continue;
            context.append(m.getRole().equals("user") ? "用户：" : "AI：")
                   .append(m.getContent()).append("\n");
        }

        String systemPrompt = SYSTEM_PROMPT;
        if (!medicalContext.isEmpty()) {
            systemPrompt += "\n\n以下是该家庭成员的健康档案，请在问诊时结合这些信息：\n" + medicalContext;
            systemPrompt += "\n在回复时可以引用相关历史记录。\n请给出较全面的分析，不必限制字数。";
        }

        String fullPrompt = context + "用户：" + message;
        String reply = deepSeek.chat(systemPrompt, fullPrompt);

        Message aiMsg = new Message();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("ai");
        aiMsg.setContent(reply);
        msgMapper.insert(aiMsg);

        conv.setUpdatedAt(LocalDateTime.now());
        convMapper.updateById(conv);

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("conversationId", conversationId);
        return result;
    }

    private String buildMedicalContext(Long userId, Long patientId) {
        if (patientId == null || patientId == 0) return "";
        StringBuilder ctx = new StringBuilder();

        // 1. 成员信息
        FamilyMember m = memberMapper.selectById(patientId);
        if (m != null) {
            ctx.append("【成员】").append(m.getName());
            if (m.getBirthDate() != null) {
                int age = java.time.Period.between(m.getBirthDate(), LocalDateTime.now().toLocalDate()).getYears();
                ctx.append("，").append(age).append("岁");
            }
            ctx.append("，").append(m.getGender() == 1 ? "男" : m.getGender() == 2 ? "女" : "未知").append("\n");
        }

        // 2. 病历
        List<MedicalRecord> records = medicalRecordMapper.selectList(
                new LambdaQueryWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getUserId, userId)
                        .eq(MedicalRecord::getPatientId, patientId)
                        .eq(MedicalRecord::getParseStatus, "done")
                        .orderByDesc(MedicalRecord::getRecordDate).last("LIMIT 5"));
        if (!records.isEmpty()) {
            ctx.append("【历史病历】\n");
            for (MedicalRecord r : records) {
                String tip = extractSummary(r.getParsedData());
                ctx.append("- ").append(r.getFileType().equals("pdf") ? "PDF" : "图片");
                if (r.getRecordDate() != null) ctx.append("(").append(r.getRecordDate()).append(")");
                if (!tip.isEmpty()) ctx.append(": ").append(tip);
                ctx.append("\n");
            }
        }

        // 3. 健康指标
        List<HealthRecord> health = healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getUserId, userId)
                        .eq(HealthRecord::getMemberId, patientId)
                        .orderByDesc(HealthRecord::getRecordedAt).last("LIMIT 10"));
        if (!health.isEmpty()) {
            ctx.append("【近期健康指标】\n");
            for (HealthRecord h : health) {
                ctx.append("- ").append(labelOf(h.getType())).append(": ").append(h.getValue());
                if (h.getUnit() != null) ctx.append(h.getUnit());
                ctx.append(" (").append(h.getRecordedAt().toLocalDate()).append(")\n");
            }
        }

        // 4. 用药
        List<Drug> drugs = drugMapper.selectList(
                new LambdaQueryWrapper<Drug>().eq(Drug::getUserId, userId));
        if (!drugs.isEmpty()) {
            ctx.append("【当前用药】\n");
            for (Drug d : drugs) {
                ctx.append("- ").append(d.getName());
                if (d.getDose() != null) ctx.append(" ").append(d.getDose());
                if (d.getFreq() != null) ctx.append(" ").append(d.getFreq());
                ctx.append("\n");
            }
        }

        return ctx.isEmpty() ? "暂无相关健康记录。\n" : ctx.toString();
    }

    private String extractSummary(String parsedData) {
        if (parsedData == null) return "";
        try {
            for (String key : new String[]{"summary", "analysis"}) {
                int i = parsedData.indexOf("\"" + key + "\":\"");
                if (i < 0) continue;
                i = parsedData.indexOf("\"", i + key.length() + 3) + 1;
                int end = parsedData.indexOf("\"", i);
                if (end < 0) continue;
                String s = parsedData.substring(i, end);
                return s.length() > 80 ? s.substring(0, 80) + "..." : s;
            }
        } catch (Exception e) {}
        return "";
    }

    private String labelOf(String dbType) {
        return switch (dbType) {
            case "blood_pressure" -> "血压";
            case "glucose" -> "血糖";
            case "SpO2" -> "血氧";
            case "heart_rate" -> "心率";
            case "weight" -> "体重";
            default -> dbType;
        };
    }

    public List<Map<String, Object>> getHistory(Long userId) {
        List<Conversation> convs = convMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation c : convs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("symptom", c.getTitle());
            item.put("date", c.getUpdatedAt() != null
                    ? c.getUpdatedAt().toLocalDate().toString()
                    : c.getCreatedAt().toLocalDate().toString());
            item.put("time", c.getUpdatedAt() != null
                    ? c.getUpdatedAt().toLocalTime().toString().substring(0, 5)
                    : c.getCreatedAt().toLocalTime().toString().substring(0, 5));
            item.put("severity", "mild");
            result.add(item);
        }
        return result;
    }
}
