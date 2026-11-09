package com.ai.operations.infrastructure.repository;

import com.ai.operations.application.model.SessionRecord;
import com.ai.operations.application.port.SessionStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * SessionStore 实现：Redis 键布局、Lua 脚本、JSON 序列化全部收敛于此。
 *
 * <p>键布局：
 * <ul>
 *   <li>tokenHash → 会话 JSON：{@code ai-operations:auth:session:{tokenHash}}</li>
 *   <li>sessionId → tokenHash：{@code ai-operations:auth:session-id:{sessionId}}</li>
 *   <li>userId → 会话 Hash（设备槽位）：{@code ai-operations:auth:user-sessions:{userId}}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSessionStore implements SessionStore {

    private static final String DEVICE_LIMIT_EXCEEDED = "DEVICE_LIMIT_EXCEEDED";
    private static final RedisScript<String> REPLACE_DEVICE_SESSION_SCRIPT = new DefaultRedisScript<>(
            "local oldSessionId = redis.call('HGET', KEYS[1], ARGV[1])\n"
                    + "if not oldSessionId and redis.call('HLEN', KEYS[1]) >= tonumber(ARGV[2]) then return 'DEVICE_LIMIT_EXCEEDED' end\n"
                    + "redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])\n"
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[4])\nreturn oldSessionId or ''", String.class);
    private static final RedisScript<Long> ROLLBACK_DEVICE_SESSION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('HGET', KEYS[1], ARGV[1]) ~= ARGV[2] then return 0 end\n"
                    + "if ARGV[3] == '' then return redis.call('HDEL', KEYS[1], ARGV[1]) end\n"
                    + "return redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])", Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public DeviceSlot replaceDeviceSlot(String userId, String deviceId, String sessionId,
                                        long ttlMillis, int maxDevices) {
        String result = redis.execute(REPLACE_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)),
                deviceId, String.valueOf(maxDevices), sessionId, String.valueOf(ttlMillis));
        if (DEVICE_LIMIT_EXCEEDED.equals(result)) {
            return new DeviceSlot("", true);
        }
        return new DeviceSlot(result == null ? "" : result, false);
    }

    @Override
    public void rollbackDeviceSlot(String userId, String deviceId, String sessionId, String oldSessionId) {
        redis.execute(ROLLBACK_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)),
                deviceId, sessionId, oldSessionId);
    }

    @Override
    public void saveSession(SessionRecord session, String tokenHash) {
        try {
            redis.opsForValue().set(sessionKey(tokenHash), serialize(session), Duration.ofMillis(session.ttlMillis()));
            redis.opsForValue().set(sessionIdKey(session.sessionId()), tokenHash, Duration.ofMillis(session.ttlMillis()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize session", exception);
        }
    }

    @Override
    public Optional<String> findTokenHash(String sessionId) {
        return Optional.ofNullable(redis.opsForValue().get(sessionIdKey(sessionId)));
    }

    @Override
    public Optional<SessionRecord> findSession(String tokenHash) {
        String value = redis.opsForValue().get(sessionKey(tokenHash));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, SessionData.class).toRecord());
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteSession(String tokenHash, String sessionId) {
        redis.delete(sessionKey(tokenHash));
        redis.delete(sessionIdKey(sessionId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listSessionIds(String userId) {
        List<Object> values = redis.opsForHash().values(userSessionsKey(userId));
        if (values == null) {
            return List.of();
        }
        return values.stream().map(Object::toString).toList();
    }

    @Override
    public void removeDevice(String userId, String deviceId) {
        redis.opsForHash().delete(userSessionsKey(userId), deviceId);
    }

    private String sessionKey(String tokenHash) {
        return "ai-operations:auth:session:{" + tokenHash + "}";
    }

    private String sessionIdKey(String sessionId) {
        return "ai-operations:auth:session-id:{" + sessionId + "}";
    }

    private String userSessionsKey(String userId) {
        return "ai-operations:auth:user-sessions:{" + userId + "}";
    }

    private String serialize(SessionRecord record) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new SessionData(record));
    }

    /** Redis 存储的会话数据形状（loginAt 序列化为字符串，兼容既有线上格式） */
    private record SessionData(String sessionId, String userId, String deviceId, String loginIp,
                               String userAgent, String loginAt, long ttlMillis, long renewWindowMillis) {

        private SessionData(SessionRecord record) {
            this(record.sessionId(), record.userId(), record.deviceId(), record.loginIp(), record.userAgent(),
                    record.loginAt() == null ? null : record.loginAt().toString(),
                    record.ttlMillis(), record.renewWindowMillis());
        }

        private SessionRecord toRecord() {
            LocalDateTime parsedLoginAt = null;
            if (loginAt != null) {
                try {
                    parsedLoginAt = LocalDateTime.parse(loginAt);
                } catch (DateTimeParseException ignored) {
                    parsedLoginAt = null;
                }
            }
            return new SessionRecord(sessionId, userId, deviceId, loginIp, userAgent, parsedLoginAt,
                    ttlMillis, renewWindowMillis);
        }
    }
}
