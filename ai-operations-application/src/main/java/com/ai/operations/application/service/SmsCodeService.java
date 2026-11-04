package com.ai.operations.application.service;

public interface SmsCodeService {
    void sendLoginCode(String mobile);

    void consumeLoginCode(String mobile, String smsCode);
}

