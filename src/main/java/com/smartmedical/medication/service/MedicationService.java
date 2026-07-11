package com.smartmedical.medication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.medication.entity.Drug;
import com.smartmedical.medication.mapper.DrugMapper;
import com.smartmedical.llm.DeepSeekClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final DrugMapper drugMapper;
    private final DeepSeekClient deepSeek;

    public List<Drug> getList(Long userId) {
        return drugMapper.selectList(
                new LambdaQueryWrapper<Drug>().eq(Drug::getUserId, userId));
    }

    public Drug add(Long userId, Map<String, Object> body) {
        Drug d = new Drug();
        d.setUserId(userId);
        d.setName(body.get("name").toString());
        if (body.get("spec") != null) d.setSpec(body.get("spec").toString());
        if (body.get("dose") != null) d.setDose(body.get("dose").toString());
        if (body.get("freq") != null) d.setFreq(body.get("freq").toString());
        if (body.get("time") != null) d.setTimeOfDay(body.get("time").toString());
        d.setWarning(0);
        drugMapper.insert(d);
        return d;
    }

    public void delete(Long userId, Long id) {
        drugMapper.delete(new LambdaQueryWrapper<Drug>()
                .eq(Drug::getId, id).eq(Drug::getUserId, userId));
    }

    public Map<String, Object> scan(Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", "阿莫西林胶囊");
        result.put("spec", "0.5g×24粒");
        result.put("instructions", "口服。成人一次0.5g，每6-8小时1次。");
        return result;
    }

    public Map<String, Object> childrenDosage(Map<String, Object> body) {
        double weight = Double.parseDouble(body.get("weight").toString());
        double age = Double.parseDouble(body.get("age").toString());
        double adultDose = body.get("adultDose") != null
                ? Double.parseDouble(body.get("adultDose").toString()) : 500;

        if (age < 2 || age > 12) {
            return Map.of("recommendedDose", "不适用", "unit", "",
                    "warning", "Clark公式仅适用于2-12岁儿童");
        }

        double childDose = (weight / 68.0) * adultDose;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recommendedDose", String.format("%.0f", childDose));
        result.put("unit", "mg/每次");
        result.put("freq", "每日2-3次");
        result.put("warning", childDose > adultDose * 1.5 ? "计算剂量偏高，请咨询医生" : "");
        return result;
    }

    public Map<String, Object> checkInteraction(Long userId, Map<String, Object> body) {
        // 前端传来的是药品名称数组
        @SuppressWarnings("unchecked")
        List<Object> drugNames = (List<Object>) body.get("drugIds");
        if (drugNames == null) drugNames = new ArrayList<>();

        // 收集已有的药品名 + 新输入的药品名
        List<Drug> existing = drugMapper.selectList(
                new LambdaQueryWrapper<Drug>().eq(Drug::getUserId, userId));
        Set<String> allNames = new LinkedHashSet<>();
        for (Drug d : existing) allNames.add(d.getName());
        for (Object n : drugNames) allNames.add(n.toString());
        if (allNames.size() < 2) allNames.add("常见感冒药");

        String names = String.join("、", allNames);
        String reply = deepSeek.chat(
                "你是药物相互作用专家。分析以下药物组合是否存在相互作用风险。" +
                "如果安全回复safe，有风险回复caution，严重风险回复danger。",
                "药物：" + names);

        Map<String, Object> result = new LinkedHashMap<>();
        if (reply.toLowerCase().contains("danger")) {
            result.put("hasRisk", true);
            result.put("detail", reply);
        } else if (reply.toLowerCase().contains("caution")) {
            result.put("hasRisk", true);
            result.put("detail", reply);
        } else {
            result.put("hasRisk", false);
            result.put("detail", "暂未检测到明显相互作用");
        }
        return result;
    }
}
