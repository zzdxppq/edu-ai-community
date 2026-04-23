package com.edu.ai.common.response;

import java.io.Serializable;

/**
 * Unified response envelope: {@code {code, message, data}}.
 *
 * <p>Contract: {@code code == 0} denotes success (BR-common: zero-is-success).
 * Any non-zero code denotes a business error with caller-facing {@code message}.
 * The {@code data} field is serialized as explicit null when absent (Jackson
 * default include policy) so clients can distinguish "success, no payload"
 * from a missing key.
 */
public class R<T> implements Serializable {

    public static final int CODE_OK = 0;
    public static final String MSG_OK = "OK";

    private int code;
    private String message;
    private T data;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> success(T data) {
        return new R<>(CODE_OK, MSG_OK, data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
