package com.ai.operations.starter.handler;

import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.starter.common.ErrorItem;
import com.ai.operations.starter.common.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex, Locale locale) {
        HttpStatus status = ex.getErrorCode() == ErrorCodeEnum.SYSTEM_ERROR
                ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(error(ex.getErrorCode(), ex.getErrorDetails(), locale, ex.getArgs()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex, Locale locale) {
        List<ErrorCodeEnum> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> toErrorCode(error.getDefaultMessage(), error.getCode()))
                .toList();
        log.warn("[参数校验失败] {}", details.stream().map(ErrorCodeEnum::getCode).toList());
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, details, locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex, Locale locale) {
        List<ErrorCodeEnum> details = ex.getConstraintViolations().stream()
                .map(violation -> toErrorCode(violation.getMessageTemplate(), violation.getConstraintDescriptor()
                        .getAnnotation().annotationType().getSimpleName()))
                .toList();
        log.warn("[参数校验失败] {}", details.stream().map(ErrorCodeEnum::getCode).toList());
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, details, locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex, Locale locale) {
        List<ErrorCodeEnum> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> toErrorCode(error.getDefaultMessage(), error.getCode()))
                .toList();
        log.warn("[参数校验失败] {}", details.stream().map(ErrorCodeEnum::getCode).toList());
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, details, locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public Result<Void> handleMissingRequestHeaderException(MissingRequestHeaderException ex, Locale locale) {
        log.warn("[缺少请求头] {}", ex.getHeaderName());
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, List.of(ErrorCodeEnum.PARAMETER_REQUIRED), locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public Result<Void> handleRequestValidationException(Exception ex, Locale locale) {
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, List.of(ErrorCodeEnum.PARAMETER_VALUE_INVALID), locale);
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, Locale locale) {
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, List.of(ErrorCodeEnum.PARAMETER_VALUE_INVALID), locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, Locale locale) {
        log.warn("[请求体不可读]", ex);
        return error(ErrorCodeEnum.REQUEST_BODY_INVALID, List.of(), locale);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex, Locale locale) {
        log.error("[系统异常]", ex);
        return error(ErrorCodeEnum.SYSTEM_ERROR, List.of(), locale);
    }

    private Result<Void> error(ErrorCodeEnum errorCode, List<ErrorCodeEnum> errorDetails, Locale locale, Object... args) {
        List<ErrorItem> errors = errorDetails.stream()
                .map(errorDetail -> new ErrorItem(errorDetail.getCode(), message(errorDetail, locale)))
                .toList();
        return Result.error(errorCode, message(errorCode, locale, args), errors);
    }

    private String message(ErrorCodeEnum errorCode, Locale locale, Object... args) {
        String message = messageSource.getMessage(errorCode.getMessageKey(), args, errorCode.getDefaultMessage(), locale);
        return message == null || (args != null && args.length > 0) ? message : message.replaceFirst("[:：] ?\\{0}$", "");
    }

    private ErrorCodeEnum toErrorCode(String code, String constraint) {
        try {
            return ErrorCodeEnum.fromCode(code);
        } catch (IllegalArgumentException ignored) {
            return switch (constraint) {
                case "NotBlank", "NotEmpty", "NotNull" -> ErrorCodeEnum.PARAMETER_REQUIRED;
                case "Pattern", "Email", "URL" -> ErrorCodeEnum.PARAMETER_FORMAT_INVALID;
                case "DecimalMax", "DecimalMin", "Max", "Min", "Negative", "NegativeOrZero", "Positive", "PositiveOrZero", "Size" ->
                        ErrorCodeEnum.PARAMETER_OUT_OF_RANGE;
                default -> ErrorCodeEnum.PARAMETER_VALUE_INVALID;
            };
        }
    }
}

