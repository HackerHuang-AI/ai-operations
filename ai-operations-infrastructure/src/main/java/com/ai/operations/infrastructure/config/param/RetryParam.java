package com.ai.operations.infrastructure.config.param;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 重试参数配置实体，对应 Nacos ai-operations-retry.json 中各 scope 配置块（如 "default"）。
 *
 * <p>两个开关彼此正交，可按平台/场景自由组合：
 * <ul>
 *   <li>{@link #retryOnSocket}：socket 网络异常（{@link java.net.SocketException} /
 *       {@link java.net.SocketTimeoutException}）是否重试，默认 true；</li>
 *   <li>{@link #retryableHttpCodes}：HTTP 状态码白名单，默认空表示不按状态码重试，
 *       上游返回任何状态码都直接透传给调用方；</li>
 *   <li>重试等待为指数退避 + 0~10% 随机抖动，单次等待不超过 {@link #maxWaitMs}。</li>
 * </ul>
 * 因此「只重网络」「只重状态码」「两者都重」「都不重」四种策略均可通过配置表达，
 * 具体重试哪些状态码不在代码中限定，由配置中心按环境/平台下发。
 *
 * <p>Nacos 配置示例：
 * <pre>{@code
 * "default": { "maxRetries": 3, "retryOnSocket": true, "retryableHttpCodes": [429, 502, 503, 504] }
 * }</pre>
 *
 * @author HUANGcong
 * @since 2026-04-20
 */
@Data
public class RetryParam {

    /** 最大重试次数（不含首次调用） */
    private int maxRetries = 3;

    /** 首次重试前基础等待时间（毫秒） */
    private long intervalMs = 500;

    /**
     * 指数退避乘数。
     * 第 n 次重试等待时间 = intervalMs * multiplier^n
     */
    private double backoffMultiplier = 2.0;

    /**
     * 单次等待上限（毫秒），防止退避时间无限增长。
     */
    private long maxWaitMs = 30_000;

    /**
     * socket 网络异常是否重试，默认 true。
     * 置为 false 时，即使 {@link #retryableHttpCodes} 非空，也只按状态码重试。
     */
    private boolean retryOnSocket = true;

    /**
     * 触发重试的 HTTP 状态码白名单。
     * 默认为空：上游返回任何状态码（含 401/403/404/429/5xx）都不重试，原样透传；
     * 需要对 429/50x 等瞬时故障重试时，在 Nacos 配置中心下发。
     */
    private List<Integer> retryableHttpCodes = Collections.emptyList();
}
