package com.ai.operations.application.service.impl;

import com.ai.operations.application.auth.AuthenticatedIdentity;
import com.ai.operations.application.auth.LoginCommand;
import com.ai.operations.application.auth.LoginTypeEnum;
import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.enums.IdentityProviderEnum;
import com.ai.operations.application.enums.IdentityTypeEnum;
import com.ai.operations.application.service.LoginAuthenticator;
import com.ai.operations.application.service.SmsCodeService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class SmsLoginAuthenticatorImpl implements LoginAuthenticator {
    private final SmsCodeService smsCodeService;

    public SmsLoginAuthenticatorImpl(SmsCodeService smsCodeService) {
        this.smsCodeService = smsCodeService;
    }

    @Override
    public LoginTypeEnum supports() {
        return LoginTypeEnum.MOBILE_SMS;
    }

    @Override
    public AuthenticatedIdentity authenticate(LoginCommand command) {
        List<ErrorCodeEnum> errors = new ArrayList<>();
        if (!StringUtils.hasText(command.getMobile())) {
            errors.add(ErrorCodeEnum.MOBILE_REQUIRED);
        }
        if (!StringUtils.hasText(command.getSmsCode())) {
            errors.add(ErrorCodeEnum.SMS_CODE_REQUIRED);
        }
        if (!errors.isEmpty()) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, errors);
        }
        smsCodeService.consumeLoginCode(command.getMobile(), command.getSmsCode());
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setIdentityType(IdentityTypeEnum.MOBILE.getValue());
        identity.setIdentityProvider(IdentityProviderEnum.SMS.getValue());
        identity.setIdentityValue(command.getMobile());
        return identity;
    }
}

