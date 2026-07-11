package com.smartmedical.user.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.user.dto.*;
import com.smartmedical.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 发送验证码 */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendCode(request);
        return ApiResponse.ok(null);
    }

    /** 注册 */
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    /** 登录 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    /** 获取当前用户信息（对应 /me） */
    @GetMapping("/me")
    public ApiResponse<LoginResponse> getCurrentUser(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getCurrentUser(userId));
    }

    /** 获取用户信息（兼容 /user/info） */
    @GetMapping("/info")
    public ApiResponse<LoginResponse> getUserInfo(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getCurrentUser(userId));
    }

    /** 更新个人资料 */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@AuthenticationPrincipal Long userId,
                                           @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(userId, request);
        return ApiResponse.ok(null);
    }
}
