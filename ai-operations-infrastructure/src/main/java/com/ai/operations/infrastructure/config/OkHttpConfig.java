package com.ai.operations.infrastructure.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                // 关闭 OkHttp 内建静默重试：重试统一由 RetryUtil 按 RetryParam 控制，避免两层重试相乘
                .retryOnConnectionFailure(false)
                .build();
    }
}

