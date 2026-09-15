package com.ai.operations.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ai.operations")
public class AiOperationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiOperationsApplication.class, args);
    }
}

