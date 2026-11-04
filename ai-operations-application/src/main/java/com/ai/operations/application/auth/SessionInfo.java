package com.ai.operations.application.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SessionInfo {
    /** 登录会话 ID。 */
    private String sessionId;
    /** 登录设备标识。 */
    private String deviceId;
    /** 登录来源 IP。 */
    private String loginIp;
    /** 登录客户端 User-Agent。 */
    private String userAgent;
    /** 会话登录时间。 */
    private LocalDateTime loginAt;
    /** 是否为当前请求使用的登录会话。 */
    private boolean current;
}

