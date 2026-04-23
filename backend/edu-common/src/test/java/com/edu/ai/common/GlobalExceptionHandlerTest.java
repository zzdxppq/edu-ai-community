package com.edu.ai.common;

import com.edu.ai.common.exception.BizException;
import com.edu.ai.common.exception.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link GlobalExceptionHandler}.
 * <p>
 * Test Design: docs/qa/assessments/1.1-test-design-20260422.md
 * Scenarios:
 *   - 1.1-UNIT-002 (P1): BizException → HTTP 200, R{code, message, data:null}
 *   - 1.1-UNIT-003 (P2): @NotBlank validation failure → R{code:4000, ...}
 *   - 1.1-UNIT-004 (P2): unknown RuntimeException → HTTP 500, R{code:5000, message:"系统繁忙..."},
 *                         must NOT leak stack/internal messages to the client.
 *
 * Uses standalone MockMvc (no Spring context) so edu-common can unit-test
 * the advice in isolation without inheriting a web auto-configuration.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void test_1_1_UNIT_002_handlesBizException() throws Exception {
        mvc.perform(post("/t/biz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(4001)))
                .andExpect(jsonPath("$.message", is("x")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void test_1_1_UNIT_003_handlesValidation() throws Exception {
        String body = "{\"name\":\"\"}";
        mvc.perform(post("/t/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(4000)))
                .andExpect(jsonPath("$.message", containsString("name")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void test_1_1_UNIT_004_handlesUnknown() throws Exception {
        String secret = "db-password=supersecret";
        mvc.perform(post("/t/unknown").param("secret", secret))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", is(5000)))
                .andExpect(jsonPath("$.message", is(GlobalExceptionHandler.MSG_UNKNOWN)))
                .andExpect(jsonPath("$.message", not(containsString(secret))))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @RestController
    static class TestController {

        @PostMapping("/t/biz")
        Object bizRoute() {
            throw new BizException(4001, "x");
        }

        @PostMapping("/t/validate")
        Object validateRoute(@Valid @RequestBody Payload body) {
            return body;
        }

        @PostMapping("/t/unknown")
        Object unknownRoute(@RequestParam String secret) {
            throw new RuntimeException(secret);
        }

        static class Payload {
            @NotBlank
            public String name;
        }
    }
}
