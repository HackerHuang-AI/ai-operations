package com.ai.operations.application.service;

import com.ai.operations.application.auth.LoginCommand;
import com.ai.operations.application.auth.LoginResult;
import com.ai.operations.application.auth.SessionInfo;

import java.util.List;

public interface LoginService {
    LoginResult login(LoginCommand command);

    void logout();

    void logout(String sessionId);

    void logoutAll();

    List<SessionInfo> listSessions();
}

