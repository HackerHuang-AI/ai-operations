package com.ai.operations.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.ai.operations.infrastructure.persistence.mapper")
@SpringBootApplication(scanBasePackages = "com.ai.operations")
public class AiOperationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiOperationsApplication.class, args);
    }
}

