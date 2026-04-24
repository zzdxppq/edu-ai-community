package com.edu.ai.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 1.2-UNIT-001 — 6 enum values expose the canonical Chinese labels (BR-1.2).
 *
 * Test Design: docs/qa/assessments/1.2-test-design-20260423.md
 */
class UserRoleTest {

    @Test
    void test_1_2_UNIT_001_displayNamesMatchSpec() {
        assertEquals("区域教育管理人员", UserRole.REGION_ADMIN.getDisplayName());
        assertEquals("校共体核心牵头校人员", UserRole.CONSORTIUM_LEAD.getDisplayName());
        assertEquals("校共体成员校人员（城镇）", UserRole.MEMBER_URBAN.getDisplayName());
        assertEquals("校共体成员校人员（乡村）", UserRole.MEMBER_RURAL.getDisplayName());
        assertEquals("研究人员", UserRole.RESEARCHER.getDisplayName());
        assertEquals("其他人员", UserRole.OTHER.getDisplayName());
        assertEquals(6, UserRole.values().length);
    }
}
