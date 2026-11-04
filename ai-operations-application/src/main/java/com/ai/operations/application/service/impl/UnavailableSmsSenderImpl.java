package com.ai.operations.application.service.impl;

import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.service.SmsSender;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"stag", "prod"})
public class UnavailableSmsSenderImpl implements SmsSender {
    @Override
    public void sendLoginCode(String mobile, String code) {
        throw new BizException(ErrorCodeEnum.SMS_SERVICE_UNAVAILABLE);
    }
}

