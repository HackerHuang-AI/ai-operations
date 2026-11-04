package com.ai.operations.application.service.impl;

import com.ai.operations.application.auth.*;
import com.ai.operations.application.common.BizException;
import com.ai.operations.application.config.OperationsAuthConfig;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.model.request.RequestContext;
import com.ai.operations.application.model.request.RequestContextHolder;
import com.ai.operations.application.service.LoginAuthenticator;
import com.ai.operations.application.service.LoginService;
import com.ai.operations.infrastructure.config.NacosConfig;
import com.ai.operations.infrastructure.enums.NacosDataIdEnum;
import com.ai.operations.infrastructure.persistence.entity.OperationLoginAuditEntity;
import com.ai.operations.infrastructure.persistence.entity.OperationUserEntity;
import com.ai.operations.infrastructure.persistence.entity.OperationUserIdentityEntity;
import com.ai.operations.infrastructure.persistence.mapper.OperationLoginAuditMapper;
import com.ai.operations.infrastructure.persistence.mapper.OperationUserIdentityMapper;
import com.ai.operations.infrastructure.persistence.mapper.OperationUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {
    private static final Duration DEFAULT_SESSION_TTL = Duration.ofHours(4);
    private static final Duration DEFAULT_SESSION_RENEW_WINDOW = Duration.ofMinutes(10);
    private static final int DEFAULT_MAX_DEVICES = 5;
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

    private final Map<String, LoginAuthenticator> authenticators;
    private final OperationUserIdentityMapper identityMapper;
    private final OperationUserMapper userMapper;
    private final OperationLoginAuditMapper auditMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final NacosConfig nacosConfig;
    private final Executor loginAuditExecutor;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginServiceImpl(List<LoginAuthenticator> authenticators, OperationUserIdentityMapper identityMapper,
                            OperationUserMapper userMapper, OperationLoginAuditMapper auditMapper,
                            StringRedisTemplate redis, ObjectMapper objectMapper, NacosConfig nacosConfig,
                            @Qualifier("loginAuditExecutor") Executor loginAuditExecutor) {
        this.authenticators = authenticators.stream().collect(Collectors.toMap(
                authenticator -> authenticator.supports().getValue(), Function.identity()));
        this.identityMapper = identityMapper;
        this.userMapper = userMapper;
        this.auditMapper = auditMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.nacosConfig = nacosConfig;
        this.loginAuditExecutor = loginAuditExecutor;
    }

    @Override
    public LoginResult login(LoginCommand command) {
        LoginTypeEnum loginType = LoginTypeEnum.fromValue(command.getLoginType());
        command.setLoginType(loginType.getValue());
        LoginAuthenticator authenticator = authenticators.get(loginType.getValue());
        if (authenticator == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_TYPE_NOT_AVAILABLE);
        }
        AuthenticatedIdentity identity = authenticator.authenticate(command);
        String userId = identity.getUserId();
        if (userId == null) {
            OperationUserIdentityEntity userIdentity = identityMapper.selectOne(new LambdaQueryWrapper<OperationUserIdentityEntity>()
                    .eq(OperationUserIdentityEntity::getIdentityType, identity.getIdentityType())
                    .eq(OperationUserIdentityEntity::getIdentityProvider, identity.getIdentityProvider())
                    .eq(OperationUserIdentityEntity::getIdentityValue, identity.getIdentityValue()));
            if (userIdentity == null) {
                throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
            }
            userId = userIdentity.getUserId();
        }
        OperationUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<OperationUserEntity>()
                .eq(OperationUserEntity::getUserId, userId).eq(OperationUserEntity::getStatus, 1));
        if (user == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        String sessionId = IdWorker.getIdStr();
        String sessionToken = newSessionToken();
        String tokenHash = sha256(sessionToken);
        SessionInfo session = newSession(sessionId, command);
        SessionPolicy policy = sessionPolicy();
        String oldSessionId = replaceDeviceSession(userId, command.getDeviceId(), sessionId, policy.ttl());
        try {
            redis.opsForValue().set(sessionKey(tokenHash), sessionValue(session, userId, policy), policy.ttl());
            redis.opsForValue().set(sessionIdKey(sessionId), tokenHash, policy.ttl());
        } catch (RuntimeException exception) {
            rollbackDeviceSession(userId, command.getDeviceId(), sessionId, oldSessionId);
            throw exception;
        }
        if (!oldSessionId.isEmpty()) {
            deleteSessionValues(oldSessionId);
        }
        writeLoginAuditAsync(userId, command, identity);
        LoginResult result = new LoginResult();
        result.setSessionToken(sessionToken);
        result.setSessionId(sessionId);
        result.setUserId(userId);
        return result;
    }

    private String replaceDeviceSession(String userId, String deviceId, String sessionId, Duration ttl) {
        String result = redis.execute(REPLACE_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)),
                deviceId, String.valueOf(maxDevices()), sessionId, String.valueOf(ttl.toMillis()));
        if (DEVICE_LIMIT_EXCEEDED.equals(result)) {
            throw new BizException(ErrorCodeEnum.DEVICE_LIMIT_EXCEEDED);
        }
        return result == null ? "" : result;
    }

    private void rollbackDeviceSession(String userId, String deviceId, String sessionId, String oldSessionId) {
        redis.execute(ROLLBACK_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)), deviceId, sessionId, oldSessionId);
    }

    private int maxDevices() {
        OperationsAuthConfig config = nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_OPERATIONS_AUTH, OperationsAuthConfig.class);
        Integer value = config == null || config.getSession() == null ? null : config.getSession().getMaxDevices();
        return value == null || value <= 0 ? DEFAULT_MAX_DEVICES : value;
    }

    private SessionPolicy sessionPolicy() {
        OperationsAuthConfig config = nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_OPERATIONS_AUTH, OperationsAuthConfig.class);
        OperationsAuthConfig.SessionConfig session = config == null ? null : config.getSession();
        Integer ttlMinutes = session == null ? null : session.getTtlMinutes();
        Integer renewMinutes = session == null ? null : session.getRenewWindowMinutes();
        Duration ttl = ttlMinutes == null || ttlMinutes <= 0 ? DEFAULT_SESSION_TTL : Duration.ofMinutes(ttlMinutes);
        Duration renew = renewMinutes == null || renewMinutes <= 0
                ? DEFAULT_SESSION_RENEW_WINDOW : Duration.ofMinutes(renewMinutes);
        if (renew.compareTo(ttl) >= 0) {
            renew = Duration.ofMillis(ttl.toMillis() / 4);
        }
        return new SessionPolicy(ttl, renew);
    }

    private void writeLoginAuditAsync(String userId, LoginCommand command, AuthenticatedIdentity identity) {
        OperationLoginAuditEntity audit = new OperationLoginAuditEntity();
        audit.setUserId(userId);
        audit.setIdentityType(identity.getIdentityType());
        audit.setIdentityValueHash(sha256(identity.getIdentityValue()));
        audit.setLoginResult(1);
        audit.setLoginIp(command.getLoginIp());
        audit.setUserAgent(command.getUserAgent());
        audit.setLoginAt(LocalDateTime.now());
        try {
            loginAuditExecutor.execute(() -> {
                try {
                    auditMapper.insert(audit);
                } catch (Exception exception) {
                    log.error("[LoginAudit] 写入登录审计失败, userId={}", userId, exception);
                }
            });
        } catch (RuntimeException exception) {
            log.error("[LoginAudit] 审计任务提交失败, userId={}", userId, exception);
        }
    }

    @Override
    public void logout() {
        RequestContext context = RequestContextHolder.current().orElseThrow(() -> new IllegalStateException("current user is unavailable"));
        logout(context.userId(), context.sessionId());
    }

    @Override
    public void logout(String sessionId) {
        RequestContext context = RequestContextHolder.current().orElseThrow(() -> new IllegalStateException("current user is unavailable"));
        logout(context.userId(), sessionId);
    }

    @Override
    public void logoutAll() {
        RequestContext context = RequestContextHolder.current().orElseThrow(() -> new IllegalStateException("current user is unavailable"));
        List<Object> sessions = redis.opsForHash().values(userSessionsKey(context.userId()));
        if (sessions != null) {
            sessions.forEach(id -> logout(context.userId(), id.toString()));
        }
    }

    private void logout(String userId, String sessionId) {
        String tokenHash = redis.opsForValue().get(sessionIdKey(sessionId));
        if (tokenHash == null) {
            return;
        }
        SessionData session = readSession(tokenHash);
        if (session == null || !userId.equals(session.userId())) {
            return;
        }
        deleteSessionValues(sessionId);
        redis.opsForHash().delete(userSessionsKey(userId), session.deviceId());
    }

    @Override
    public List<SessionInfo> listSessions() {
        RequestContext context = RequestContextHolder.current().orElseThrow(() -> new IllegalStateException("current user is unavailable"));
        List<Object> sessionIds = redis.opsForHash().values(userSessionsKey(context.userId()));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        List<SessionInfo> sessions = new ArrayList<>();
        for (Object value : sessionIds) {
            String sessionId = value.toString();
            String tokenHash = redis.opsForValue().get(sessionIdKey(sessionId));
            SessionData session = tokenHash == null ? null : readSession(tokenHash);
            if (session == null || !context.userId().equals(session.userId())) {
                continue;
            }
            SessionInfo info = session.toInfo();
            info.setCurrent(sessionId.equals(context.sessionId()));
            sessions.add(info);
        }
        sessions.sort(Comparator.comparing(SessionInfo::getLoginAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return sessions;
    }

    private SessionInfo newSession(String sessionId, LoginCommand command) {
        SessionInfo session = new SessionInfo();
        session.setSessionId(sessionId);
        session.setDeviceId(command.getDeviceId());
        session.setLoginIp(command.getLoginIp());
        session.setUserAgent(command.getUserAgent());
        session.setLoginAt(LocalDateTime.now());
        return session;
    }

    private String newSessionToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    private String sessionValue(SessionInfo info, String userId, SessionPolicy policy) {
        try {
            return objectMapper.writeValueAsString(new SessionData(info, userId, policy));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize session", exception);
        }
    }

    private SessionData readSession(String tokenHash) {
        try {
            String value = redis.opsForValue().get(sessionKey(tokenHash));
            return value == null ? null : objectMapper.readValue(value, SessionData.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private void deleteSessionValues(String sessionId) {
        String tokenHash = redis.opsForValue().get(sessionIdKey(sessionId));
        if (tokenHash != null) {
            redis.delete(sessionKey(tokenHash));
            redis.delete(sessionIdKey(sessionId));
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte valueByte : digest) {
                result.append(String.format("%02x", valueByte & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private record SessionData(String sessionId, String userId, String deviceId, String loginIp,
                               String userAgent, String loginAt, long ttlMillis, long renewWindowMillis) {
        private SessionData(SessionInfo session, String userId, SessionPolicy policy) {
            this(session.getSessionId(), userId, session.getDeviceId(), session.getLoginIp(), session.getUserAgent(),
                    session.getLoginAt() == null ? null : session.getLoginAt().toString(),
                    policy.ttl().toMillis(), policy.renewWindow().toMillis());
        }

        private SessionInfo toInfo() {
            SessionInfo info = new SessionInfo();
            info.setSessionId(sessionId);
            info.setDeviceId(deviceId);
            info.setLoginIp(loginIp);
            info.setUserAgent(userAgent);
            try {
                info.setLoginAt(loginAt == null ? null : LocalDateTime.parse(loginAt));
            } catch (DateTimeParseException ignored) {
                info.setLoginAt(null);
            }
            return info;
        }
    }

    private record SessionPolicy(Duration ttl, Duration renewWindow) {
    }
}

