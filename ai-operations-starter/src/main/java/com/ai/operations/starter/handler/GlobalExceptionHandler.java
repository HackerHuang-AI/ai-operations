package com.ai.operations.starter.handler;

import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.starter.common.ErrorItem;
import com.ai.operations.starter.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 全局异常处理器（全平台统一模版）。
 *
 * <p>单一职责：把异常翻译成「HTTP 状态码 + Result 信封」，并保证一个异常恰好打一次日志：
 * <ul>
 *   <li>业务异常 → warn，不打堆栈（堆栈对定位业务问题无价值，还会冲淡日志）；</li>
 *   <li>参数类异常 → warn，只打参数名/方法名等定位信息，不打堆栈；</li>
 *   <li>系统异常 → error，打全栈。</li>
 * </ul>
 * traceId 由 logback layout 从 MDC 渲染，与在哪个类打日志无关，
 * 因此 Controller 层不再需要 try/catch 兜底日志。
 *
 * @author HUANGcong
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // ==================== 业务异常 ====================

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex, Locale locale) {
        HttpStatus status = ex.getErrorCode() == ErrorCodeEnum.SYSTEM_ERROR
                ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        log.warn("[业务异常] code={}, details={}, args={}, msg={}", ex.getErrorCode().getCode(),
                ex.getErrorDetails(), Arrays.toString(ex.getArgs()), ex.getMessage());
        return ResponseEntity.status(status)
                .body(error(ex.getErrorCode(), ex.getErrorDetails(), locale, ex.getArgs()));
    }

    // ==================== 参数校验 ====================

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex, Locale locale) {
        List<ErrorCodeEnum> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorCode)
                .toList();
        log.warn("[参数校验失败] {}", details.stream().map(ErrorCodeEnum::getCode).toList());
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, details, locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex, Locale locale) {
        List<ErrorCodeEnum> details = ex.getConstraintViolations().stream()
                .map(this::toErrorCode)
                .toList();
        log.warn("[参数校验失败] {}", details.stream().map(ErrorCodeEnum::getCode).toList());
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, details, locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex, Locale locale) {
        List<ErrorCodeEnum> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorCode)
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
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameterException(MissingServletRequestParameterException ex, Locale locale) {
        log.warn("[缺少请求参数] name={}, type={}", ex.getParameterName(), ex.getParameterType());
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, List.of(ErrorCodeEnum.PARAMETER_REQUIRED), locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, Locale locale) {
        log.warn("[参数类型不匹配] name={}, value={}, requiredType={}", ex.getName(), ex.getValue(),
                ex.getRequiredType());
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, List.of(ErrorCodeEnum.PARAMETER_VALUE_INVALID), locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex, Locale locale) {
        log.warn("[非法参数] {}", ex.getMessage());
        return Result.error(ErrorCodeEnum.PARAM_ILLEGAL, message(ErrorCodeEnum.PARAM_ILLEGAL, locale), List.of());
    }

    // ==================== 请求协议 ====================

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, Locale locale) {
        log.warn("[请求方法不支持] method={}, supported={}", ex.getMethod(),
                ex.getSupportedHttpMethods() == null ? List.of() : List.of(ex.getSupportedHttpMethods()));
        return error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, List.of(ErrorCodeEnum.PARAMETER_VALUE_INVALID), locale);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, Locale locale) {
        log.warn("[请求体不可读] {}", ex.getMostSpecificCause().getMessage());
        return error(ErrorCodeEnum.REQUEST_BODY_INVALID, List.of(), locale);
    }

    // ==================== 兜底 ====================

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex, Locale locale) {
        log.error("[系统异常]", ex);
        return error(ErrorCodeEnum.SYSTEM_ERROR, List.of(), locale);
    }

    // ==================== 内部工具 ====================

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

    private ErrorCodeEnum toErrorCode(FieldError error) {
        ErrorCodeEnum errorCode = findErrorCode(error.getDefaultMessage());
        return errorCode != null ? errorCode : validationErrorCode(error.getCode());
    }

    private ErrorCodeEnum toErrorCode(ConstraintViolation<?> violation) {
        ErrorCodeEnum errorCode = findErrorCode(violation.getMessageTemplate());
        return errorCode != null ? errorCode
                : validationErrorCode(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());
    }

    /**
     * 校验注解未携带错误码时，按约束注解类型收敛到通用错误码。
     */
    private ErrorCodeEnum validationErrorCode(String constraint) {
        return switch (constraint) {
            case "NotBlank", "NotEmpty", "NotNull" -> ErrorCodeEnum.PARAMETER_REQUIRED;
            case "Pattern", "Email", "URL" -> ErrorCodeEnum.PARAMETER_FORMAT_INVALID;
            case "DecimalMax", "DecimalMin", "Max", "Min", "Negative", "NegativeOrZero", "Positive", "PositiveOrZero", "Size" ->
                    ErrorCodeEnum.PARAMETER_OUT_OF_RANGE;
            default -> ErrorCodeEnum.PARAMETER_VALUE_INVALID;
        };
    }

    /**
     * 校验注解的 message 允许直接写本模块错误码（如 {@code @Min(message = "4001017")}），命中则优先采用。
     */
    private ErrorCodeEnum findErrorCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return ErrorCodeEnum.fromCode(code);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
