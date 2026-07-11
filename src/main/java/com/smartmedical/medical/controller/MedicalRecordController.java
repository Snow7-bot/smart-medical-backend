package com.smartmedical.medical.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.medical.entity.MedicalRecord;
import com.smartmedical.medical.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService service;

    @PostMapping
    public ApiResponse<MedicalRecord> upload(@AuthenticationPrincipal Long userId,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) Long patientId,
                                              @RequestParam(required = false) String recordDate) throws Exception {
        return ApiResponse.ok(service.upload(userId, patientId != null ? patientId : 0, file, recordDate));
    }

    @PostMapping("/upload")
    public ApiResponse<MedicalRecord> uploadSync(@AuthenticationPrincipal Long userId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam(required = false) Long patientId) throws Exception {
        return ApiResponse.ok(service.uploadSync(userId, patientId != null ? patientId : 0, file));
    }

    @PostMapping("/{id}/parse")
    public ApiResponse<MedicalRecord> parse(@PathVariable Long id) {
        return ApiResponse.ok(service.parse(id));
    }

    @GetMapping("/patients/{patientId}")
    public ApiResponse<List<MedicalRecord>> listByPatient(@AuthenticationPrincipal Long userId,
                                                           @PathVariable Long patientId) {
        return ApiResponse.ok(service.listByPatient(userId, patientId));
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listAll(@AuthenticationPrincipal Long userId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listAll(userId, page, size));
    }

    @GetMapping("/timeline")
    public ApiResponse<List<Map<String, Object>>> timeline(@AuthenticationPrincipal Long userId,
                                                            @RequestParam(required = false) Long patientId) {
        return ApiResponse.ok(service.timeline(userId, patientId));
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicalRecord> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @GetMapping("/{id}/url")
    public ApiResponse<Map<String, String>> getPresignedUrl(@PathVariable Long id) {
        return ApiResponse.ok(service.getPresignedUrl(id));
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<Map<String, Object>> getSummary(@PathVariable Long id) {
        return ApiResponse.ok(service.getSummary(id));
    }

    @PutMapping("/{id}/date")
    public ApiResponse<Void> updateDate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.updateDate(id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping
    public ApiResponse<Void> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("ids");
        List<Long> ids = rawIds.stream().map(Long::valueOf).toList();
        service.batchDelete(ids);
        return ApiResponse.ok(null);
    }
}
