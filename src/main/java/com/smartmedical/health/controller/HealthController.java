package com.smartmedical.health.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.health.entity.HealthRecord;
import com.smartmedical.health.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService service;

    @PostMapping("/record")
    public ApiResponse<HealthRecord> addRecord(@AuthenticationPrincipal Long userId,
                                               @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.addRecord(userId, body));
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> getRecords(@AuthenticationPrincipal Long userId,
                                                              @RequestParam(required = false) Long patientId,
                                                              @RequestParam(required = false) String metricType) {
        return ApiResponse.ok(service.getRecords(userId, patientId, metricType));
    }

    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> getTrend(@AuthenticationPrincipal Long userId,
                                                            @RequestParam(required = false) Long patientId,
                                                            @RequestParam(required = false) String metricType) {
        return ApiResponse.ok(service.getTrend(userId, patientId, metricType));
    }

    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> getReport(@AuthenticationPrincipal Long userId,
                                                       @RequestParam(required = false) Long memberId,
                                                       @RequestParam(defaultValue = "week") String period) {
        return ApiResponse.ok(service.getReport(userId, memberId, period));
    }
}
