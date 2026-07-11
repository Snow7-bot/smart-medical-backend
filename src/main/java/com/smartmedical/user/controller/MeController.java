package com.smartmedical.user.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.user.dto.LoginResponse;
import com.smartmedical.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 独立路径 /me，对应前端 userApi.getCurrentUser()
 */
@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping("/api/me")
    public ApiResponse<LoginResponse> getCurrentUser(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getCurrentUser(userId));
    }
}
