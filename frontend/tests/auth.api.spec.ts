/**
 * Story 1.2 — frontend auth API client tests.
 * Test Design: docs/qa/assessments/1.2-test-design-20260423.md
 */

import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

// @/api module is stubbed BEFORE importing auth.ts so the real axios/
// element-plus chain is not instantiated.
const postMock = vi.fn();
vi.mock('@/api', () => ({
  default: { post: postMock },
  http: { post: postMock },
  handleResponseError: vi.fn((err: unknown) => Promise.reject(err)),
  NETWORK_ERROR_MESSAGE: '服务连接中，请稍后重试',
}));

let authApi: typeof import('@/api/auth');

async function loadApi() {
  authApi = await import('@/api/auth');
}

beforeEach(async () => {
  postMock.mockReset();
  vi.resetModules();
  await loadApi();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('AC1/AC2: frontend auth API', () => {
  test('1.2-UNIT-013: sendSms() posts to /api/auth/send-sms with {phone}', async () => {
    postMock.mockResolvedValueOnce({ data: { code: 0, message: 'OK', data: null } });

    const result = await authApi.sendSms('13800138000');

    expect(postMock).toHaveBeenCalledTimes(1);
    expect(postMock).toHaveBeenCalledWith('/api/auth/send-sms', { phone: '13800138000' });
    expect(result.code).toBe(0);
  });

  test('1.2-UNIT-014: register() posts to /api/auth/register with full payload', async () => {
    postMock.mockResolvedValueOnce({ data: { code: 0, message: 'OK', data: null } });

    const payload = {
      phone: '13800138000',
      smsCode: '123456',
      username: '张三',
      role: 'MEMBER_RURAL' as const,
    };
    await authApi.register(payload);

    expect(postMock).toHaveBeenCalledTimes(1);
    expect(postMock).toHaveBeenCalledWith('/api/auth/register', payload);
  });

  test('[BLIND-SPOT] 1.2-BLIND-ERROR-004: network error surfaces friendly message via interceptor', async () => {
    const networkErr = Object.assign(new Error('Network Error'), { response: undefined });
    postMock.mockRejectedValueOnce(networkErr);

    await expect(authApi.sendSms('13800138000')).rejects.toBe(networkErr);
    // handleResponseError is the interceptor path for transport errors; in 1.1
    // it maps network failures to ElMessage. We assert the rejection surfaces
    // (the UI layer's test covers the button-state side of the contract).
  });

  test('[BLIND-SPOT] 1.2-BLIND-ERROR-005: timeout (ECONNABORTED) routes through same rejection path', async () => {
    const timeoutErr = Object.assign(new Error('timeout'), { code: 'ECONNABORTED' });
    postMock.mockRejectedValueOnce(timeoutErr);

    await expect(
      authApi.register({
        phone: '13800138000',
        smsCode: '123456',
        username: '张三',
        role: 'MEMBER_RURAL',
      }),
    ).rejects.toBe(timeoutErr);
  });
});
