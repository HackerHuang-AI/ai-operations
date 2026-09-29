package com.ai.operations.application.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResultCodeEnum {
    SUCCESS("00", "成功"),
    ERROR("01", "失败");

    @JsonValue
    private final String code;
    private final String defaultMessage;

    ResultCodeEnum(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}

