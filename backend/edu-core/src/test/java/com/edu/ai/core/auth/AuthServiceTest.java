package com.edu.ai.core.auth;

import com.edu.ai.common.constant.UserRole;
import com.edu.ai.common.constant.UserType;
import com.edu.ai.common.exception.BizException;
import com.edu.ai.core.auth.dto.RegisterRequest;
import com.edu.ai.core.auth.dto.UserDto;
import com.edu.ai.core.auth.entity.User;
import com.edu.ai.core.auth.repository.UserRepository;
import com.edu.ai.core.auth.service.AuthService;
import com.edu.ai.core.auth.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService#register} — Story 1.2 AC1 business
 * orchestration. No Spring context; dependencies are mocked.
 *
 * Test Design: docs/qa/assessments/1.2-test-design-20260423.md
 */
class AuthServiceTest {

    private static final String PHONE = "13800138000";
    private static final String CODE = "123456";

    private final SmsService smsService = mock(SmsService.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(smsService, userRepository);
    }

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone(PHONE);
        request.setSmsCode(CODE);
        request.setUsername("张三");
        request.setRole(UserRole.MEMBER_RURAL);
        return request;
    }

    @Test
    void test_1_2_UNIT_007_registerHappyPath() {
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User input = invocation.getArgument(0);
            input.setId(UUID.randomUUID());
            input.setCreatedAt(OffsetDateTime.now());
            input.setUpdatedAt(OffsetDateTime.now());
            if (input.getUserType() == null) {
                input.setUserType(UserType.USER);
            }
            return input;
        });

        UserDto dto = authService.register(validRequest());

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getPhone()).isEqualTo(PHONE);
        assertThat(dto.getUsername()).isEqualTo("张三");
        assertThat(dto.getRole()).isEqualTo(UserRole.MEMBER_RURAL);
        assertThat(dto.getUserType()).isEqualTo(UserType.USER);
        assertThat(dto.getCreatedAt()).isNotNull();
    }

    @Test
    void test_1_2_UNIT_008_registerDuplicatePhone() {
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(userRepository.existsByPhone(PHONE)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest()))
                .isInstanceOf(BizException.class)
                .hasMessage("该手机号已注册，请直接登录")
                .extracting("code").isEqualTo(409);

        verify(userRepository, never()).save(any());
    }

    @Test
    void test_1_2_UNIT_009_registerWrongCode() {
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(false);

        assertThatThrownBy(() -> authService.register(validRequest()))
                .isInstanceOf(BizException.class)
                .hasMessage("验证码错误或已过期")
                .extracting("code").isEqualTo(400);

        verify(userRepository, never()).existsByPhone(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void test_1_2_UNIT_010_registerTransactionalRollback() {
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        // Service does not swallow — advice converts to R.code 409 or 5000.
        assertThatThrownBy(() -> authService.register(validRequest()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
