package com.smartmedical.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用
    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 token 已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户
    PHONE_EXISTS(1001, "手机号已注册"),
    PHONE_NOT_FOUND(1002, "手机号未注册"),
    PASSWORD_WRONG(1003, "密码错误"),
    CODE_WRONG(1004, "验证码错误"),
    CODE_EXPIRED(1005, "验证码已过期"),
    CODE_TOO_FREQUENT(1006, "验证码发送过于频繁"),
    PASSWORD_TOO_WEAK(1007, "密码强度不足"),
    USER_DISABLED(1008, "账号已被禁用"),
    USER_DELETED(1009, "账号已注销"),

    // 家庭成员
    MEMBER_NOT_FOUND(2001, "家庭成员不存在"),
    MEMBER_LIMIT_EXCEEDED(2002, "家庭成员数量已达上限"),
    ;

    private final int code;
    private final String message;
}
