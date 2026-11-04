package com.ai.operations.application.service;

public interface SmsSender {
    void sendLoginCode(String mobile, String code);
}

