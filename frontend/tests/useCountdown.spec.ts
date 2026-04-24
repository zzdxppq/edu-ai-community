/**
 * Story 1.2 — useCountdown composable tests.
 * Test Design: docs/qa/assessments/1.2-test-design-20260423.md
 */

import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { defineComponent, h } from 'vue';
import { mount } from '@vue/test-utils';
import { useCountdown } from '@/composables/useCountdown';

function mountHost(initial = 60) {
  const Host = defineComponent({
    setup() {
      const countdown = useCountdown(initial);
      return { countdown };
    },
    render() {
      return h('span', String(this.countdown.remaining.value));
    },
  });
  return mount(Host);
}

describe('AC1: useCountdown composable', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  test('1.2-UNIT-012: start() decrements remaining each second and auto-resets at 0', () => {
    const wrapper = mountHost(60);
    const countdown = (wrapper.vm as any).countdown;
    expect(countdown.remaining.value).toBe(60);
    expect(countdown.isRunning.value).toBe(false);

    countdown.start();
    expect(countdown.isRunning.value).toBe(true);

    vi.advanceTimersByTime(1000);
    expect(countdown.remaining.value).toBe(59);

    vi.advanceTimersByTime(59_000);
    expect(countdown.remaining.value).toBe(60); // auto-reset to initial
    expect(countdown.isRunning.value).toBe(false);
  });

  test('[BLIND-SPOT] 1.2-BLIND-FLOW-002: remount resets countdown (no cross-mount memory)', () => {
    const first = mountHost(60);
    (first.vm as any).countdown.start();
    vi.advanceTimersByTime(5_000);
    expect((first.vm as any).countdown.remaining.value).toBe(55);
    first.unmount();

    const second = mountHost(60);
    expect((second.vm as any).countdown.remaining.value).toBe(60);
    expect((second.vm as any).countdown.isRunning.value).toBe(false);
  });

  test('[BLIND-SPOT] 1.2-BLIND-RESOURCE-002: setInterval cleared on unmount', () => {
    const wrapper = mountHost(60);
    (wrapper.vm as any).countdown.start();
    expect(vi.getTimerCount()).toBe(1);

    wrapper.unmount();
    expect(vi.getTimerCount()).toBe(0);
  });
});
