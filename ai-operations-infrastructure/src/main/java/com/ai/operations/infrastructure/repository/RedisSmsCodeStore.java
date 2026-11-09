package com.ai.operations.infrastructure.repository;

import com.ai.operations.application.port.SmsCodeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * SmsCodeStore 实现：Redis 缓存验证码 + 发送限流（键：手机号 SHA-256）。
 */
@Component
@RequiredArgsConstructor
public class RedisSmsCodeStore implements SmsCodeStore {

    private final StringRedisTemplate redis;

    @Override
    public boolean tryAcquireSendLimit(String mobileHash, Duration limitTtl) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(limitKey(mobileHash), "1", limitTtl));
    }

    @Override
    public void saveCode(String mobileHash, String code, Duration ttl) {
        redis.opsForValue().set(codeKey(mobileHash), code, ttl);
    }

    @Override
    public Optional<String> consumeCode(String mobileHash) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(codeKey(mobileHash)));
    }

    @Override
    public void clear(String mobileHash) {
        redis.delete(codeKey(mobileHash));
        redis.delete(limitKey(mobileHash));
    }

    private String codeKey(String mobileHash) {
        return "ai-operations:auth:sms-code:" + mobileHash;
    }

    private String limitKey(String mobileHash) {
        return "ai-operations:auth:sms-limit:" + mobileHash;
    }
}
