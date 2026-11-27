package com.ai.operations.application.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 平台响应与错误载荷分离的 HTTP 调用结果，状态码不代表业务成功。
 * 载荷由调用方按协议解析，合法空成功可为 null；解析失败或网络异常抛异常，原文不返回。
 *
 * @param <S> 平台响应实体
 * @param <E> 平台错误实体
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TypedHttpCallResult<S, E> {

    private int httpCode;
    private S httpData;
    private E errorData;
}

