package com.ai.operations.application.service;

import com.ai.operations.application.auth.AuthenticatedIdentity;
import com.ai.operations.application.auth.LoginCommand;
import com.ai.operations.application.auth.LoginTypeEnum;

public interface LoginAuthenticator {
    LoginTypeEnum supports();

    AuthenticatedIdentity authenticate(LoginCommand command);
}

