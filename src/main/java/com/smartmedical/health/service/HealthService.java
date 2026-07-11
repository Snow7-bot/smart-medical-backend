package com.smartmedical.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.health.entity.HealthRecord;
import com.smartmedical.health.mapper.HealthRecordMapper;
import com.smartmedical.llm.DeepSeekClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final HealthRecordMapper recordMapper;
    private final DeepSeekClient deepSeek;

    private static final Map<String, String> TYPE_MAP = Map.of(
            "bp", "blood_pressure", "bs", "glucose", "bo", "SpO2",
            "hr", "heart_rate", "wt", "weight"
    );

    // 前端 key → 数据库 type
    private String dbType(String metricType) {
        return TYPE_MAP.getOrDefault(metricType, metricType);
    }

    // 数据库 type → 前端 key
    private String metricType(String dbType) {
        return TYPE_MAP.entrySet().stream()
                .filter(e -> e.getValue().equals(dbType))
                .map(Map.Entry::getKey)
                .findFirst().orElse(dbType);
    }

    public HealthRecord addRecord(Long userId, Map<String, Object> body) {
        String metricType = (String) body.get("metricType");
        String valueStr = (String) body.get("valueStr");
        String valueSys = body.get("valueSys") != null ? body.get("valueSys").toString() : null;
        String valueDia = body.get("valueDia") != null ? body.get("valueDia").toString() : null;
        Long patientId = body.get("patientId") != null
                ? Long.valueOf(body.get("patientId").toString()) : null;
        String unit = (String) body.get("unit");
        String timeLabel = (String) body.get("timeLabel");

        HealthRecord r = new HealthRecord();
        r.setUserId(userId);
        r.setMemberId(patientId != null ? patientId : 0);
        r.setType(dbType(metricType));

        // 构建存储值
        if (valueStr != null) {
            r.setValue(valueStr);
        } else if (valueSys != null && valueDia != null) {
            r.setValue(valueSys + "/" + valueDia);
        }
        r.setUnit(unit);
        r.setRecordedAt(LocalDateTime.now());
        recordMapper.insert(r);
        return r;
    }

    public List<Map<String, Object>> getRecords(Long userId, Long patientId, String metricType) {
        LambdaQueryWrapper<HealthRecord> q = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .orderByDesc(HealthRecord::getRecordedAt);

        if (patientId != null) q.eq(HealthRecord::getMemberId, patientId);
        if (metricType != null) q.eq(HealthRecord::getType, dbType(metricType));

        List<Map<String, Object>> result = new ArrayList<>();
        for (HealthRecord r : recordMapper.selectList(q)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("metricType", metricType(r.getType()));
            item.put("valueStr", r.getValue());
            item.put("value", r.getValue());
            item.put("date", r.getRecordedAt().toLocalDate().toString());
            item.put("time", r.getRecordedAt().toLocalTime().toString().substring(0, 5));
            item.put("unit", r.getUnit());

            // 血压拆成 valueSys / valueDia
            if ("blood_pressure".equals(r.getType())) {
                String[] parts = r.getValue().split("/");
                if (parts.length == 2) {
                    item.put("valueSys", parts[0]);
                    item.put("valueDia", parts[1]);
                }
            }
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getTrend(Long userId, Long patientId, String metricType) {
        List<Map<String, Object>> records = getRecords(userId, patientId, metricType);
        // 返回最近 14 条
        return records.size() > 14 ? records.subList(0, 14) : records;
    }

    public Map<String, Object> getReport(Long userId, Long patientId, String period) {
        int days = "month".equals(period) ? 30 : 7;
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<HealthRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getUserId, userId)
                        .eq(patientId != null, HealthRecord::getMemberId, patientId)
                        .ge(HealthRecord::getRecordedAt, since)
                        .orderByAsc(HealthRecord::getRecordedAt));

        if (records.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("items", List.of());
            empty.put("summary", "暂无健康数据");
            return empty;
        }

        // 按指标类型分组
        Map<String, List<HealthRecord>> grouped = new LinkedHashMap<>();
        for (HealthRecord r : records) {
            grouped.computeIfAbsent(r.getType(), k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, List<HealthRecord>> e : grouped.entrySet()) {
            String mt = metricType(e.getKey());
            List<HealthRecord> list = e.getValue();

            double sum = 0;
            double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
            for (HealthRecord r : list) {
                double v = parseFirstValue(r.getValue());
                sum += v;
                if (v > max) max = v;
                if (v < min) min = v;
            }
            double avg = sum / list.size();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", labelOf(e.getKey()) + "分析");
            item.put("trendKey", mt);
            item.put("summary", period.equals("month") ? "本月" : "本周" + labelOf(e.getKey()) + "基本稳定");
            item.put("stats", List.of(
                    Map.of("label", "平均", "value", String.format("%.0f", avg)),
                    Map.of("label", "最高", "value", String.format("%.0f", max)),
                    Map.of("label", "最低", "value", String.format("%.0f", min))
            ));
            item.put("advice", "保持规律监测");
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("summary", items.size() + "项指标已分析");
        return result;
    }

    private double parseFirstValue(String value) {
        try {
            String[] parts = value.split("/");
            return Double.parseDouble(parts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
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
}
