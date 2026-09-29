package com.ai.operations.application.enums;

import java.util.Arrays;

public enum ErrorCodeEnum {
    REQUEST_VALIDATION_FAILED("3001001", "error.3001001", "请求参数校验失败"),
    REQUEST_BODY_INVALID("3001002", "error.3001002", "请求体格式错误"),
    PARAMETER_REQUIRED("3001003", "error.3001003", "参数不能为空"),
    PARAMETER_FORMAT_INVALID("3001004", "error.3001004", "参数格式不合法"),
    PARAMETER_OUT_OF_RANGE("3001005", "error.3001005", "参数超出允许范围"),
    PARAMETER_VALUE_INVALID("3001006", "error.3001006", "参数值不合法"),
    SYSTEM_ERROR("3001999", "error.3001999", "系统异常，请稍后重试");

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

