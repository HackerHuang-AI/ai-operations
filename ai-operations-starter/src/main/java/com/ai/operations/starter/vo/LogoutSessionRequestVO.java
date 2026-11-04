package com.ai.operations.starter.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutSessionRequestVO {
    /** 要下线的登录会话 ID。 */
    @NotBlank(message = "3001003")
    private String sessionId;
}

