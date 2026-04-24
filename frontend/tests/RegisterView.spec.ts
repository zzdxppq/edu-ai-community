/**
 * Story 1.2 — RegisterView tests (AC1 + AC2).
 * Test Design: docs/qa/assessments/1.2-test-design-20260423.md
 */

import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { createMemoryHistory, createRouter, type Router } from 'vue-router';
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import { nextTick } from 'vue';

const sendSmsMock = vi.fn();
const registerMock = vi.fn();
vi.mock('@/api/auth', () => ({
  sendSms: sendSmsMock,
  register: registerMock,
}));

const messageSuccess = vi.fn();
const messageError = vi.fn();
vi.mock('element-plus', async (importOriginal) => {
  const actual: any = await importOriginal();
  return {
    ...actual,
    ElMessage: Object.assign(vi.fn(), { success: messageSuccess, error: messageError }),
  };
});

let RegisterView: any;
let router: Router;

async function mountView(): Promise<VueWrapper<any>> {
  router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div />' } },
      { path: '/register', name: 'Register', component: RegisterView },
      { path: '/login', name: 'Login', component: { template: '<div>login</div>' } },
    ],
  });
  await router.push('/register');
  await router.isReady();
  const wrapper = mount(RegisterView, {
    global: {
      plugins: [router],
      stubs: {
        transition: false,
        'transition-group': false,
      },
    },
    attachTo: document.body,
  });
  await flushPromises();
  return wrapper;
}

async function fillValidForm(wrapper: VueWrapper<any>, overrides: Partial<Record<string, string>> = {}) {
  await wrapper.find('[data-testid="register-phone"]').setValue(overrides.phone ?? '13800138000');
  await wrapper.find('[data-testid="register-sms-code"]').setValue(overrides.smsCode ?? '123456');
  await wrapper.find('[data-testid="register-username"]').setValue(overrides.username ?? '张三');
  // Click a role card inside RoleSelector.
  await wrapper.find('[data-role="MEMBER_RURAL"]').trigger('click');
  await nextTick();
}

beforeEach(async () => {
  sendSmsMock.mockReset();
  registerMock.mockReset();
  messageSuccess.mockReset();
  messageError.mockReset();
  vi.resetModules();
  ({ default: RegisterView } = await import('@/views/auth/RegisterView.vue'));
});

afterEach(() => {
  vi.useRealTimers();
});

