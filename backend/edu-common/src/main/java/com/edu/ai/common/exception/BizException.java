package com.edu.ai.common.exception;

/**
 * Business-logic exception carrying an application error code and a
 * caller-safe message. Never used for transport/system errors — those
 * are handled by {@link GlobalExceptionHandler}'s fallback branch.
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
