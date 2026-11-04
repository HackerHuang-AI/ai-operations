package com.ai.operations.application.model.request;

import java.util.Optional;
import java.util.function.Supplier;

public final class RequestContextHolder {
    private static final ThreadLocal<RequestContext> HOLDER = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static Optional<RequestContext> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static void set(RequestContext context) {
        HOLDER.set(context);
    }

    public static <T> T withTargetTenantId(String targetTenantId, Supplier<T> action) {
        RequestContext previous = current().orElseThrow(() -> new IllegalStateException("request context is unavailable"));
        set(new RequestContext(previous.traceId(), previous.traceparent(), previous.userId(), targetTenantId,
                previous.sessionId(), previous.callChain()));
        try {
            return action.get();
        } finally {
            set(previous);
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}

