package com.edu.ai.core.auth;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.edu.ai.common.exception.BizException;
import com.edu.ai.core.auth.service.impl.LogSmsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LogSmsService} — Story 1.2 AC1 SmsService contract.
 *
 * Test Design: docs/qa/assessments/1.2-test-design-20260423.md
 *
 * Strategy: Mockito for {@code RedisTemplate} + Logback {@link ListAppender}
 * for log-sanitization assertions.
 */
class SmsServiceTest {

    private static final String PHONE = "13800138000";
    private static final String CODE_KEY = LogSmsService.CODE_KEY_PREFIX + PHONE;
    private static final String LIMIT_KEY = LogSmsService.LIMIT_KEY_PREFIX + PHONE;

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Object> valueOps = mock(ValueOperations.class);

    private LogSmsService service;
    private ListAppender<ILoggingEvent> logCapture;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new LogSmsService(redisTemplate);

        logCapture = new ListAppender<>();
        logCapture.start();
        ((Logger) LoggerFactory.getLogger(LogSmsService.class)).addAppender(logCapture);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(LogSmsService.class)).detachAppender(logCapture);
    }

    // ===========================================================
    // AC1: Core SmsService contract
    // ===========================================================

    @Test
    void test_1_2_UNIT_002_sendCodeWritesRedis() {
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(Boolean.TRUE);

        service.sendVerificationCode(PHONE);

        ArgumentCaptor<Object> codeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOps).setIfAbsent(eq(LIMIT_KEY), eq("1"), eq(LogSmsService.LIMIT_TTL));
        verify(valueOps).set(eq(CODE_KEY), codeCaptor.capture(), eq(LogSmsService.CODE_TTL));
        assertThat(codeCaptor.getValue().toString()).matches("\\d{6}");
    }

    @Test
    void test_1_2_UNIT_003_rateLimitRejects() {
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> service.sendVerificationCode(PHONE))
                .isInstanceOf(BizException.class)
                .hasMessage("验证码已发送，请稍后再试")
                .extracting("code").isEqualTo(429);

        verify(valueOps, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void test_1_2_UNIT_004_verifyCodeMatchesAndDeletes() {
        when(valueOps.get(CODE_KEY)).thenReturn("123456");

        boolean result = service.verifyCode(PHONE, "123456");

        assertThat(result).isTrue();
        verify(redisTemplate).delete(CODE_KEY);
    }

    @Test
    void test_1_2_UNIT_005_verifyCodeMismatchDoesNotDelete() {
        when(valueOps.get(CODE_KEY)).thenReturn("111111");

        boolean result = service.verifyCode(PHONE, "222222");

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void test_1_2_UNIT_006_verifyCodeExpiredOrAbsent() {
        when(valueOps.get(CODE_KEY)).thenReturn(null);

        boolean result = service.verifyCode(PHONE, "123456");

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    // ===========================================================
    // Blind Spot Scenarios
    // ===========================================================

    @Test
    void test_1_2_BLIND_ERROR_001_redisDown() {
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertThatThrownBy(() -> service.sendVerificationCode(PHONE))
                .isInstanceOf(RedisConnectionFailureException.class);
    }

    @Test
    void test_1_2_BLIND_CONCURRENCY_002_rateLimitIsAtomicSetNxEx() {
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(Boolean.TRUE);

        service.sendVerificationCode(PHONE);

        verify(valueOps).setIfAbsent(anyString(), any(), any(Duration.class));
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void test_1_2_BLIND_RESOURCE_003_partialLogSanitization() {
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(Boolean.TRUE);

        service.sendVerificationCode(PHONE);

        assertThat(logCapture.list).isNotEmpty();
        assertThat(logCapture.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(msg -> msg.matches(".*\\d{11}.*"));
    }
}
