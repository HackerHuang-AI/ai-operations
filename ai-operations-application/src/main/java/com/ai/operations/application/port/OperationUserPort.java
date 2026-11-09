package com.ai.operations.application.port;

import com.ai.operations.application.model.OperationUserBO;

import java.util.Optional;

/**
 * 运营用户查询端口。
 */
public interface OperationUserPort {

    /** 查询状态正常（status=1）的用户，不存在或被禁用返回 empty */
    Optional<OperationUserBO> findActiveByUserId(String userId);
}
