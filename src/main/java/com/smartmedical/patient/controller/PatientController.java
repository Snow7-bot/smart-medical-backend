package com.smartmedical.patient.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.patient.entity.Patient;
import com.smartmedical.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService service;

    @PostMapping
    public ApiResponse<Patient> create(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.create(userId, body));
    }

    @GetMapping
    public ApiResponse<List<Patient>> list(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(service.list(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Patient> detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(service.detail(userId, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.update(userId, id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.delete(userId, id);
        return ApiResponse.ok(null);
    }
}
