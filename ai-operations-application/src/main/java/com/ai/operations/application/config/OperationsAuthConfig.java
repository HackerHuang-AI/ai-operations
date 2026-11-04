package com.ai.operations.application.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationsAuthConfig {
    private SessionConfig session;

    @Getter
    @Setter
    public static class SessionConfig {
        private Integer maxDevices;
        private Integer ttlMinutes;
        private Integer renewWindowMinutes;
    }
}

