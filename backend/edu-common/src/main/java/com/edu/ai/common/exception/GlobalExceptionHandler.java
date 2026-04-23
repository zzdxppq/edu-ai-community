package com.edu.ai.common.exception;

import com.edu.ai.common.response.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler that funnels all thrown exceptions into the
 * unified {@link R} envelope.
 *
 * <p>Contract per Story 1.1 AC1:
 * <ul>
 *   <li>{@link BizException}            → HTTP 200, R(code={bizCode}, message={bizMessage})</li>
 *   <li>{@link MethodArgumentNotValidException} → HTTP 200, R(code=4000, message={field errors})</li>
 *   <li>All other {@link Exception}      → HTTP 500, R(code=5000, message="系统繁忙，请稍后再试")
 *       — never leaks stack traces or internal messages to the caller.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final int CODE_VALIDATION = 4000;
    public static final int CODE_UNKNOWN = 5000;
    public static final String MSG_UNKNOWN = "系统繁忙，请稍后再试";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException ex) {
        log.warn("BizException code={} message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.ok(R.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "参数校验失败";
        }
        log.warn("Validation failed: {}", message);
        return ResponseEntity.ok(R.fail(CODE_VALIDATION, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknown(Exception ex) {
        // Log full stack internally; never forward exception details to the client.
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(CODE_UNKNOWN, MSG_UNKNOWN));
    }

    private static String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + (fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage());
    }
}
