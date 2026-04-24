package com.edu.ai.core.auth;

import com.edu.ai.common.exception.GlobalExceptionHandler;
import com.edu.ai.core.auth.controller.AuthController;
import com.edu.ai.core.auth.service.AuthService;
import com.edu.ai.core.auth.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice edge case: unhandled RuntimeException → HTTP 500 + R(5000).
 * Uses standalone MockMvc with the shared {@link GlobalExceptionHandler} so no
 * full Spring context is required.
 */
class AuthControllerUnitTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SmsService smsService = mock(SmsService.class);
        authService = mock(AuthService.class);
        AuthController controller = new AuthController(smsService, authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void test_1_2_BLIND_ERROR_003_unknownExceptionBubblesTo5000() throws Exception {
        when(authService.register(any())).thenThrow(new RuntimeException("boom"));

        String payload = """
                {"phone":"13800138000","smsCode":"123456","username":"张三","role":"MEMBER_RURAL"}
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> parsed = mapper.readValue(body, Map.class);
        assertThat(parsed.get("code")).isEqualTo(5000);
        assertThat(parsed.get("message")).isEqualTo("系统繁忙，请稍后再试");
        assertThat(body).doesNotContain("boom");
        assertThat(body).doesNotContain("RuntimeException");
        assertThat(body).doesNotContainIgnoringCase("stack");
    }
}
