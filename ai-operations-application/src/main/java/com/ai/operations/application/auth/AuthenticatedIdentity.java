package com.ai.operations.application.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticatedIdentity {
    /** 已认证的运营用户全局 ID，未解析用户时为空。 */
    private String userId;
    /** 已认证的身份类型。 */
    private String identityType;
    /** 身份认证提供方。 */
    private String identityProvider;
    /** 已认证的身份标识值，如账号名或手机号。 */
    private String identityValue;
}

