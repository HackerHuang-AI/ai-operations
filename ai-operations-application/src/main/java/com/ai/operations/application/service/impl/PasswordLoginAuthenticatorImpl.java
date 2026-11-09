package com.ai.operations.application.service.impl;

import com.ai.operations.application.auth.AuthenticatedIdentity;
import com.ai.operations.application.auth.LoginCommand;
import com.ai.operations.application.auth.LoginTypeEnum;
import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.enums.IdentityProviderEnum;
import com.ai.operations.application.enums.IdentityTypeEnum;
import com.ai.operations.application.model.OperationUserBO;
import com.ai.operations.application.port.OperationUserIdentityPort;
import com.ai.operations.application.port.OperationUserPort;
import com.ai.operations.application.service.LoginAuthenticator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordLoginAuthenticatorImpl implements LoginAuthenticator {
    private final OperationUserIdentityPort identityPort;
    private final OperationUserPort userPort;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordLoginAuthenticatorImpl(OperationUserIdentityPort identityPort, OperationUserPort userPort) {
        this.identityPort = identityPort;
        this.userPort = userPort;
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
        String userId = identityPort.findUserIdByIdentity(IdentityTypeEnum.ACCOUNT.getValue(),
                IdentityProviderEnum.LOCAL.getValue(), command.getAccount())
                .orElseThrow(() -> new BizException(ErrorCodeEnum.LOGIN_FAILED));
        OperationUserBO user = userPort.findActiveByUserId(userId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.LOGIN_FAILED));
        if (user.passwordHash() == null || !passwordEncoder.matches(command.getPassword(), user.passwordHash())) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        AuthenticatedIdentity authenticated = new AuthenticatedIdentity();
        authenticated.setUserId(userId);
        authenticated.setIdentityType(IdentityTypeEnum.ACCOUNT.getValue());
        authenticated.setIdentityProvider(IdentityProviderEnum.LOCAL.getValue());
        authenticated.setIdentityValue(command.getAccount());
        return authenticated;
    }
}
