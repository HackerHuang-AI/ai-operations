package com.ai.operations.application.model;

/**
 * 运营用户业务对象（登录校验所需的最小字段集）。
 */
public record OperationUserBO(String userId, String passwordHash) {
}
