package com.smartmedical.user.service;

import com.smartmedical.user.dto.*;

public interface UserService {

    /** 发送验证码 */
    void sendCode(SendCodeRequest request);

    /** 注册 */
    LoginResponse register(RegisterRequest request);

    /** 登录 */
    LoginResponse login(LoginRequest request);

    /** 获取当前用户信息 */
    LoginResponse getCurrentUser(Long userId);

    /** 更新个人资料 */
    void updateProfile(Long userId, UpdateProfileRequest request);
}
