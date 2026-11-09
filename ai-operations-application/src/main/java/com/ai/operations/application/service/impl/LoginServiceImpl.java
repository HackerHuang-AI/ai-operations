package com.ai.operations.application.service.impl;

import com.ai.operations.application.auth.*;
import com.ai.operations.application.common.BizException;
import com.ai.operations.application.config.OperationsAuthConfig;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.model.OperationLoginAuditEntry;
import com.ai.operations.application.model.SessionRecord;
import com.ai.operations.application.port.AuthPolicyPort;
import com.ai.operations.application.port.LoginAuditPort;
import com.ai.operations.application.port.OperationUserIdentityPort;
import com.ai.operations.application.port.OperationUserPort;
import com.ai.operations.application.port.SessionStore;
import com.ai.operations.application.model.request.RequestContext;
import com.ai.operations.application.model.request.RequestContextHolder;
import com.ai.operations.application.service.LoginAuthenticator;
import com.ai.operations.application.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
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

    private final Map<String, LoginAuthenticator> authenticators;
    private final OperationUserIdentityPort identityPort;
    private final OperationUserPort userPort;
    private final LoginAuditPort loginAuditPort;
    private final SessionStore sessionStore;
    private final AuthPolicyPort authPolicyPort;
    private final Executor loginAuditExecutor;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginServiceImpl(List<LoginAuthenticator> authenticators, OperationUserIdentityPort identityPort,
                            OperationUserPort userPort, LoginAuditPort loginAuditPort, SessionStore sessionStore,
                            AuthPolicyPort authPolicyPort,
                            @Qualifier("loginAuditExecutor") Executor loginAuditExecutor) {
        this.authenticators = authenticators.stream().collect(Collectors.toMap(
                authenticator -> authenticator.supports().getValue(), Function.identity()));
        this.identityPort = identityPort;
        this.userPort = userPort;
        this.loginAuditPort = loginAuditPort;
        this.sessionStore = sessionStore;
        this.authPolicyPort = authPolicyPort;
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
            userId = identityPort.findUserIdByIdentity(identity.getIdentityType(),
                    identity.getIdentityProvider(), identity.getIdentityValue())
                    .orElseThrow(() -> new BizException(ErrorCodeEnum.LOGIN_FAILED));
        }
        userPort.findActiveByUserId(userId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.LOGIN_FAILED));
        String sessionId = newSessionId();
        String sessionToken = newSessionToken();
        String tokenHash = sha256(sessionToken);
        SessionInfo session = newSession(sessionId, command);
        SessionPolicy policy = sessionPolicy();
        SessionStore.DeviceSlot deviceSlot = replaceDeviceSession(userId, command.getDeviceId(), sessionId, policy.ttl());
        try {
            sessionStore.saveSession(toRecord(session, userId, policy), tokenHash);
        } catch (RuntimeException exception) {
            sessionStore.rollbackDeviceSlot(userId, command.getDeviceId(), sessionId, deviceSlot.oldSessionId());
            throw exception;
        }
        if (!deviceSlot.oldSessionId().isEmpty()) {
            deleteSessionValues(deviceSlot.oldSessionId());
        }
        writeLoginAuditAsync(userId, command, identity);
        LoginResult result = new LoginResult();
        result.setSessionToken(sessionToken);
        result.setSessionId(sessionId);
        result.setUserId(userId);
        return result;
    }

    private SessionStore.DeviceSlot replaceDeviceSession(String userId, String deviceId, String sessionId,
                                                         Duration ttl) {
        SessionStore.DeviceSlot deviceSlot = sessionStore.replaceDeviceSlot(userId, deviceId, sessionId,
                ttl.toMillis(), maxDevices());
        if (deviceSlot.deviceLimitExceeded()) {
            throw new BizException(ErrorCodeEnum.DEVICE_LIMIT_EXCEEDED);
        }
        return deviceSlot;
    }

    private int maxDevices() {
        OperationsAuthConfig config = authPolicyPort.authConfig();
        Integer value = config == null || config.getSession() == null ? null : config.getSession().getMaxDevices();
        return value == null || value <= 0 ? DEFAULT_MAX_DEVICES : value;
    }

    private SessionPolicy sessionPolicy() {
        OperationsAuthConfig config = authPolicyPort.authConfig();
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
        OperationLoginAuditEntry audit = new OperationLoginAuditEntry(userId, identity.getIdentityType(),
                sha256(identity.getIdentityValue()), 1, command.getLoginIp(), command.getUserAgent(),
                LocalDateTime.now());
        try {
            loginAuditExecutor.execute(() -> {
                try {
                    loginAuditPort.record(audit);
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
        sessionStore.listSessionIds(context.userId())
                .forEach(sessionId -> logout(context.userId(), sessionId));
    }

    private void logout(String userId, String sessionId) {
        String tokenHash = sessionStore.findTokenHash(sessionId).orElse(null);
        if (tokenHash == null) {
            return;
        }
        SessionRecord session = sessionStore.findSession(tokenHash).orElse(null);
        if (session == null || !userId.equals(session.userId())) {
            return;
        }
        deleteSessionValues(sessionId);
        sessionStore.removeDevice(userId, session.deviceId());
    }

    @Override
    public List<SessionInfo> listSessions() {
        RequestContext context = RequestContextHolder.current().orElseThrow(() -> new IllegalStateException("current user is unavailable"));
        List<String> sessionIds = sessionStore.listSessionIds(context.userId());
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        List<SessionInfo> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            String tokenHash = sessionStore.findTokenHash(sessionId).orElse(null);
            SessionRecord session = tokenHash == null ? null : sessionStore.findSession(tokenHash).orElse(null);
            if (session == null || !context.userId().equals(session.userId())) {
                continue;
            }
            SessionInfo info = toInfo(session);
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

    private String newSessionId() {
        return java.util.UUID.randomUUID().toString();
    }

    private SessionRecord toRecord(SessionInfo info, String userId, SessionPolicy policy) {
        return new SessionRecord(info.getSessionId(), userId, info.getDeviceId(), info.getLoginIp(),
                info.getUserAgent(), info.getLoginAt(), policy.ttl().toMillis(), policy.renewWindow().toMillis());
    }

    private SessionInfo toInfo(SessionRecord record) {
        SessionInfo info = new SessionInfo();
        info.setSessionId(record.sessionId());
        info.setDeviceId(record.deviceId());
        info.setLoginIp(record.loginIp());
        info.setUserAgent(record.userAgent());
        info.setLoginAt(record.loginAt());
        return info;
    }

    private void deleteSessionValues(String sessionId) {
        sessionStore.findTokenHash(sessionId).ifPresent(
                tokenHash -> sessionStore.deleteSession(tokenHash, sessionId));
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

    private record SessionPolicy(Duration ttl, Duration renewWindow) {
    }
}
