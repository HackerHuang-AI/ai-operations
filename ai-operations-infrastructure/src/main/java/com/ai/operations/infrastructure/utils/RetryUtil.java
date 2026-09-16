package com.ai.operations.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public final class RetryUtil {

    private RetryUtil() {
    }

    public static <T> T retry(Callable<T> callable, int maxRetries, long intervalMs) throws Exception {
        return retry(callable, maxRetries, intervalMs, 2.0D, 30_000);
    }

    public static <T> T retry(Callable<T> callable, int maxRetries, long intervalMs,
                              double backoffMultiplier, long maxWaitMs) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (attempt == maxRetries) {
                    break;
                }
                long waitMs = Math.min((long) (intervalMs * Math.pow(backoffMultiplier, attempt)), maxWaitMs);
                waitMs += ThreadLocalRandom.current().nextLong(Math.max(1, waitMs / 10));
                log.warn("[RetryUtil] 第 {}/{} 次执行失败，{}ms 后重试", attempt + 1, maxRetries + 1, waitMs, e);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
            }
        }
        throw lastException;
    }
}

