/**
 * Tests for HomeView (Story 1.1 AC2).
 * Test Design: docs/qa/assessments/1.1-test-design-20260422.md
 * Scenarios: 1.1-UNIT-005, 1.1-UNIT-006, 1.1-BLIND-BOUNDARY-002
 */

import { describe, test, expect, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';

vi.mock('@/api', () => {
  const get = vi.fn();
  return { default: { get }, http: { get } };
});
vi.mock('element-plus', () => ({ ElMessage: vi.fn() }));

async function mountHome() {
  const { default: HomeView } = await import('@/views/home/HomeView.vue');
  return mount(HomeView);
}

describe('AC2: HomeView', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  test('1.1-UNIT-005: renders project title', async () => {
    const api = await import('@/api');
    (api.default.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: { status: 'UP' } });

    const wrapper = await mountHome();
    await flushPromises();

    expect(wrapper.text()).toContain('edu-ai-community');
  });

  test('1.1-UNIT-006: renders health UP status', async () => {
    const api = await import('@/api');
    (api.default.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: { status: 'UP' } });

    const wrapper = await mountHome();
    await flushPromises();

    const statusEl = wrapper.get('[data-testid="health-status"]');
    expect(statusEl.text()).toContain('UP');
  });

  test('[BLIND-SPOT] 1.1-BLIND-BOUNDARY-002: renders degraded text when health is non-UP', async () => {
    const api = await import('@/api');
    (api.default.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: { status: 'DOWN' } });

    const wrapper = await mountHome();
    await flushPromises();

    const statusEl = wrapper.get('[data-testid="health-status"]');
    const text = statusEl.text();
    // Must not claim UP and must surface a degraded indicator.
    expect(text).not.toMatch(/：\s*UP/);
    expect(text.toLowerCase()).toMatch(/down|异常/);
  });
});
