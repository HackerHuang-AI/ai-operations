package com.ai.operations.application.port;

import com.ai.operations.application.model.OperationLoginAuditEntry;

/**
 * 登录审计写入端口。
 */
public interface LoginAuditPort {

    /** 写入一条登录审计记录 */
    void record(OperationLoginAuditEntry entry);
}
