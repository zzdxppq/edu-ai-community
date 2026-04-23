/**
 * Tests for the Axios instance + response interceptor (Story 1.1 AC2).
 * Test Design: docs/qa/assessments/1.1-test-design-20260422.md
 * Scenarios: 1.1-UNIT-008, 1.1-BLIND-ERROR-004, 1.1-BLIND-FLOW-001
 */

import { describe, test, expect, vi, beforeEach } from 'vitest';
import type { AxiosError } from 'axios';

vi.mock('element-plus', () => ({ ElMessage: vi.fn() }));

async function loadModule() {
  return import('@/api');
}

describe('AC2: Axios instance', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.resetModules();
  });

  test('1.1-UNIT-008: instance configured with baseURL and 15s timeout', async () => {
    const mod = await loadModule();
    expect(mod.http.defaults.baseURL).toBe(import.meta.env.VITE_API_BASE_URL);
    expect(mod.http.defaults.timeout).toBe(15000);
  });

  test('[BLIND-SPOT] 1.1-BLIND-ERROR-004: network error triggers friendly toast', async () => {
    const { ElMessage } = await import('element-plus');
    const mod = await loadModule();

    const error = Object.assign(new Error('Network Error') as AxiosError, {
      isAxiosError: true,
      toJSON: () => ({}),
      config: undefined,
      name: 'Error',
      response: undefined,
    });

    await expect(mod.handleResponseError(error)).rejects.toBe(error);

    const messageMock = ElMessage as unknown as ReturnType<typeof vi.fn>;
    expect(messageMock).toHaveBeenCalledTimes(1);
    const call = messageMock.mock.calls[0][0];
    expect(call).toMatchObject({ type: 'error', message: mod.NETWORK_ERROR_MESSAGE });
    expect(call.message).toBe('服务连接中，请稍后重试');
    // Contract: no technical detail leakage to the user-facing toast.
    expect(call.message).not.toMatch(/Network|5\d\d|timeout|ECONN/i);
  });

  test('[BLIND-SPOT] 1.1-BLIND-FLOW-001: timeout triggers same degraded path', async () => {
    const { ElMessage } = await import('element-plus');
    const mod = await loadModule();

    const error = Object.assign(new Error('timeout') as AxiosError, {
      isAxiosError: true,
      toJSON: () => ({}),
      config: undefined,
      name: 'AxiosError',
      code: 'ECONNABORTED',
      response: undefined,
    });

    await expect(mod.handleResponseError(error)).rejects.toBe(error);

    const messageMock = ElMessage as unknown as ReturnType<typeof vi.fn>;
    expect(messageMock).toHaveBeenCalledTimes(1);
    expect(messageMock.mock.calls[0][0]).toMatchObject({
      message: mod.NETWORK_ERROR_MESSAGE,
    });
  });
});
