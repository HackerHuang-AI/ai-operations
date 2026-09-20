package com.ai.operations.infrastructure.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
public class NacosConfig {

    @Value("${spring.cloud.nacos.config.server-addr:}")
    private String serverAddr;

    @Value("${spring.cloud.nacos.config.group:${spring.application.name}}")
    private String group;

    @Value("${spring.cloud.nacos.config.username:}")
    private String username;

    @Value("${spring.cloud.nacos.config.password:}")
    private String password;

    private ConfigService configService;

    @PostConstruct
    public void init() {
        if (!isAvailable()) {
            log.warn("[NacosConfig] Nacos 地址未配置，跳过动态配置初始化");
            return;
        }
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", serverAddr);
            if (!username.isBlank()) {
                properties.put("username", username);
                properties.put("password", password);
            }
            configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            log.error("[NacosConfig] 创建 ConfigService 失败", e);
        }
    }

    public boolean isAvailable() {
        return serverAddr != null && !serverAddr.isBlank();
    }

    public String getConfig(String dataId) {
        if (configService == null) {
            return null;
        }
        try {
            return configService.getConfig(dataId, group, 5_000);
        } catch (NacosException e) {
            log.error("[NacosConfig] 获取配置失败，dataId={}", dataId, e);
            return null;
        }
    }

    public void addListener(String dataId, Listener listener) {
        if (configService == null) {
            log.warn("[NacosConfig] ConfigService 未初始化，无法订阅 dataId={}", dataId);
            return;
        }
        try {
            configService.addListener(dataId, group, listener);
        } catch (NacosException e) {
            log.error("[NacosConfig] 订阅配置失败，dataId={}", dataId, e);
        }
    }
}

