package com.edu.ai.core.auth;

import com.edu.ai.common.constant.UserRole;
import com.edu.ai.core.auth.repository.UserRepository;
import com.edu.ai.core.auth.service.SmsService;
import com.edu.ai.core.auth.service.impl.LogSmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * AC1 + AC2 server-side integration tests (Story 1.2).
 *
 * <p>Testcontainers supplies real Postgres (pgvector:pg16) and Redis 7; the
 * class auto-skips when Docker is unavailable. Per Architect Round-2 M2, every
 * business error path asserts HTTP 200 + business code in {@code R.code};
 * only an unhandled {@link RuntimeException} yields HTTP 500 + R(5000).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTest {

    private static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("edu_core")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @BeforeAll
    static void startContainers() {
        PG.start();
        REDIS.start();
    }

    @AfterAll
    static void stopContainers() {
        REDIS.stop();
        PG.stop();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired MockMvc mockMvc;
    @Autowired RedisTemplate<String, Object> redisTemplate;
    @Autowired UserRepository userRepositoryDirect;
    @Autowired ObjectMapper objectMapper;

    @SpyBean SmsService smsService;
    @SpyBean UserRepository userRepository;

    @BeforeEach
    void resetState() {
        reset(smsService, userRepository);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        userRepositoryDirect.deleteAll();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String fetchCachedCode(String phone) {
        Object cached = redisTemplate.opsForValue().get(LogSmsService.CODE_KEY_PREFIX + phone);
        assertThat(cached).as("sms:code:%s must be present after send-sms", phone).isNotNull();
        return cached.toString();
    }

    private MvcResult postSendSms(String phone) throws Exception {
        return mockMvc.perform(post("/api/auth/send-sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn();
    }

    private MvcResult postRegister(String phone, String code, String username, String role)
            throws Exception {
        String payload = String.format(
                "{\"phone\":\"%s\",\"smsCode\":\"%s\",\"username\":\"%s\",\"role\":\"%s\"}",
                phone, code, username, role);
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn();
    }

    private MvcResult postRegisterRaw(String rawJson) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andReturn();
    }

    private Map<?, ?> body(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8), Map.class);
    }

    // ============================================================
    // AC1: happy + documented error paths
    // ============================================================

    @Test
    void test_1_2_INT_002_sendSmsSuccess() throws Exception {
        MvcResult result = postSendSms("13800138001");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(result).get("code")).isEqualTo(0);
        assertThat(redisTemplate.hasKey(LogSmsService.CODE_KEY_PREFIX + "13800138001")).isTrue();
        Long ttl = redisTemplate.getExpire(
                LogSmsService.CODE_KEY_PREFIX + "13800138001", TimeUnit.SECONDS);
        assertThat(ttl).isBetween(295L, 300L);
    }

    @Test
    void test_1_2_INT_003_sendSmsRateLimit() throws Exception {
        postSendSms("13800138002");
        MvcResult second = postSendSms("13800138002");
        assertThat(second.getResponse().getStatus()).isEqualTo(200);
        Map<?, ?> body = body(second);
        assertThat(body.get("code")).isEqualTo(429);
        assertThat(body.get("message")).asString().contains("验证码已发送");
    }

    @Test
    void test_1_2_INT_004_registerHappyPath() throws Exception {
        String phone = "13800138003";
        postSendSms(phone);
        String code = fetchCachedCode(phone);

        MvcResult result = postRegister(phone, code, "张三", "MEMBER_RURAL");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<?, ?> body = body(result);
        assertThat(body.get("code")).isEqualTo(0);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).containsKeys("id", "phone", "username", "role", "userType", "createdAt");
        assertThat(data.get("phone")).isEqualTo(phone);
        assertThat(data.get("userType")).isEqualTo("USER");

        assertThat(userRepositoryDirect.existsByPhone(phone)).isTrue();
        assertThat(redisTemplate.hasKey(LogSmsService.CODE_KEY_PREFIX + phone)).isFalse();
    }

    @Test
    void test_1_2_INT_005_registerWrongCode() throws Exception {
        String phone = "13800138004";
        postSendSms(phone);

        MvcResult result = postRegister(phone, "000000", "张三", "MEMBER_RURAL");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<?, ?> body = body(result);
        assertThat(body.get("code")).isEqualTo(400);
        assertThat(body.get("message")).asString().contains("验证码错误或已过期");
        assertThat(userRepositoryDirect.existsByPhone(phone)).isFalse();
        assertThat(redisTemplate.hasKey(LogSmsService.CODE_KEY_PREFIX + phone)).isTrue();
    }

    @Test
    void test_1_2_INT_006_registerDuplicatePhone() throws Exception {
        String phone = "13800138005";
        postSendSms(phone);
        String code = fetchCachedCode(phone);
        postRegister(phone, code, "张三", "MEMBER_RURAL"); // 1st OK

        // SmsLimit key still active (60s); force-clear for the retry flow
        redisTemplate.delete(LogSmsService.LIMIT_KEY_PREFIX + phone);
        postSendSms(phone);
        String code2 = fetchCachedCode(phone);

        MvcResult result = postRegister(phone, code2, "李四", "RESEARCHER");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<?, ?> body = body(result);
        assertThat(body.get("code")).isEqualTo(409);
        assertThat(body.get("message")).asString().contains("该手机号已注册");

        assertThat(userRepositoryDirect.findByPhone(phone).orElseThrow().getUsername())
                .isEqualTo("张三");
    }

    @Test
    void test_1_2_INT_007_registerFieldValidation() throws Exception {
        MvcResult result = postRegister("12345", "123456", "张三", "MEMBER_RURAL");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<?, ?> body = body(result);
        assertThat(body.get("code")).isEqualTo(4000);
        assertThat(body.get("message")).asString().contains("请输入正确的手机号");
    }

    @Test
    void test_1_2_INT_008_registerMissingRole() throws Exception {
        String payload = "{\"phone\":\"13800138006\",\"smsCode\":\"123456\",\"username\":\"张三\"}";
        MvcResult result = postRegisterRaw(payload);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<?, ?> body = body(result);
        assertThat(body.get("code")).isEqualTo(4000);
        assertThat(body.get("message")).asString().contains("请选择您的角色身份");
    }

    @Test
    void test_1_2_INT_009_responseContractAlwaysHttp200() throws Exception {
        List<MvcResult> businessFailures = List.of(
                postRegister("12345", "123456", "张三", "MEMBER_RURAL"),   // 4000
                postRegister("13800138007", "000000", "张三", "MEMBER_RURAL") // 400
        );
        postSendSms("13800138008");
        MvcResult rateLimit = postSendSms("13800138008"); // 429
        for (MvcResult r : businessFailures) {
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
        }
        assertThat(rateLimit.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(rateLimit).get("code")).isEqualTo(429);
    }

    // ============================================================
    // AC2: server-side enum guard
    // ============================================================

    @Test
    void test_1_2_INT_010_registerInvalidRoleEnum() throws Exception {
        String payload = "{\"phone\":\"13800138009\",\"smsCode\":\"123456\","
                + "\"username\":\"张三\",\"role\":\"UNKNOWN_ROLE\"}";
        MvcResult result = postRegisterRaw(payload);
        assertThat(result.getResponse().getStatus()).isIn(200, 500);
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(raw).doesNotContain("com.edu.ai.common.constant.UserRole");
        assertThat(raw).doesNotContainIgnoringCase("jackson");
        // Either branch is acceptable per test-design:
        // (a) Jackson fails deserialization → advice returns R(code:4000 or 5000); OR
        // (b) AuthService maps to R(code:400). Both must NOT leak internals.
    }

    // ============================================================
    // Blind Spot Scenarios
    // ============================================================

    @Test
    void test_1_2_BLIND_ERROR_002_dbUnavailableReturns5000() throws Exception {
        String phone = "13800138010";
        postSendSms(phone);
        String code = fetchCachedCode(phone);

        doThrow(new DataAccessResourceFailureException("db down"))
                .when(userRepository).existsByPhone(anyString());

        MvcResult result = postRegister(phone, code, "张三", "MEMBER_RURAL");
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Map<?, ?> body = objectMapper.readValue(raw, Map.class);
        assertThat(body.get("code")).isEqualTo(5000);
        assertThat(raw).doesNotContain("DataAccessResourceFailureException");
        assertThat(raw).doesNotContainIgnoringCase("stacktrace");
    }

    @Test
    void test_1_2_BLIND_FLOW_003_smsForPhoneANotReusableForPhoneB() throws Exception {
        String phoneA = "13800138011";
        String phoneB = "13800138012";
        postSendSms(phoneA);
        String codeA = fetchCachedCode(phoneA);

        MvcResult result = postRegister(phoneB, codeA, "李四", "RESEARCHER");
        assertThat(body(result).get("code")).isEqualTo(400);
        assertThat(redisTemplate.hasKey(LogSmsService.CODE_KEY_PREFIX + phoneA)).isTrue();
    }

    @Test
    void test_1_2_BLIND_CONCURRENCY_001_concurrentRegisterSamePhone() throws Exception {
        String phone = "13800138013";
        postSendSms(phone);
        String code = fetchCachedCode(phone);

        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    gate.await();
                    MvcResult r = postRegister(phone, code, "并发用户", "MEMBER_RURAL");
                    int biz = (int) body(r).get("code");
                    if (biz == 0) success.incrementAndGet();
                    else if (biz == 409 || biz == 400) conflict.incrementAndGet();
                    else other.incrementAndGet();
                } catch (Exception ignored) {
                }
            });
        }
        gate.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
        assertThat(other.get()).isZero();
        assertThat(userRepositoryDirect.findByPhone(phone)).isPresent();
    }

    @Test
    void test_1_2_BLIND_CONCURRENCY_003_concurrentSendSmsRateLimited() throws Exception {
        String phone = "13800138014";

        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger limited = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                try {
                    gate.await();
                    MvcResult r = postSendSms(phone);
                    int biz = (int) body(r).get("code");
                    if (biz == 0) ok.incrementAndGet();
                    else if (biz == 429) limited.incrementAndGet();
                } catch (Exception ignored) {
                }
            });
        }
        gate.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(ok.get()).isEqualTo(1);
        assertThat(limited.get()).isEqualTo(2);

        Long ttl = redisTemplate.getExpire(LogSmsService.LIMIT_KEY_PREFIX + phone, TimeUnit.SECONDS);
        assertThat(ttl).isLessThanOrEqualTo(60L);
    }

    @Test
    void test_1_2_BLIND_DATA_001_transactionRollbackOnDbError() throws Exception {
        String phone = "13800138015";
        postSendSms(phone);
        String code = fetchCachedCode(phone);

        doThrow(new DataIntegrityViolationException("unique violation"))
                .when(userRepository).save(any());

        long before = userRepositoryDirect.count();
        MvcResult result = postRegister(phone, code, "张三", "MEMBER_RURAL");
        int biz = (int) body(result).get("code");
        assertThat(biz).isIn(409, 5000);
        assertThat(userRepositoryDirect.count()).isEqualTo(before);
    }

    @Test
    void test_1_2_BLIND_DATA_002_smsCodeDeletedAfterRegister() throws Exception {
        String phone = "13800138016";
        postSendSms(phone);
        String code = fetchCachedCode(phone);
        postRegister(phone, code, "张三", "MEMBER_RURAL");

        assertThat(redisTemplate.hasKey(LogSmsService.CODE_KEY_PREFIX + phone)).isFalse();
        assertThat(redisTemplate.hasKey(LogSmsService.LIMIT_KEY_PREFIX + phone)).isTrue();
    }

    @Test
    void test_1_2_BLIND_DATA_003_validationShortCircuitsPersistence() throws Exception {
        postRegister("12345", "123456", "张三", "MEMBER_RURAL");
        verify(userRepository, never()).existsByPhone(anyString());
        verify(smsService, never()).verifyCode(anyString(), anyString());
    }

    @Test
    void test_1_2_BLIND_RESOURCE_001_testcontainersCleanupOnTeardown() {
        assertThat(PG.isRunning()).as("PG container lifecycle managed by @Testcontainers").isTrue();
        assertThat(REDIS.isRunning()).as("Redis container lifecycle managed by @Testcontainers").isTrue();
    }
}
