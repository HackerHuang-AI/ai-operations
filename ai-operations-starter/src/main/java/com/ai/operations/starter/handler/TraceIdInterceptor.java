package com.ai.operations.starter.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class TraceIdInterceptor implements HandlerInterceptor {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String APP_KEY_HEADER = "X-Source-AppKey";

    @Value("${spring.application.name:ai-operations}")
    private String applicationName;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        String sourceAppKey = request.getHeader(APP_KEY_HEADER);
        MDC.put("sourceAppKey", sourceAppKey != null && !sourceAppKey.isBlank() ? sourceAppKey : applicationName);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        MDC.clear();
    }
}

