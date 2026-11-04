package com.ai.operations.application.enums;

public enum IdentityTypeEnum {
    ACCOUNT("ACCOUNT"),
    MOBILE("MOBILE");

    private final String value;

    IdentityTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

