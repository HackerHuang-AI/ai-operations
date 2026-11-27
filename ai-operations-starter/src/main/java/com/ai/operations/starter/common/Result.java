package com.ai.operations.starter.common;

import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.enums.ResultCodeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private ResultCodeEnum code;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;
    private T data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ErrorItem> errors;

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS, ResultCodeEnum.SUCCESS.getDefaultMessage(), null, data, null);
    }

    public static <T> Result<T> error(ErrorCodeEnum errorCode, String message, List<ErrorItem> errors) {
        return new Result<>(ResultCodeEnum.ERROR, message, errorCode.getCode(), null, List.copyOf(errors));
    }

    /** 成功，携带数据与自定义提示。 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCodeEnum.SUCCESS, message, null, data, null);
    }
    /** 成功，无数据。 */
    public static <T> Result<T> success() {
        return new Result<>(ResultCodeEnum.SUCCESS, ResultCodeEnum.SUCCESS.getDefaultMessage(), null, null, null);
    }
    /** 失败，仅携带错误码与提示。 */
    public static <T> Result<T> error(ErrorCodeEnum errorCode, String message) {
        return error(errorCode, message, List.of());
    }
}
