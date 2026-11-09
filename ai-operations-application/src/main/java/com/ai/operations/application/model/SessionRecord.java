package com.ai.operations.application.model;

import java.time.LocalDateTime;

/**
 * 登录会话记录（业务模型）。
 *
 * <p>序列化格式与键布局由 infrastructure 的 SessionStore 实现负责。
 */
public record SessionRecord(String sessionId, String userId, String deviceId, String loginIp,
                            String userAgent, LocalDateTime loginAt, long ttlMillis,
                            long renewWindowMillis) {
}
