package com.ai.operations.application.model;

import java.time.LocalDateTime;

/**
 * 登录审计记录（application 层业务模型，持久化由 infra 完成字段映射）。
 */
public record OperationLoginAuditEntry(String userId, String identityType, String identityValueHash,
                                       int loginResult, String loginIp, String userAgent,
                                       LocalDateTime loginAt) {
}
