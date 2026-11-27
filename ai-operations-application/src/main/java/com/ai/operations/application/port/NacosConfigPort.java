package com.ai.operations.application.port;

import com.ai.operations.application.enums.NacosDataIdEnum;

import java.util.List;

/**
 * 动态配置查询端口（Nacos）。
 *
 * <p>application 业务层通过本端口读取动态配置。实现方负责与 Nacos 的
 * 连接管理、缓存与热更新，业务语义与具体配置中心解耦。
 *
 * <p>约定：本模块所有 application 层配置读取统一走此端口；
 * infrastructure 内部的连接/线程池类配置（RetryConfig、OkHttpConfig 等）
 * 允许直接依赖 NacosConfig，无需绕经本端口。
 */
public interface NacosConfigPort {

    /** 读取 dataId 下指定 key 的配置对象，不存在时返回 null */
    <T> T getObject(NacosDataIdEnum dataId, String key, Class<T> clazz);

    /** 将整个 dataId 的 JSON 内容作为对象读取，不存在时返回 null */
    <T> T getDataIdAsObject(NacosDataIdEnum dataId, Class<T> clazz);

    /** 读取 dataId 下指定 key 的对象列表，不存在时返回空列表 */
    <T> List<T> getDataIdAsList(NacosDataIdEnum dataId, String key, Class<T> elementType);
}
