package com.smartmedical.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
import com.smartmedical.llm.DeepSeekClient;
import com.smartmedical.llm.QwenOcrClient;
import com.smartmedical.medical.entity.MedicalRecord;
import com.smartmedical.medical.mapper.MedicalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordMapper mapper;
    private final DeepSeekClient deepSeek;
    private final QwenOcrClient qwenOcr;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public MedicalRecord upload(Long userId, Long patientId, MultipartFile file, String recordDate) throws IOException {
        MedicalRecord r = saveFile(userId, patientId, file, recordDate);
        // 自动 OCR 提取 + AI 分析
        analyzeRecord(r, file);
        return r;
    }

    public MedicalRecord uploadSync(Long userId, Long patientId, MultipartFile file) throws IOException {
        return saveFile(userId, patientId, file, null);
    }

    private MedicalRecord saveFile(Long userId, Long patientId, MultipartFile file, String recordDate) throws IOException {
        File dir = new File(uploadDir, "medical");
        if (!dir.exists()) dir.mkdirs();

        String uuid = UUID.randomUUID().toString();
        String ext = "";
        String origName = file.getOriginalFilename();
        if (origName != null && origName.contains(".")) ext = origName.substring(origName.lastIndexOf("."));
        String fileName = uuid + ext;
        file.transferTo(new File(dir, fileName));

        MedicalRecord r = new MedicalRecord();
        r.setUserId(userId);
        r.setPatientId(patientId);
        r.setFileUrl("/uploads/medical/" + fileName);
        r.setFileType(ext.toLowerCase().contains("pdf") ? "pdf" : "image");
        r.setParseStatus("pending");
        if (recordDate != null && !recordDate.isEmpty()) r.setRecordDate(LocalDate.parse(recordDate));
        r.setParsedData("{}");
        mapper.insert(r);
        return r;
    }

    /** OCR 提取 + AI 分析 */
    private void analyzeRecord(MedicalRecord r, MultipartFile file) {
        try {
            r.setParseStatus("processing");
            mapper.updateById(r);

            // 1. 提取文本（出错会抛异常，里面带详细原因）
            String extractedText;
            try {
                extractedText = extractText(r.getFileUrl());
            } catch (Exception ex) {
                String errMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                log.error("extractText 失败: {}", errMsg);
                r.setParseStatus("failed");
                r.setParsedData(objectMapper.writeValueAsString(Map.of(
                    "status", "ocr_failed",
                    "summary", "OCR提取失败: " + errMsg,
                    "fileType", r.getFileType()
                )));
                mapper.updateById(r);
                return;
            }

            if (extractedText == null || extractedText.isBlank()) {
                r.setParseStatus("done");
                r.setParsedData(objectMapper.writeValueAsString(Map.of(
                    "status", "no_text",
                    "summary", "未能识别文件内容（文件类型: " + r.getFileType() + "，图片中可能没有文字）",
                    "fileType", r.getFileType()
                )));
                mapper.updateById(r);
                return;
            }

            // 2. DeepSeek 医疗分析
            String analysis = deepSeek.chat(
                    "你是专业医学文档分析助手。请用通俗易懂的大白话，分析以下病历内容。按这个格式回复：" +
                    "「主要问题」一句话概括；「重要发现」列出2-3个关键指标；「用药情况」列出涉及的药物；「建议」给出1-2条通俗建议。200字以内。",
                    extractedText);

            // 3. 存储分析结果
            Map<String, Object> parsed = new LinkedHashMap<>();
            parsed.put("status", "analyzed");
            parsed.put("analysis", analysis);
            parsed.put("raw_text", extractedText.substring(0, Math.min(500, extractedText.length())));

            r.setParsedData(objectMapper.writeValueAsString(parsed));
            r.setParseStatus("done");
            mapper.updateById(r);

        } catch (Exception e) {
            log.error("病历分析失败", e);
            r.setParseStatus("failed");
            try {
                r.setParsedData(objectMapper.writeValueAsString(Map.of(
                    "status", "error",
                    "summary", "分析异常: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                )));
            } catch (Exception ignored) {}
            mapper.updateById(r);
        }
    }

    /** 提取文件中的文本 */
    private String extractText(String fileUrl) throws IOException {
        File file = new File(uploadDir, fileUrl.substring("/uploads/".length()));
        String name = file.getName().toLowerCase();
        log.info("extractText: url={}, diskPath={}, exists={}, name={}", fileUrl, file.getAbsolutePath(), file.exists(), name);

        if (!file.exists()) {
            throw new IOException("文件不存在: " + file.getAbsolutePath());
        }

        // PDF → PDFBox 提取
        if (name.endsWith(".pdf")) {
            log.info("extractText: 使用PDFBox提取PDF");
            try (PDDocument doc = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);
                return text.isBlank() ? null : text.substring(0, Math.min(3000, text.length()));
            }
        }

        // TXT → 直接读
        if (name.endsWith(".txt")) {
            log.info("extractText: 直接读取TXT");
            return new String(java.nio.file.Files.readAllBytes(file.toPath()));
        }

        // 图片 — 千问 VL OCR（内部失败会抛 IOException 带完整错误信息）
        log.info("extractText: 调用千问VL OCR, fileSize={}", file.length());
        return qwenOcr.extractText(file);
    }

    /** 触发重新解析 — 真正重新读取文件跑 OCR + AI */
    public MedicalRecord parse(Long recordId) {
        MedicalRecord r = mapper.selectById(recordId);
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        reAnalyze(r);
        return r;
    }

    /** 重新分析已有记录（读取磁盘文件） */
    private void reAnalyze(MedicalRecord r) {
        try {
            r.setParseStatus("processing");
            mapper.updateById(r);

            String extractedText;
            try {
                extractedText = extractText(r.getFileUrl());
            } catch (Exception ex) {
                String errMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                log.error("reAnalyze extractText 失败 id={}: {}", r.getId(), errMsg);
                r.setParseStatus("failed");
                r.setParsedData(objectMapper.writeValueAsString(Map.of(
                    "status", "ocr_failed",
                    "summary", "OCR提取失败: " + errMsg,
                    "fileType", r.getFileType()
                )));
                mapper.updateById(r);
                return;
            }

            if (extractedText == null || extractedText.isBlank()) {
                r.setParseStatus("done");
                r.setParsedData(objectMapper.writeValueAsString(Map.of(
                    "status", "no_text",
                    "summary", "未能识别文件内容（文件类型: " + r.getFileType() + "，图片中可能没有文字）",
                    "fileType", r.getFileType()
                )));
                mapper.updateById(r);
                return;
            }

            String analysis = deepSeek.chat(
                    "你是专业医学文档分析助手。请用通俗易懂的大白话，分析以下病历内容。按这个格式回复：" +
                    "「主要问题」一句话概括；「重要发现」列出2-3个关键指标；「用药情况」列出涉及的药物；「建议」给出1-2条通俗建议。200字以内。",
                    extractedText);

            Map<String, Object> parsed = new LinkedHashMap<>();
            parsed.put("status", "re-parsed");
            parsed.put("analysis", analysis);
            parsed.put("raw_text", extractedText.substring(0, Math.min(500, extractedText.length())));

            r.setParsedData(objectMapper.writeValueAsString(parsed));
            r.setParseStatus("done");
            mapper.updateById(r);

        } catch (Exception e) {
            log.error("重新解析病历失败 id={}", r.getId(), e);
            r.setParseStatus("failed");
            try {
                r.setParsedData(objectMapper.writeValueAsString(Map.of(
                    "status", "error",
                    "summary", "重新解析异常: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                )));
            } catch (Exception ignored) {}
            mapper.updateById(r);
        }
    }

    /** 获取 AI 分析摘要 */
    public Map<String, Object> getSummary(Long recordId) {
        MedicalRecord r = mapper.selectById(recordId);
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", r.getId());
        result.put("parseStatus", r.getParseStatus());
        result.put("fileType", r.getFileType());
        result.put("recordDate", r.getRecordDate() != null ? r.getRecordDate().toString() : "");

        try {
            if (r.getParsedData() != null) {
                Map<String, Object> parsed = objectMapper.readValue(r.getParsedData(), Map.class);
                result.put("analysis", parsed.getOrDefault("analysis", "暂无分析结果"));
                result.put("rawText", parsed.getOrDefault("raw_text", ""));
                result.put("status", parsed.getOrDefault("status", "unknown"));
            }
        } catch (Exception e) {
            result.put("analysis", "数据解析异常");
        }

        return result;
    }

    // ====== 基础 CRUD ======

    public List<MedicalRecord> listByPatient(Long userId, Long patientId) {
        return mapper.selectList(new LambdaQueryWrapper<MedicalRecord>()
                .eq(MedicalRecord::getUserId, userId)
                .eq(MedicalRecord::getPatientId, patientId)
                .orderByDesc(MedicalRecord::getCreatedAt));
    }

    public Map<String, Object> listAll(Long userId, int page, int size) {
        List<MedicalRecord> records = mapper.selectList(new LambdaQueryWrapper<MedicalRecord>()
                .eq(MedicalRecord::getUserId, userId)
                .orderByDesc(MedicalRecord::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page * size)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return result;
    }

    public List<Map<String, Object>> timeline(Long userId, Long patientId) {
        List<MedicalRecord> records = mapper.selectList(new LambdaQueryWrapper<MedicalRecord>()
                .eq(MedicalRecord::getUserId, userId)
                .eq(patientId != null, MedicalRecord::getPatientId, patientId)
                .orderByDesc(MedicalRecord::getRecordDate));

        Map<String, List<MedicalRecord>> grouped = new LinkedHashMap<>();
        for (MedicalRecord r : records) {
            String date = r.getRecordDate() != null ? r.getRecordDate().toString() : "";
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<MedicalRecord>> e : grouped.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", e.getKey());
            item.put("records", e.getValue());
            result.add(item);
        }
        return result;
    }

    public MedicalRecord detail(Long id) {
        MedicalRecord r = mapper.selectById(id);
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return r;
    }

    public Map<String, String> getPresignedUrl(Long id) {
        MedicalRecord r = mapper.selectById(id);
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return Map.of("url", r.getFileUrl());
    }

    public void updateDate(Long id, Map<String, Object> body) {
        MedicalRecord r = mapper.selectById(id);
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        r.setRecordDate(LocalDate.parse(body.get("recordDate").toString()));
        mapper.updateById(r);
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }

    public void batchDelete(List<Long> ids) {
        mapper.deleteBatchIds(ids);
    }
}
