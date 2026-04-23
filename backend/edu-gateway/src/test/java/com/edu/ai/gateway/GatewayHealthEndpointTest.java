package com.edu.ai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration test for {@code GET /api/health}.
 *
 * Verifies AC1 main_scenario: Actuator health endpoint is reachable
 * via the {@code /api} base-path configured in {@code application.yml}
 * and returns HTTP 200 with {@code "status":"UP"}.
 *
 * Test Design: docs/qa/assessments/1.1-test-design-20260422.md (1.1-INT-001, P1)
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        // Neutralize routes — this test only exercises the gateway's own actuator.
        "spring.cloud.gateway.routes[0].id=noop",
        "spring.cloud.gateway.routes[0].uri=http://localhost:0",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/__noop__"
})
class GatewayHealthEndpointTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void test_1_1_INT_001_getApiHealthReturnsUp() {
        webTestClient.get()
                .uri("/api/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
