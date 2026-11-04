package com.ai.operations.application.service.impl;

import com.ai.operations.application.auth.AuthenticatedIdentity;
import com.ai.operations.application.auth.LoginCommand;
import com.ai.operations.application.auth.LoginTypeEnum;
import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.enums.IdentityProviderEnum;
import com.ai.operations.application.enums.IdentityTypeEnum;
import com.ai.operations.application.service.LoginAuthenticator;
import com.ai.operations.infrastructure.persistence.entity.OperationUserEntity;
import com.ai.operations.infrastructure.persistence.entity.OperationUserIdentityEntity;
import com.ai.operations.infrastructure.persistence.mapper.OperationUserIdentityMapper;
import com.ai.operations.infrastructure.persistence.mapper.OperationUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordLoginAuthenticatorImpl implements LoginAuthenticator {
    private final OperationUserIdentityMapper identityMapper;
    private final OperationUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordLoginAuthenticatorImpl(OperationUserIdentityMapper identityMapper, OperationUserMapper userMapper) {
        this.identityMapper = identityMapper;
        this.userMapper = userMapper;
    }

    @Override
    public LoginTypeEnum supports() {
        return LoginTypeEnum.ACCOUNT_PASSWORD;
    }

    @Override
    public AuthenticatedIdentity authenticate(LoginCommand command) {
        List<ErrorCodeEnum> errors = new ArrayList<>();
        if (!StringUtils.hasText(command.getAccount())) {
            errors.add(ErrorCodeEnum.ACCOUNT_REQUIRED);
        }
        if (!StringUtils.hasText(command.getPassword())) {
            errors.add(ErrorCodeEnum.PASSWORD_REQUIRED);
        }
        if (!errors.isEmpty()) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, errors);
        }
        OperationUserIdentityEntity identity = identityMapper.selectOne(new LambdaQueryWrapper<OperationUserIdentityEntity>()
                .eq(OperationUserIdentityEntity::getIdentityType, IdentityTypeEnum.ACCOUNT.getValue())
                .eq(OperationUserIdentityEntity::getIdentityProvider, IdentityProviderEnum.LOCAL.getValue())
                .eq(OperationUserIdentityEntity::getIdentityValue, command.getAccount()));
        if (identity == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        OperationUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<OperationUserEntity>()
                .eq(OperationUserEntity::getUserId, identity.getUserId())
                .eq(OperationUserEntity::getStatus, 1));
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        AuthenticatedIdentity authenticated = new AuthenticatedIdentity();
        authenticated.setUserId(identity.getUserId());
        authenticated.setIdentityType(IdentityTypeEnum.ACCOUNT.getValue());
        authenticated.setIdentityProvider(IdentityProviderEnum.LOCAL.getValue());
        authenticated.setIdentityValue(command.getAccount());
        return authenticated;
    }
}

