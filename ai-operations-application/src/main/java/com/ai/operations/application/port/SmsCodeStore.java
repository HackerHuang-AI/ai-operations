package com.ai.operations.application.port;

import java.time.Duration;
import java.util.Optional;

/**
 * 短信验证码存取端口。
 *
 * <p>验证码的缓存、发送频率限制由 infrastructure 实现（Redis），
 * 哈希与键布局对业务层透明。
 */
public interface SmsCodeStore {

    /** 尝试获得发送资格（60s 内首次返回 true；限流实现的原子语义） */
    boolean tryAcquireSendLimit(String mobileHash, Duration limitTtl);

    /** 保存验证码 */
    void saveCode(String mobileHash, String code, Duration ttl);

    /** 读取并删除验证码（一次性消费），不存在返回 empty */
    Optional<String> consumeCode(String mobileHash);

    /** 清除验证码与发送限流键（短信发送失败时回滚） */
    void clear(String mobileHash);
}
