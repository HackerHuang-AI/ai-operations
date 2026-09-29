package com.ai.operations.infrastructure.enums;

/**
 * Nacos 业务配置 DataId。
 */
public enum NacosDataIdEnum {
    ;

    private final String dataId;

    NacosDataIdEnum(String dataId) {
        this.dataId = dataId;
    }

    public String dataId() {
        return dataId;
    }
}

