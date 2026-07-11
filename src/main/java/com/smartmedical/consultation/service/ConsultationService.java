package com.smartmedical.consultation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
import com.smartmedical.consultation.entity.Conversation;
import com.smartmedical.consultation.entity.Message;
import com.smartmedical.consultation.mapper.ConversationMapper;
import com.smartmedical.consultation.mapper.MessageMapper;
import com.smartmedical.llm.DeepSeekClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConversationMapper convMapper;
    private final MessageMapper msgMapper;
    private final DeepSeekClient deepSeek;

    private static final String SYSTEM_PROMPT = """
        你是一个专业的AI医疗问诊助手。请严格遵守以下规则：
        1. 你不是医生，不能给出确诊诊断
        2. 每次只问一个问题，逐步收集症状信息（部位、时长、程度、伴随症状、既往病史）
        3. 遇到紧急情况（胸痛、呼吸困难、大出血、意识模糊等）立即终止问诊，建议拨打120
        4. 不推荐具体药品名称，只说"可能需要XX类药物，请咨询医生后使用"
        5. 每次回复结尾追问一个澄清问题
        6. 每次回复不超过200字
        7. 每次回复末尾附加：\n\n---\n*本回复仅为AI分析参考，不构成医疗诊断，请遵医嘱。*
        """;

    private static final Set<String> EMERGENCY_KEYWORDS = Set.of(
            "胸痛", "胸闷", "呼吸困难", "喘不上气", "大出血", "吐血", "咳血",
            "意识模糊", "昏迷", "晕倒", "抽搐", "剧烈头痛", "心慌", "心悸"
    );

    @Transactional
    public Map<String, Object> chat(Long userId, String message, Long conversationId) {
        // 紧急关键词检测
        for (String kw : EMERGENCY_KEYWORDS) {
            if (message.contains(kw)) {
                Map<String, Object> result = new HashMap<>();
                result.put("reply", "⚠️ 检测到您描述的" + kw + "属于紧急症状，请立即拨打120或前往最近的医院急诊科。AI问诊无法处理紧急情况。");
                result.put("conversationId", conversationId);
                return result;
            }
        }

        // 创建或获取对话
        Conversation conv;
        if (conversationId != null) {
            conv = convMapper.selectById(conversationId);
            if (conv == null || !conv.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
        } else {
            conv = new Conversation();
            conv.setUserId(userId);
            conv.setTitle(message.length() > 30 ? message.substring(0, 30) : message);
            conv.setStatus("active");
            convMapper.insert(conv);
            conversationId = conv.getId();
        }

        // 保存用户消息
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        msgMapper.insert(userMsg);

        // 加载历史消息（最近10轮）
        List<Message> history = msgMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt));

        // 构建上下文
        StringBuilder context = new StringBuilder();
        for (Message m : history) {
            if (m.getId().equals(userMsg.getId())) continue; // 跳过刚插入的
            context.append(m.getRole().equals("user") ? "用户：" : "AI：").append(m.getContent()).append("\n");
        }
        String fullPrompt = context + "用户：" + message;

        // 调用 DeepSeek
        String reply = deepSeek.chat(SYSTEM_PROMPT, fullPrompt);

        // 保存AI回复
        Message aiMsg = new Message();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("ai");
        aiMsg.setContent(reply);
        msgMapper.insert(aiMsg);

        // 更新对话时间
        conv.setUpdatedAt(LocalDateTime.now());
        convMapper.updateById(conv);

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("conversationId", conversationId);
        return result;
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
