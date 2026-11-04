package com.ai.operations.application.model.request;

public record RequestContext(
        String traceId,
        String traceparent,
        String userId,
        String targetTenantId,
        String sessionId,
        String callChain
) {
}

