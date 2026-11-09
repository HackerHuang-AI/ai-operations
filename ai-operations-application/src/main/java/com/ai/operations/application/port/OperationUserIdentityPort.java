package com.ai.operations.application.port;

import java.util.Optional;

/**
 * 运营用户身份凭据查询端口。
 */
public interface OperationUserIdentityPort {

    /** 按三元组（类型/提供方/值）查询关联的用户 ID，不存在返回 empty */
    Optional<String> findUserIdByIdentity(String identityType, String identityProvider, String identityValue);
}
