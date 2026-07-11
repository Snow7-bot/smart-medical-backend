package com.smartmedical.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordMapper mapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public MedicalRecord upload(Long userId, Long patientId, MultipartFile file, String recordDate) throws IOException {
        return saveFile(userId, patientId, file, recordDate, true);
    }

    public MedicalRecord uploadSync(Long userId, Long patientId, MultipartFile file) throws IOException {
        return saveFile(userId, patientId, file, null, false);
    }

    private MedicalRecord saveFile(Long userId, Long patientId, MultipartFile file, String recordDate, boolean autoParse) throws IOException {
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
        r.setParseStatus(autoParse ? "processing" : "pending");
        if (recordDate != null) r.setRecordDate(LocalDate.parse(recordDate));
        r.setParsedData("{}");
        mapper.insert(r);

        // Mock 解析（实际应异步 OCR）
        if (autoParse) {
            r.setParseStatus("done");
            r.setParsedData("{\"diagnosis\":\"待专业医生解读\",\"status\":\"auto_parsed\"}");
            mapper.updateById(r);
        }

        return r;
    }

    public MedicalRecord parse(Long recordId) {
        MedicalRecord r = mapper.selectById(recordId);
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        r.setParseStatus("done");
        r.setParsedData("{\"diagnosis\":\"待专业医生解读\",\"status\":\"parsed\"}");
        mapper.updateById(r);
        return r;
    }

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
