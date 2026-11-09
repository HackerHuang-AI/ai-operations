package com.ai.operations.application.port;

import com.ai.operations.application.config.OperationsAuthConfig;

/**
 * 认证与登录策略配置端口。
 *
 * <p>会话 TTL、续期窗口、同账号最大设备数等策略来自运维侧
 * 动态配置，由 infrastructure 实现（Nacos）提供。
 */
public interface AuthPolicyPort {

    /** 读取当前生效的认证配置（未配置时实现方可返回 null，业务层有默认值兜底） */
    OperationsAuthConfig authConfig();
}
