package com.ai.operations.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("operation_user_identity")
public class OperationUserIdentityEntity extends VersionedEntity {
    /** 关联的运营用户全局 ID。 */
    private String userId;
    /** 身份类型。 */
    private String identityType;
    /** 身份提供方。 */
    private String identityProvider;
    /** 身份标识值，如账号名或手机号。 */
    private String identityValue;
    /** 身份认证通过时间。 */
    private LocalDateTime verifiedAt;
}

