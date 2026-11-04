package com.ai.operations.application.enums;

import java.util.Arrays;

public enum ErrorCodeEnum {
    REQUEST_VALIDATION_FAILED("3001001", "error.3001001", "请求参数校验失败"),
    REQUEST_BODY_INVALID("3001002", "error.3001002", "请求体格式错误"),
    PARAMETER_REQUIRED("3001003", "error.3001003", "参数不能为空"),
    PARAMETER_FORMAT_INVALID("3001004", "error.3001004", "参数格式不合法"),
    PARAMETER_OUT_OF_RANGE("3001005", "error.3001005", "参数超出允许范围"),
    PARAMETER_VALUE_INVALID("3001006", "error.3001006", "参数值不合法"),
    SYSTEM_ERROR("3001999", "error.3001999", "系统异常，请稍后重试"),
    LOGIN_FAILED("3101001", "error.3101001", "账号或密码错误"),
    SMS_CODE_INVALID("3101002", "error.3101002", "验证码错误或已过期"),
    SMS_CODE_SEND_TOO_FREQUENT("3101003", "error.3101003", "验证码发送过于频繁"),
    SMS_SERVICE_UNAVAILABLE("3101004", "error.3101004", "短信服务暂不可用"),
    LOGIN_TYPE_NOT_AVAILABLE("3101005", "error.3101005", "当前登录方式暂未开放"),
    DEVICE_LIMIT_EXCEEDED("3101006", "error.3101006", "当前在线设备数量已达上限，请先下线其他设备"),
    MOBILE_REQUIRED("3101109", "error.3101109", "手机号不能为空"),
    LOGIN_TYPE_REQUIRED("3101101", "error.3101101", "登录方式不能为空"),
    LOGIN_TYPE_INVALID("3101102", "error.3101102", "登录方式不合法"),
    ACCOUNT_REQUIRED("3101103", "error.3101103", "账号不能为空"),
    PASSWORD_REQUIRED("3101104", "error.3101104", "密码不能为空"),
    SMS_CODE_REQUIRED("3101105", "error.3101105", "验证码不能为空"),
    DEVICE_ID_REQUIRED("3101107", "error.3101107", "设备标识不能为空"),
    DEVICE_ID_FORMAT_INVALID("3101108", "error.3101108", "设备标识格式不合法");

    private final String code;
    private final String messageKey;
    private final String defaultMessage;

    ErrorCodeEnum(String code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public static ErrorCodeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown error code: " + code));
    }
}

