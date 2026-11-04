package com.ai.operations.application.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResult {
    /** 登录会话令牌，用于后续请求的 Bearer 认证。 */
    private String sessionToken;
    /** 登录会话 ID。 */
    private String sessionId;
    /** 登录成功的运营用户全局 ID。 */
    private String userId;
}

