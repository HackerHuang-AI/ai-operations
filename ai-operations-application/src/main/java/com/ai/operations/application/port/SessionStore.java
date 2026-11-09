package com.ai.operations.application.port;

import com.ai.operations.application.model.SessionRecord;

import java.util.List;
import java.util.Optional;

/**
 * 登录会话存取端口。
 *
 * <p>会话双写（tokenHash → 会话记录、sessionId → tokenHash）、
 * 设备槽位的原子替换与回滚、登录态查询全部收敛在本端口。
 * 实现方（infrastructure）负责 Redis 键布局、Lua 脚本与序列化。
 */
public interface SessionStore {

    /**
     * 原子替换设备槽位（同设备旧会话被顶替）。
     * 超过最大设备数时返回 deviceLimitExceeded=true，业务层应拒绝登录。
     */
    DeviceSlot replaceDeviceSlot(String userId, String deviceId, String sessionId,
                                 long ttlMillis, int maxDevices);

    /** 回滚设备槽位（会话写入失败时恢复） */
    void rollbackDeviceSlot(String userId, String deviceId, String sessionId, String oldSessionId);

    /** 写入会话（tokenHash 与 sessionId 双键映射，TTL 相同） */
    void saveSession(SessionRecord session, String tokenHash);

    /** 由 sessionId 反查 tokenHash */
    Optional<String> findTokenHash(String sessionId);

    /** 由 tokenHash 读取会话记录 */
    Optional<SessionRecord> findSession(String tokenHash);

    /** 删除 tokenHash 与 sessionId 两个键 */
    void deleteSession(String tokenHash, String sessionId);

    /** 当前用户的全部 sessionId */
    List<String> listSessionIds(String userId);

    /** 移除设备槽位 */
    void removeDevice(String userId, String deviceId);

    /** 设备槽位替换结果 */
    record DeviceSlot(String oldSessionId, boolean deviceLimitExceeded) {

        public static DeviceSlot of(String oldSessionId) {
            return new DeviceSlot(oldSessionId, false);
        }
    }
}
