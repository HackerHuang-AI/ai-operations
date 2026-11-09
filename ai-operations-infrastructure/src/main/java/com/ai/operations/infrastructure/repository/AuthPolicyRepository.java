package com.ai.operations.infrastructure.repository;

import com.ai.operations.application.config.OperationsAuthConfig;
import com.ai.operations.application.port.AuthPolicyPort;
import com.ai.operations.infrastructure.config.NacosConfig;
import com.ai.operations.infrastructure.enums.NacosDataIdEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AuthPolicyPort 实现：从 Nacos 动态配置读取认证策略。
 */
@Component
@RequiredArgsConstructor
public class AuthPolicyRepository implements AuthPolicyPort {

    private final NacosConfig nacosConfig;

    @Override
    public OperationsAuthConfig authConfig() {
        return nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_OPERATIONS_AUTH, OperationsAuthConfig.class);
    }
}
