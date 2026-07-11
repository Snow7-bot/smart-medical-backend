package com.smartmedical.medication.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.medication.entity.Drug;
import com.smartmedical.medication.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medication")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService service;

    @GetMapping("/list")
    public ApiResponse<List<Drug>> getList(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(service.getList(userId));
    }

    @PostMapping("/add")
    public ApiResponse<Drug> add(@AuthenticationPrincipal Long userId,
                                  @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.add(userId, body));
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id) {
        service.delete(userId, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/scan")
    public ApiResponse<Map<String, Object>> scan(@AuthenticationPrincipal Long userId,
                                                  @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.scan(body));
    }

    @PostMapping("/children-dosage")
    public ApiResponse<Map<String, Object>> calcDosage(@AuthenticationPrincipal Long userId,
                                                        @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.childrenDosage(body));
    }

    @PostMapping("/check-interaction")
    public ApiResponse<Map<String, Object>> checkInteraction(@AuthenticationPrincipal Long userId,
                                                              @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.checkInteraction(userId, body));
    }
}
