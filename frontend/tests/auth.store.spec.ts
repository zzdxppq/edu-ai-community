/**
 * Tests for Pinia auth store skeleton (Story 1.1 AC2).
 * Test Design: docs/qa/assessments/1.1-test-design-20260422.md
 * Scenario: 1.1-UNIT-009
 */

import { describe, test, expect, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from '@/stores/auth';

describe('AC2: Pinia auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  test('1.1-UNIT-009: initial state { user:null, accessToken:"" }', () => {
    const store = useAuthStore();
    expect(store.user).toBeNull();
    expect(store.accessToken).toBe('');
  });
});
