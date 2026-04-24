package com.edu.ai.core.auth.service.impl;

import com.edu.ai.common.exception.BizException;
import com.edu.ai.core.auth.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Dev/test SMS implementation: writes a 6-digit code into Redis and logs a
 * masked hint instead of actually sending an SMS. Replaced by a real provider
 * in Story 1.5 (prod profile).
 */
@Service
@Profile({"dev", "test", "default"})
@RequiredArgsConstructor
public class LogSmsService implements SmsService {

    public static final String CODE_KEY_PREFIX = "sms:code:";
    public static final String LIMIT_KEY_PREFIX = "sms:limit:";
    public static final Duration CODE_TTL = Duration.ofMinutes(5);
    public static final Duration LIMIT_TTL = Duration.ofSeconds(60);

    private static final Logger log = LoggerFactory.getLogger(LogSmsService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void sendVerificationCode(String phone) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LIMIT_KEY_PREFIX + phone, "1", LIMIT_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            throw new BizException(429, "验证码已发送，请稍后再试");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone, code, CODE_TTL);
        log.info("[DEV] SMS to {}: {}", maskPhone(phone), code);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String key = CODE_KEY_PREFIX + phone;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null || !cached.toString().equals(code)) {
            return false;
        }
        redisTemplate.delete(key);
        return true;
    }

    @Override
    public void clearLimit(String phone) {
        redisTemplate.delete(LIMIT_KEY_PREFIX + phone);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