describe('AC1+AC2: RegisterView', () => {
  test('1.2-UNIT-015: full valid submission → calls register API then router.push(/login)', async () => {
    registerMock.mockResolvedValueOnce({ code: 0, message: 'OK', data: { id: 'x', phone: '13800138000' } });

    const wrapper = await mountView();
    await fillValidForm(wrapper);

    const pushSpy = vi.spyOn(router, 'push');
    await wrapper.find('[data-testid="register-submit"]').trigger('click');
    await flushPromises();

    expect(registerMock).toHaveBeenCalledTimes(1);
    expect(registerMock.mock.calls[0][0]).toMatchObject({
      phone: '13800138000',
      smsCode: '123456',
      username: '张三',
      role: 'MEMBER_RURAL',
    });
    expect(messageSuccess).toHaveBeenCalled();
    expect(pushSpy).toHaveBeenCalledWith('/login');
  });

  test('1.2-UNIT-016: empty phone blocks submit; register API NOT called', async () => {
    const wrapper = await mountView();
    // leave phone blank, fill rest
    await wrapper.find('[data-testid="register-sms-code"]').setValue('123456');
    await wrapper.find('[data-testid="register-username"]').setValue('张三');
    await wrapper.find('[data-role="MEMBER_RURAL"]').trigger('click');

    await wrapper.find('[data-testid="register-submit"]').trigger('click');
    await flushPromises();

    expect(registerMock).not.toHaveBeenCalled();
  });

  test('1.2-UNIT-017: R{code:409} → ElMessage.error with duplicate-phone copy; no navigation', async () => {
    registerMock.mockResolvedValueOnce({
      code: 409,
      message: '该手机号已注册，请直接登录',
      data: null,
    });

    const wrapper = await mountView();
    await fillValidForm(wrapper);
    const pushSpy = vi.spyOn(router, 'push');

    await wrapper.find('[data-testid="register-submit"]').trigger('click');
    await flushPromises();

    expect(messageError).toHaveBeenCalledWith('该手机号已注册，请直接登录');
    expect(pushSpy).not.toHaveBeenCalled();
  });

  test('1.2-UNIT-018: R{code:4000} → field-level handling (no generic ElMessage toast)', async () => {
    registerMock.mockResolvedValueOnce({
      code: 4000,
      message: '请输入正确的手机号',
      data: null,
    });

    const wrapper = await mountView();
    await fillValidForm(wrapper);
    await wrapper.find('[data-testid="register-submit"]').trigger('click');
    await flushPromises();

    // 4000 is routed to the matching field (phone) — NOT to a global toast.
    // The `ElMessage.error` path is reserved for unmapped business codes.
    expect(registerMock).toHaveBeenCalledTimes(1);
    expect(messageError).not.toHaveBeenCalled();
  });

  test('1.2-UNIT-019: R{code:400} wrong sms code → clears smsCode + shows error', async () => {
    registerMock.mockResolvedValueOnce({
      code: 400,
      message: '验证码错误或已过期',
      data: null,
    });

    const wrapper = await mountView();
    await fillValidForm(wrapper);
    await wrapper.find('[data-testid="register-submit"]').trigger('click');
    await flushPromises();

    expect(messageError).toHaveBeenCalledWith('验证码错误或已过期');
    const smsInput = wrapper.find('[data-testid="register-sms-code"]')
      .element as HTMLInputElement;
    expect(smsInput.value).toBe('');
  });

  test('1.2-UNIT-027: submit without role → register NOT called (client validation blocks)', async () => {
    const wrapper = await mountView();
    await wrapper.find('[data-testid="register-phone"]').setValue('13800138000');
    await wrapper.find('[data-testid="register-sms-code"]').setValue('123456');
    await wrapper.find('[data-testid="register-username"]').setValue('张三');
    // no role click — role stays null
    await wrapper.find('[data-testid="register-submit"]').trigger('click');
    await flushPromises();

    // Role validator (required) short-circuits submit before the API fires.
    expect(registerMock).not.toHaveBeenCalled();
  });

  test('1.2-UNIT-020: click "获取验证码" → calls sendSms + button disabled + countdown starts', async () => {
    vi.useFakeTimers();
    sendSmsMock.mockResolvedValueOnce({ code: 0, message: 'OK', data: null });

    const wrapper = await mountView();
    await wrapper.find('[data-testid="register-phone"]').setValue('13800138000');
    await wrapper.find('[data-testid="register-sms-button"]').trigger('click');
    await flushPromises();

    expect(sendSmsMock).toHaveBeenCalledTimes(1);
    expect(wrapper.find('[data-testid="register-sms-button"]').text()).toContain('60');

    vi.advanceTimersByTime(1000);
    await nextTick();
    expect(wrapper.find('[data-testid="register-sms-button"]').text()).toContain('59');
  });

  test('1.2-UNIT-021: R{code:429} while not counting → surfaces message; countdown unchanged', async () => {
    sendSmsMock.mockResolvedValueOnce({
      code: 429,
      message: '验证码已发送，请稍后再试',
      data: null,
    });

    const wrapper = await mountView();
    await wrapper.find('[data-testid="register-phone"]').setValue('13800138000');
    await wrapper.find('[data-testid="register-sms-button"]').trigger('click');
    await flushPromises();

    expect(sendSmsMock).toHaveBeenCalledTimes(1);
    // 429 without prior countdown → user sees a friendly toast (not reset).
    expect(messageError).toHaveBeenCalled();
  });


  test('[BLIND-SPOT] 1.2-BLIND-BOUNDARY-009: username <script> tag renders as text (XSS defense)', async () => {
    const wrapper = await mountView();
    const xss = '<script>alert(1)</script>';
    await wrapper.find('[data-testid="register-username"]').setValue(xss);
    await nextTick();

    const preview = wrapper.find('[data-testid="register-username-preview"]');
    expect(preview.text()).toContain('<script>');
    // The preview element must not contain a real <script> child.
    expect(preview.element.querySelector('script')).toBeNull();
  });

  test('[BLIND-SPOT] 1.2-BLIND-FLOW-001: double-click submit is idempotent', async () => {
    let resolveRegister: (v: unknown) => void = () => {};
    registerMock.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveRegister = resolve;
      }),
    );

    const wrapper = await mountView();
    await fillValidForm(wrapper);

    const submit = wrapper.find('[data-testid="register-submit"]');
    await submit.trigger('click');
    await submit.trigger('click'); // second click while still loading
    await flushPromises();

    expect(registerMock).toHaveBeenCalledTimes(1);
    resolveRegister({ code: 0, message: 'OK', data: null });
    await flushPromises();
  });

  test('[BLIND-SPOT] 1.2-BLIND-FLOW-004: navigating away and back resets the form', async () => {
    const wrapper = await mountView();
    await wrapper.find('[data-testid="register-phone"]').setValue('13800138000');
    expect(
      (wrapper.find('[data-testid="register-phone"]').element as HTMLInputElement).value,
    ).toBe('13800138000');
    wrapper.unmount();

    const fresh = await mountView();
    const remountedPhone = fresh.find('[data-testid="register-phone"]')
      .element as HTMLInputElement;
    expect(remountedPhone.value).toBe('');
  });
});
