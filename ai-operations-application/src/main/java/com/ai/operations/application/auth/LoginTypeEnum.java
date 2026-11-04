package com.ai.operations.application.auth;

import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;

import java.util.Arrays;

public enum LoginTypeEnum {
    ACCOUNT_PASSWORD("ACCOUNT_PASSWORD"),
    MOBILE_SMS("MOBILE_SMS");

    private final String value;

    LoginTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LoginTypeEnum fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.LOGIN_TYPE_INVALID));
    }
}

