package com.ai.operations.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("operation_login_audit")
public class OperationLoginAuditEntity {
    /** 登录审计主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 运营用户全局 ID，登录失败时可为空。 */
    private String userId;
    /** 全局租户 ID，运营端登录时为空。 */
    private String tenantId;
    /** 登录使用的身份类型。 */
    private String identityType;
    /** 身份标识值的 SHA-256 摘要。 */
    private String identityValueHash;
    /** 登录结果：1-成功，2-失败。 */
    private Integer loginResult;
    /** 登录失败码。 */
    private String failureCode;
    /** 登录来源 IP。 */
    private String loginIp;
    /** 客户端 User-Agent。 */
    private String userAgent;
    /** 登录时间。 */
    private LocalDateTime loginAt;
    /** 审计记录创建时间。 */
    private LocalDateTime createTime;
}

