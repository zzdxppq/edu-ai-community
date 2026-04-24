package com.edu.ai.core.auth.service;

/**
 * SMS verification code abstraction.
 *
 * <p>The {@code void} return on {@link #sendVerificationCode(String)} is
 * intentional (Architect Round-2 L3): the generated code must never leave the
 * service boundary. Production callers treat "no exception" as success.
 */
public interface SmsService {

    /**
     * Rate-limits (atomic SET NX EX 60 on {@code sms:limit:{phone}}), generates
     * a 6-digit code, and stores it under {@code sms:code:{phone}} with a
     * 5-minute TTL.
     *
     * @throws com.edu.ai.common.exception.BizException with code 429 if the
     *         rate limit is already held by this phone.
     */
    void sendVerificationCode(String phone);

    /**
     * Returns {@code true} iff the cached code for {@code phone} matches
     * {@code code}. On match, deletes the cache entry (one-time consumption —
     * guards against SMS replay).
     */
    boolean verifyCode(String phone, String code);

    /** Force-clears the 60s rate-limit key. Currently unused outside tests. */
    void clearLimit(String phone);
}
