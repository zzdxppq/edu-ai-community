package com.edu.ai.core.auth;

import com.edu.ai.common.exception.GlobalExceptionHandler;
import com.edu.ai.core.auth.controller.AuthController;
import com.edu.ai.core.auth.entity.User;
import com.edu.ai.core.auth.repository.UserRepository;
import com.edu.ai.core.auth.service.AuthService;
import com.edu.ai.core.auth.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Boundary coverage for the registration path (AC1 Data Validation).
 *
 * <p>Uses standalone MockMvc so validation flows through the shared
 * {@link GlobalExceptionHandler} without spinning a full Spring Boot context
 * (mirrors 1.1 {@code GlobalExceptionHandlerTest}).
 *
 * <p>Assertion policy: the advice formats field errors as
 * {@code "<field>: <message>"}; tests assert the Chinese copy via
 * {@code contains(...)} to remain resilient to that prefix.
 */
class RegisterBoundaryTest {

    private MockMvc mockMvc;
    private SmsService smsService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        smsService = mock(SmsService.class);
        userRepository = mock(UserRepository.class);
        AuthService authService = new AuthService(smsService, userRepository);
        AuthController controller = new AuthController(smsService, authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void stubHappyPath() {
        when(smsService.verifyCode(anyString(), anyString())).thenReturn(true);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User input = invocation.getArgument(0);
            if (input.getId() == null) input.setId(UUID.randomUUID());
            input.setCreatedAt(OffsetDateTime.now());
            input.setUpdatedAt(OffsetDateTime.now());
            return input;
        });
    }

    private String body(String phone, String smsCode, String username, String role) {
        return String.format(
                "{\"phone\":%s,\"smsCode\":%s,\"username\":%s,\"role\":%s}",
                quote(phone), quote(smsCode), quote(username), quote(role));
    }

    private static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s + "\"";
    }

    private String postRegister(String json) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_001_phoneEmptyOrNull() throws Exception {
        String emptyPhoneBody = postRegister(body("", "123456", "张三", "MEMBER_RURAL"));
        assertThat(emptyPhoneBody).contains("\"code\":4000").contains("请输入正确的手机号");

        String missingPhoneBody = postRegister(
                "{\"smsCode\":\"123456\",\"username\":\"张三\",\"role\":\"MEMBER_RURAL\"}");
        assertThat(missingPhoneBody).contains("\"code\":4000").contains("请输入正确的手机号");

        verify(userRepository, never()).existsByPhone(anyString());
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_002_phoneNotStartingWithOne() throws Exception {
        String resp = postRegister(body("23800138000", "123456", "张三", "MEMBER_RURAL"));
        assertThat(resp).contains("\"code\":4000").contains("请输入正确的手机号");
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_003_phoneLengthOutOfRange() throws Exception {
        assertThat(postRegister(body("1380013800", "123456", "张三", "MEMBER_RURAL")))
                .contains("\"code\":4000").contains("请输入正确的手机号");

        assertThat(postRegister(body("138001380000", "123456", "张三", "MEMBER_RURAL")))
                .contains("\"code\":4000").contains("请输入正确的手机号");
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_004_smsCodeInvalidShape() throws Exception {
        assertThat(postRegister(body("13800138000", "12345", "张三", "MEMBER_RURAL")))
                .contains("\"code\":4000").contains("验证码错误或已过期");

        assertThat(postRegister(body("13800138000", "1234567", "张三", "MEMBER_RURAL")))
                .contains("\"code\":4000").contains("验证码错误或已过期");

        assertThat(postRegister(body("13800138000", "12345a", "张三", "MEMBER_RURAL")))
                .contains("\"code\":4000").contains("验证码错误或已过期");
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_005_usernameOneCharacter() throws Exception {
        String resp = postRegister(body("13800138000", "123456", "张", "MEMBER_RURAL"));
        assertThat(resp).contains("\"code\":4000").contains("用户名需2-20个字符");
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_006_usernameExactlyMin() throws Exception {
        stubHappyPath();
        String resp = postRegister(body("13800138000", "123456", "张三", "MEMBER_RURAL"));
        assertThat(resp).contains("\"code\":0");
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_007_usernameExactlyMax() throws Exception {
        stubHappyPath();
        String twenty = "张".repeat(20);
        String resp = postRegister(body("13800138000", "123456", twenty, "MEMBER_RURAL"));
        assertThat(resp).contains("\"code\":0");
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_008_usernameJustBeyondMax() throws Exception {
        String twentyOne = "张".repeat(21);
        String resp = postRegister(body("13800138000", "123456", twentyOne, "MEMBER_RURAL"));
        assertThat(resp).contains("\"code\":4000").contains("用户名需2-20个字符");
    }

    @Test
    void test_1_2_BLIND_BOUNDARY_010_phoneSqlInjectionPattern() throws Exception {
        String injection = "1' OR '1'='1";
        String resp = postRegister(body(injection, "123456", "张三", "MEMBER_RURAL"));
        assertThat(resp).contains("\"code\":4000").contains("请输入正确的手机号");
        verify(userRepository, never()).existsByPhone(anyString());
    }
}
