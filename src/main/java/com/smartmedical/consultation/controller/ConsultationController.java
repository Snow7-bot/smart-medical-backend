package com.smartmedical.consultation.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.consultation.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService service;

    @PostMapping("/consultation/chat")
    public ApiResponse<Map<String, Object>> chat(@AuthenticationPrincipal Long userId,
                                                  @RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        Long conversationId = body.get("conversationId") != null
                ? Long.valueOf(body.get("conversationId").toString()) : null;
        return ApiResponse.ok(service.chat(userId, message, conversationId));
    }

    @GetMapping("/consultation/history")
    public ApiResponse<List<Map<String, Object>>> history(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(service.getHistory(userId));
    }
}
