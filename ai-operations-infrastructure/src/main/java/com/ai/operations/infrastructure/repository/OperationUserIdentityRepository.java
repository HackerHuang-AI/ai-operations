package com.ai.operations.infrastructure.repository;

import com.ai.operations.application.port.OperationUserIdentityPort;
import com.ai.operations.infrastructure.persistence.entity.OperationUserIdentityEntity;
import com.ai.operations.infrastructure.persistence.mapper.OperationUserIdentityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * OperationUserIdentityPort 实现：MyBatis-Plus 查询。
 */
@Component
@RequiredArgsConstructor
public class OperationUserIdentityRepository implements OperationUserIdentityPort {

    private final OperationUserIdentityMapper identityMapper;

    @Override
    public Optional<String> findUserIdByIdentity(String identityType, String identityProvider,
                                                 String identityValue) {
        OperationUserIdentityEntity identity = identityMapper.selectOne(
                new LambdaQueryWrapper<OperationUserIdentityEntity>()
                        .eq(OperationUserIdentityEntity::getIdentityType, identityType)
                        .eq(OperationUserIdentityEntity::getIdentityProvider, identityProvider)
                        .eq(OperationUserIdentityEntity::getIdentityValue, identityValue));
        return Optional.ofNullable(identity).map(OperationUserIdentityEntity::getUserId);
    }
}
