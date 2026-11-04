package com.ai.operations.application.enums;

public enum IdentityProviderEnum {
    LOCAL("LOCAL"),
    SMS("SMS");

    private final String value;

    IdentityProviderEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

