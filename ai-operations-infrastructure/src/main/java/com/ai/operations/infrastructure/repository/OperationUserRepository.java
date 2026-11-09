package com.ai.operations.infrastructure.repository;

import com.ai.operations.application.model.OperationUserBO;
import com.ai.operations.application.port.OperationUserPort;
import com.ai.operations.infrastructure.persistence.entity.OperationUserEntity;
import com.ai.operations.infrastructure.persistence.mapper.OperationUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * OperationUserPort 实现：Entity → BO 转换下沉。
 */
@Component
@RequiredArgsConstructor
public class OperationUserRepository implements OperationUserPort {

    private final OperationUserMapper userMapper;

    @Override
    public Optional<OperationUserBO> findActiveByUserId(String userId) {
        OperationUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<OperationUserEntity>()
                .eq(OperationUserEntity::getUserId, userId)
                .eq(OperationUserEntity::getStatus, 1));
        return Optional.ofNullable(user)
                .map(u -> new OperationUserBO(u.getUserId(), u.getPasswordHash()));
    }
}
