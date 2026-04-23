package com.edu.ai.common;

import com.edu.ai.common.response.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R} unified response envelope.
 * <p>
 * Test Design: docs/qa/assessments/1.1-test-design-20260422.md
 * Scenarios:
 *   - 1.1-UNIT-001 (P1): success(data) → {code:0, message:"OK", data:payload}
 *   - 1.1-BLIND-BOUNDARY-001 (P2): success(null) → no NPE, data field serialized as literal null
 */
class RTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void test_1_1_UNIT_001_success() {
        String payload = "hello";

        R<String> r = R.success(payload);

        assertThat(r.getCode()).isEqualTo(0);
        assertThat(r.getMessage()).isEqualTo("OK");
        assertThat(r.getData()).isEqualTo(payload);
    }

    @Test
    void test_1_1_BLIND_BOUNDARY_001_successWithNullData() throws Exception {
        R<Object> r = R.success(null);

        assertThat(r.getCode()).isEqualTo(0);
        assertThat(r.getMessage()).isEqualTo("OK");
        assertThat(r.getData()).isNull();

        // Serialization contract: data must be emitted as explicit null, not absent.
        String json = MAPPER.writeValueAsString(r);
        ObjectNode tree = (ObjectNode) MAPPER.readTree(json);
        assertThat(tree.has("data")).isTrue();
        assertThat(tree.get("data").isNull()).isTrue();
    }
}
