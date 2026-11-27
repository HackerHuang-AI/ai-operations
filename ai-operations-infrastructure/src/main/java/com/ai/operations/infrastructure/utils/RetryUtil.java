package com.ai.operations.infrastructure.utils;

import com.ai.operations.infrastructure.config.param.RetryParam;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 白名单重试工具（全平台统一模版）。
 *
 * <p>职责单一：只负责「要不要重试、等多久」，不解析响应体、不做业务错误映射。
 * 编排（建 Request → 重试 → 读 body → 状态码判定）由上层 HTTP 调用编排器承担。
 *
 * <p>重试边界（详见 {@link RetryParam}）：
 * <ul>
 *   <li>socket 网络异常重试受 {@code retryOnSocket} 开关控制（默认开）；</li>
 *   <li>HTTP 状态码仅在命中 {@code retryableHttpCodes} 白名单时重试；</li>
 *   <li>线程中断立即终止（恢复中断标记后上抛），绝不继续消耗重试预算；</li>
 *   <li>响应被判定为可重试时先 {@link Response#close()} 释放连接，再等待下一次尝试。</li>
 * </ul>
 *
 * @author HUANGcong
 * @since 2026-11-12
 */
@Slf4j
public final class RetryUtil {

    private RetryUtil() {
    }

    /**
     * 带白名单重试地执行一次 HTTP 调用。
     *
     * @param callable 发起 OkHttp 同步调用并返回 Response（调用方保证 Response 由本方法消费方关闭）
     * @param param    重试参数
     * @return 上游 Response（可能为 null，与 callable 返回一致；未命中白名单时原样返回）
     * @throws Exception callable 抛出的最后一次异常（网络异常重试耗尽 / InterruptedException / 其他意外异常）
     */
    public static Response retryHttp(Callable<Response> callable, RetryParam param) throws Exception {
        List<Integer> codes = param.getRetryableHttpCodes();
        int maxRetries = param.getMaxRetries();
        long intervalMs = param.getIntervalMs();
        double multiplier = param.getBackoffMultiplier();
        long maxWaitMs = param.getMaxWaitMs();

        for (int attempt = 0; ; attempt++) {
            checkInterrupted();
            try {
                Response response = callable.call();
                if (response == null || codes == null
                        || !codes.contains(response.code()) || attempt >= maxRetries) {
                    return response;
                }
                log.warn("[RetryUtil] HTTP {} 命中白名单，准备第 {}/{} 次重试",
                        response.code(), attempt + 1, maxRetries);
                response.close();
            } catch (SocketException | SocketTimeoutException e) {
                if (!param.isRetryOnSocket() || attempt >= maxRetries) {
                    throw e;
                }
                log.warn("[RetryUtil] socket 异常，准备第 {}/{} 次重试，type={}",
                        attempt + 1, maxRetries, e.getClass().getSimpleName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            sleep(computeWait(attempt, intervalMs, multiplier, maxWaitMs));
        }
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("retry cancelled before attempt");
        }
    }

    /**
     * 指数退避 + 0~10% 随机抖动，单次等待以 maxWaitMs 封顶。
     */
    private static long computeWait(int attempt, long intervalMs, double multiplier, long maxWaitMs) {
        double base = intervalMs * Math.pow(multiplier, attempt);
        long wait = Math.min((long) base, maxWaitMs);
        long jitter = (long) (wait * Math.random() * 0.1);
        return wait + Math.min(jitter, maxWaitMs - wait);
    }

    private static void sleep(long waitMs) {
        if (waitMs <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("retry interrupted while waiting", e);
        }
    }
}
