/**
 * Tests for Vue Router baseline wiring (Story 1.1 AC2).
 * Test Design: docs/qa/assessments/1.1-test-design-20260422.md
 * Scenario: 1.1-UNIT-007
 */

import { describe, test, expect } from 'vitest';
import router from '@/router';

describe('AC2: Router', () => {
  test('1.1-UNIT-007: default route "/" resolves to HomeView', () => {
    const resolved = router.resolve('/');
    expect(resolved.matched.length).toBeGreaterThan(0);
    expect(resolved.name).toBe('Home');
  });
});
