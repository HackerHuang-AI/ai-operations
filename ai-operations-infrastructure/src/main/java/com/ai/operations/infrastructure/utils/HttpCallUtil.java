package com.ai.operations.infrastructure.utils;

import com.ai.operations.application.model.TypedHttpCallResult;
import com.ai.operations.infrastructure.config.param.RetryParam;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

/**
 * 统一 HTTP 调用入口：{@link RetryUtil} 重试 + {@link TypedHttpCallResult} 结果信封。
 *
 * <p>2xx 由成功解析器处理，非 2xx 由错误解析器处理；网络失败与协议解析失败抛 IOException。
 * HTTP 200 中的业务错误由调用方按平台协议适配，本工具不推断业务成功。
 */
@Slf4j
public final class HttpCallUtil {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final RetryParam DEFAULT_PARAM = new RetryParam();

    private HttpCallUtil() {
    }

    /** 响应体解析器；合法空响应也交给解析器，无法按协议解析时应抛异常。 */
    @FunctionalInterface
    public interface BodyParser<T> {
        T parse(String body) throws Exception;
    }

    // ==================== 便捷方法 ====================

    public static <S, E> TypedHttpCallResult<S, E> get(OkHttpClient client, String url,
            Map<String, String> headers, RetryParam param,
            BodyParser<S> successParser, BodyParser<E> errorParser) throws IOException {
        return execute(client, build(new Request.Builder().url(url).get(), headers), param, successParser, errorParser);
    }

    public static <S, E> TypedHttpCallResult<S, E> post(OkHttpClient client, String url, String jsonBody,
            Map<String, String> headers, RetryParam param,
            BodyParser<S> successParser, BodyParser<E> errorParser) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        return execute(client, build(new Request.Builder().url(url).post(body), headers), param, successParser, errorParser);
    }

    public static <S, E> TypedHttpCallResult<S, E> postForm(OkHttpClient client, String url, Map<String, String> formParams,
            Map<String, String> headers, RetryParam param,
            BodyParser<S> successParser, BodyParser<E> errorParser) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (formParams != null) {
            formParams.forEach(formBuilder::add);
        }
        return execute(client, build(new Request.Builder().url(url).post(formBuilder.build()), headers), param, successParser, errorParser);
    }

    public static <S, E> TypedHttpCallResult<S, E> put(OkHttpClient client, String url, String jsonBody,
            Map<String, String> headers, RetryParam param,
            BodyParser<S> successParser, BodyParser<E> errorParser) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        return execute(client, build(new Request.Builder().url(url).put(body), headers), param, successParser, errorParser);
    }

    public static <S, E> TypedHttpCallResult<S, E> delete(OkHttpClient client, String url,
            Map<String, String> headers, RetryParam param,
            BodyParser<S> successParser, BodyParser<E> errorParser) throws IOException {
        return execute(client, build(new Request.Builder().url(url).delete(), headers), param, successParser, errorParser);
    }

    // ==================== 底层执行 ====================

    /** 两个解析器均必填；param 为 null 时使用默认重试参数，原始响应体不进入返回值。 */
    public static <S, E> TypedHttpCallResult<S, E> execute(OkHttpClient client, Request request,
            RetryParam param, BodyParser<S> successParser, BodyParser<E> errorParser) throws IOException {
        if (successParser == null || errorParser == null) {
            throw new IOException("HTTP successParser 和 errorParser 均不能为空");
        }
        RetryParam effective = param != null ? param : DEFAULT_PARAM;
        long startTime = System.currentTimeMillis();
        Response response;
        try {
            response = RetryUtil.retryHttp(() -> client.newCall(request).execute(), effective);
        } catch (IOException e) {
            log.error("[HttpCallUtil] 请求失败: {}, 耗时: {}ms, 异常类型: {}",
                    request.method(), System.currentTimeMillis() - startTime, e.getClass().getSimpleName());
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP 调用被中断", e);
        } catch (Exception e) {
            throw new IOException("HTTP 调用失败", e);
        }
        if (response == null) {
            throw new IOException("HTTP 调用无响应");
        }
        try (Response r = response) {
            long costTime = System.currentTimeMillis() - startTime;
            String body = r.body() != null ? r.body().string() : "";
            if (!r.isSuccessful()) {
                log.warn("[HttpCallUtil] 上游返回非 2xx: {}, 状态码: {}, 耗时: {}ms",
                        request.method(), r.code(), costTime);
            }
            try {
                if (r.isSuccessful()) {
                    return new TypedHttpCallResult<>(r.code(), successParser.parse(body), null);
                }
                E error = errorParser.parse(body);
                if (error == null) {
                    throw new IOException("无法识别平台错误");
                }
                return new TypedHttpCallResult<>(r.code(), null, error);
            } catch (Exception e) {
                log.error("[HttpCallUtil] 响应解析失败: {}, 状态码: {}, 耗时: {}ms, 异常类型: {}",
                        request.method(), r.code(), costTime, e.getClass().getSimpleName());
                throw new IOException("HTTP 响应解析失败，状态码: " + r.code());
            }
        }
    }

    private static Request build(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        return builder.build();
    }
}
