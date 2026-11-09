package com.ai.operations.infrastructure.repository;

import com.ai.operations.application.model.OperationLoginAuditEntry;
import com.ai.operations.application.port.LoginAuditPort;
import com.ai.operations.infrastructure.persistence.entity.OperationLoginAuditEntity;
import com.ai.operations.infrastructure.persistence.mapper.OperationLoginAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LoginAuditPort 实现：BO → Entity 映射下沉。
 */
@Component
@RequiredArgsConstructor
public class OperationLoginAuditRepository implements LoginAuditPort {

    private final OperationLoginAuditMapper auditMapper;

    @Override
    public void record(OperationLoginAuditEntry entry) {
        OperationLoginAuditEntity audit = new OperationLoginAuditEntity();
        audit.setUserId(entry.userId());
        audit.setIdentityType(entry.identityType());
        audit.setIdentityValueHash(entry.identityValueHash());
        audit.setLoginResult(entry.loginResult());
        audit.setLoginIp(entry.loginIp());
        audit.setUserAgent(entry.userAgent());
        audit.setLoginAt(entry.loginAt());
        auditMapper.insert(audit);
    }
}
