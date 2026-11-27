package com.ai.operations.infrastructure.repository;

import com.ai.operations.application.enums.NacosDataIdEnum;
import com.ai.operations.application.port.NacosConfigPort;
import com.ai.operations.infrastructure.config.NacosConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NacosConfigPort 实现：直接委托 NacosConfig 动态配置中心。
 *
 * <p>NacosConfig 内部已实现缓存与热更新（Listener），本仓储仅做
 * 端口适配，无额外状态。
 */
@Component
@RequiredArgsConstructor
public class NacosConfigRepository implements NacosConfigPort {

    private final NacosConfig nacosConfig;

    @Override
    public <T> T getObject(NacosDataIdEnum dataId, String key, Class<T> clazz) {
        return nacosConfig.getObject(dataId, key, clazz);
    }

    @Override
    public <T> T getDataIdAsObject(NacosDataIdEnum dataId, Class<T> clazz) {
        return nacosConfig.getDataIdAsObject(dataId, clazz);
    }

    @Override
    public <T> List<T> getDataIdAsList(NacosDataIdEnum dataId, String key, Class<T> elementType) {
        return nacosConfig.getList(dataId, key, elementType);
    }
}
