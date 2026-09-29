package com.ai.operations.application.common;

import com.ai.operations.application.enums.ErrorCodeEnum;

import java.util.List;

public class BizException extends RuntimeException {
    private final ErrorCodeEnum errorCode;
    private final List<ErrorCodeEnum> errorDetails;
    private final Object[] args;

    public BizException(ErrorCodeEnum errorCode, Object... args) {
        this(errorCode, List.of(), args);
    }

    public BizException(ErrorCodeEnum errorCode, ErrorCodeEnum errorDetail) {
        this(errorCode, List.of(errorDetail));
    }

    public BizException(ErrorCodeEnum errorCode, List<ErrorCodeEnum> errorDetails, Object... args) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.errorDetails = List.copyOf(errorDetails);
        this.args = args;
    }

    public ErrorCodeEnum getErrorCode() {
        return errorCode;
    }

    public List<ErrorCodeEnum> getErrorDetails() {
        return errorDetails;
    }

    public Object[] getArgs() {
        return args;
    }
}

