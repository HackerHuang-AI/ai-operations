package com.ai.operations.infrastructure.config;

import com.ai.operations.infrastructure.enums.NacosDataIdEnum;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Nacos 动态配置中心，通过索引 DataId 管理业务配置订阅与缓存。
 */
@Slf4j
@Component
public class NacosConfig {

    private static final Set<String> VALID_SUFFIXES = Set.of(".json", ".properties", ".yaml", ".yml");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    @Value("${spring.cloud.nacos.config.server-addr:}")
    private String serverAddr;

    @Value("${spring.application.name}")
    private String group;

    @Value("${spring.cloud.nacos.config.namespace}")
    private String namespace;

    @Value("${spring.cloud.nacos.config.username:}")
    private String username;

    @Value("${spring.cloud.nacos.config.password:}")
    private String password;

    @Value("${nacos.index-data-id:ai-operations-index.properties}")
    private String indexDataId;

    private final Map<String, Map<String, String>> dataIdCache = new ConcurrentHashMap<>();
    private final Map<String, String> rawContentCache = new ConcurrentHashMap<>();
    private final Set<String> registeredDataIds = ConcurrentHashMap.newKeySet();

    private ConfigService configService;

    @PostConstruct
    public void init() {
        if (!isAvailable()) {
            log.warn("[NacosConfig] spring.cloud.nacos.config.server-addr 未配置，跳过 Nacos 初始化");
            return;
        }
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", serverAddr);
            properties.put("namespace", namespace);
            properties.put("appName", group);
            if (!username.isBlank()) {
                properties.put("username", username);
                properties.put("password", password);
            }
            configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            log.error("[NacosConfig] 创建 ConfigService 失败，serverAddr={}", serverAddr, e);
            return;
        }
        listenIndexDataId();
    }

    public boolean isAvailable() {
        return serverAddr != null && !serverAddr.isBlank();
    }

    private void listenIndexDataId() {
        try {
            String content = configService.getConfig(indexDataId, group, 5_000);
            if (content == null || content.isBlank()) {
                log.warn("[NacosConfig] 索引 DataId 内容为空，dataId={}", indexDataId);
            } else {
                parseIndexContent(content).forEach(this::listenBusinessDataId);
            }
            configService.addListener(indexDataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String content) {
                    onIndexChanged(content);
                }
            });
        } catch (NacosException e) {
            log.error("[NacosConfig] 订阅索引 DataId 失败，dataId={}", indexDataId, e);
        }
    }

    private void onIndexChanged(String content) {
        if (content == null || content.isBlank()) {
            log.warn("[NacosConfig] 索引 DataId 内容变为空，跳过 diff");
            return;
        }
        Set<String> dataIds = parseIndexContent(content);
        dataIds.forEach(this::listenBusinessDataId);
        Set<String> removedDataIds = new HashSet<>(registeredDataIds);
        removedDataIds.removeAll(dataIds);
        removedDataIds.forEach(dataId -> {
            dataIdCache.remove(dataId);
            rawContentCache.remove(dataId);
            registeredDataIds.remove(dataId);
        });
    }

    private void listenBusinessDataId(String dataId) {
        if (!registeredDataIds.add(dataId)) {
            return;
        }
        try {
            String content = configService.getConfig(dataId, group, 5_000);
            if (content == null || content.isBlank()) {
                log.warn("[NacosConfig] 业务 DataId 内容为空，dataId={}", dataId);
            } else {
                updateCache(dataId, content);
            }
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String content) {
                    if (content == null || content.isBlank()) {
                        dataIdCache.remove(dataId);
                        rawContentCache.remove(dataId);
                        return;
                    }
                    updateCache(dataId, content);
                }
            });
        } catch (NacosException e) {
            registeredDataIds.remove(dataId);
            log.error("[NacosConfig] 订阅业务 DataId 失败，dataId={}", dataId, e);
        }
    }

    private Set<String> parseIndexContent(String content) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            return properties.stringPropertyNames();
        } catch (Exception e) {
            log.error("[NacosConfig] 解析索引文件失败", e);
            return Set.of();
        }
    }

    private void updateCache(String dataId, String content) {
        try {
            dataIdCache.put(dataId, parseContent(dataId, content));
            rawContentCache.put(dataId, content);
        } catch (Exception e) {
            log.error("[NacosConfig] 解析配置失败，dataId={}", dataId, e);
        }
    }

    private Map<String, String> parseContent(String dataId, String content) throws Exception {
        if (dataId.endsWith(".json")) {
            return parseStructuredContent(dataId, JSON_MAPPER.readTree(content));
        }
        if (dataId.endsWith(".yaml") || dataId.endsWith(".yml")) {
            return parseStructuredContent(dataId, YAML_MAPPER.readTree(content));
        }
        Properties properties = new Properties();
        properties.load(new StringReader(content));
        Map<String, String> result = new HashMap<>();
        properties.forEach((key, value) -> result.put(key.toString(), value.toString()));
        return result;
    }

    private Map<String, String> parseStructuredContent(String dataId, JsonNode root) throws Exception {
        if (!root.isObject()) {
            return Map.of(dataId, JSON_MAPPER.writeValueAsString(root));
        }
        Map<String, String> result = new HashMap<>();
        root.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            result.put(entry.getKey(), value.isValueNode() ? value.asText() : value.toString());
        });
        return result;
    }

    /** 保持旧调用兼容；优先返回缓存中的原文，未缓存时直接读取 Nacos。 */
    public String getConfig(String dataId) {
        String cachedContent = rawContentCache.get(dataId);
        if (cachedContent != null || configService == null) {
            return cachedContent;
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
            log.error("[NacosConfig] 外部 listener 注册失败，dataId={}", dataId, e);
        }
    }

    public Map<String, String> getCacheByDataId(String dataId) {
        return dataIdCache.get(dataId);
    }

    public String getRawContent(String dataId) {
        return rawContentCache.get(dataId);
    }

    public String getRaw(String key) {
        for (Map<String, String> bucket : dataIdCache.values()) {
            String value = bucket.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public String getRaw(String dataId, String key) {
        Map<String, String> bucket = dataIdCache.get(dataId);
        return bucket == null ? null : bucket.get(key);
    }

    public <T> T deserialize(String dataId, String content, Class<T> clazz) throws Exception {
        if (dataId.endsWith(".json")) {
            return JSON_MAPPER.readValue(content, clazz);
        }
        if (dataId.endsWith(".yaml") || dataId.endsWith(".yml")) {
            return YAML_MAPPER.readValue(content, clazz);
        }
        throw new IllegalArgumentException("仅支持 .json / .yaml / .yml 格式整体反序列化，dataId=" + dataId);
    }

    public <T> List<T> deserializeList(String dataId, String content, Class<T> elementType) throws Exception {
        ObjectMapper mapper = dataId.endsWith(".json") ? JSON_MAPPER : dataId.endsWith(".yaml") || dataId.endsWith(".yml") ? YAML_MAPPER : null;
        if (mapper == null) {
            throw new IllegalArgumentException("仅支持 .json / .yaml / .yml 格式整体反序列化，dataId=" + dataId);
        }
        CollectionType listType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return mapper.readValue(content, listType);
    }

    public String getString(NacosDataIdEnum dataId, String key, String defaultValue) {
        String value = getRawInternal(dataId, key);
        return value == null ? defaultValue : value;
    }

    public int getInt(NacosDataIdEnum dataId, String key, int defaultValue) {
        try {
            String value = getRawInternal(dataId, key);
            return value == null ? defaultValue : Integer.parseInt(value.trim());
        } catch (Exception e) {
            log.error("[NacosConfig] 获取 int 配置失败，dataId={}，key={}", dataId.dataId(), key, e);
            return defaultValue;
        }
    }

    public long getLong(NacosDataIdEnum dataId, String key, long defaultValue) {
        try {
            String value = getRawInternal(dataId, key);
            return value == null ? defaultValue : Long.parseLong(value.trim());
        } catch (Exception e) {
            log.error("[NacosConfig] 获取 long 配置失败，dataId={}，key={}", dataId.dataId(), key, e);
            return defaultValue;
        }
    }

    public double getDouble(NacosDataIdEnum dataId, String key, double defaultValue) {
        try {
            String value = getRawInternal(dataId, key);
            return value == null ? defaultValue : Double.parseDouble(value.trim());
        } catch (Exception e) {
            log.error("[NacosConfig] 获取 double 配置失败，dataId={}，key={}", dataId.dataId(), key, e);
            return defaultValue;
        }
    }

    public boolean getBoolean(NacosDataIdEnum dataId, String key, boolean defaultValue) {
        String value = getRawInternal(dataId, key);
        if (value == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }
        log.error("[NacosConfig] 配置值不是合法 boolean，dataId={}，key={}", dataId.dataId(), key);
        return defaultValue;
    }

    public <T> T getObject(NacosDataIdEnum dataId, String key, Class<T> clazz) {
        try {
            String value = getRawInternal(dataId, key);
            return value == null ? null : JSON_MAPPER.readValue(value, clazz);
        } catch (Exception e) {
            log.error("[NacosConfig] 获取对象配置失败，dataId={}，key={}", dataId.dataId(), key, e);
            return null;
        }
    }

    public <T> T getObject(NacosDataIdEnum dataId, String key, TypeReference<T> typeReference) {
        try {
            String value = getRawInternal(dataId, key);
            return value == null ? null : JSON_MAPPER.readValue(value, typeReference);
        } catch (Exception e) {
            log.error("[NacosConfig] 获取对象配置失败，dataId={}，key={}", dataId.dataId(), key, e);
            return null;
        }
    }

    public <T> List<T> getList(NacosDataIdEnum dataId, String key, Class<T> elementType) {
        try {
            String value = getRawInternal(dataId, key);
            return value == null ? Collections.emptyList() : JSON_MAPPER.readValue(value,
                    JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            log.error("[NacosConfig] 获取列表配置失败，dataId={}，key={}", dataId.dataId(), key, e);
            return Collections.emptyList();
        }
    }

    public <K, V> Map<K, V> getMap(NacosDataIdEnum dataId, String key, Class<K> keyClass, Class<V> valueClass) {
        try {
            String value = getRawInternal(dataId, key);
            if (value == null) {
                return Collections.emptyMap();
            }
            return JSON_MAPPER.readValue(value, JSON_MAPPER.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
        } catch (Exception e) {
            log.error("[NacosConfig] 获取 Map 配置失败，dataId={}，key={}", dataId.dataId(), key, e);
            return Collections.emptyMap();
        }
    }

    public <T> Set<T> getSet(NacosDataIdEnum dataId, String key, Class<T> elementType) {
        return Set.copyOf(getList(dataId, key, elementType));
    }

    public <T> T getDataIdAsObject(NacosDataIdEnum dataId, Class<T> clazz) {
        try {
            String content = getRawContentInternal(dataId);
            return content == null ? null : deserialize(dataId.dataId(), content, clazz);
        } catch (Exception e) {
            log.error("[NacosConfig] 整体读取配置失败，dataId={}", dataId.dataId(), e);
            return null;
        }
    }

    public <T> List<T> getDataIdAsList(NacosDataIdEnum dataId, Class<T> elementType) {
        try {
            String content = getRawContentInternal(dataId);
            return content == null ? Collections.emptyList() : deserializeList(dataId.dataId(), content, elementType);
        } catch (Exception e) {
            log.error("[NacosConfig] 整体读取列表配置失败，dataId={}", dataId.dataId(), e);
            return Collections.emptyList();
        }
    }

    private String getRawInternal(NacosDataIdEnum dataId, String key) {
        validateDataId(dataId.dataId());
        return getRaw(dataId.dataId(), key);
    }

    private String getRawContentInternal(NacosDataIdEnum dataId) {
        String dataIdValue = dataId.dataId();
        validateDataId(dataIdValue);
        if (dataIdValue.endsWith(".properties")) {
            throw new IllegalArgumentException(".properties 格式不支持整体读取，dataId=" + dataIdValue);
        }
        return getRawContent(dataIdValue);
    }

    private void validateDataId(String dataId) {
        if (VALID_SUFFIXES.stream().noneMatch(dataId::endsWith)) {
            throw new IllegalArgumentException("DataId 后缀不合法: " + dataId);
        }
    }
}

