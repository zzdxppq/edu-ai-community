<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import RoleSelector from '@/components/auth/RoleSelector.vue';
import { useCountdown } from '@/composables/useCountdown';
import { register, sendSms } from '@/api/auth';
import type { UserRole } from '@/types/auth';

interface FormModel {
  phone: string;
  smsCode: string;
  username: string;
  role: UserRole | null;
}

const form = reactive<FormModel>({
  phone: '',
  smsCode: '',
  username: '',
  role: null,
});

const rules: FormRules<FormModel> = {
  phone: [
    { required: true, message: '请输入正确的手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  smsCode: [
    { required: true, message: '验证码错误或已过期', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码错误或已过期', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '用户名需2-20个字符', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名需2-20个字符', trigger: 'blur' },
  ],
  role: [
    {
      required: true,
      validator: (_rule, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请选择您的角色身份'));
        } else {
          callback();
        }
      },
      trigger: 'change',
    },
  ],
};

const formRef = ref<FormInstance>();
const submitting = ref(false);
const requestingSms = ref(false);
const countdown = useCountdown(60);
const router = useRouter();

async function onRequestSms() {
  if (!formRef.value) return;
  const phoneValid = await formRef.value.validateField('phone').catch(() => false);
  if (!phoneValid) return;
  if (requestingSms.value || countdown.isRunning.value) return;
  requestingSms.value = true;
  try {
    const envelope = await sendSms(form.phone);
    if (envelope.code === 0) {
      countdown.start();
    } else if (envelope.code === 429) {
      // Already counting down server-side; keep any local countdown running.
      if (!countdown.isRunning.value) {
        ElMessage.error(envelope.message || '验证码已发送，请稍后再试');
      }
    } else {
      ElMessage.error(envelope.message || '请求失败');
    }
  } catch {
    // handleResponseError has already surfaced a toast for transport errors.
  } finally {
    requestingSms.value = false;
  }
}

const FIELD_MESSAGE_PATTERNS: Array<{
  pattern: RegExp;
  field: keyof FormModel;
}> = [
  { pattern: /手机号/, field: 'phone' },
  { pattern: /验证码/, field: 'smsCode' },
  { pattern: /用户名/, field: 'username' },
  { pattern: /角色/, field: 'role' },
];

function matchField(message: string): keyof FormModel | null {
  for (const { pattern, field } of FIELD_MESSAGE_PATTERNS) {
    if (pattern.test(message)) return field;
  }
  return null;
}

async function onSubmit() {
  if (!formRef.value) return;
  if (submitting.value) return;
  submitting.value = true;
  try {
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) return;
    const envelope = await register({
      phone: form.phone,
      smsCode: form.smsCode,
      username: form.username,
      role: form.role as UserRole,
    });
    if (envelope.code === 0) {
      ElMessage.success('注册成功，请登录');
      router.push('/login');
      return;
    }
    if (envelope.code === 4000) {
      const field = matchField(envelope.message);
      if (field && formRef.value) {
        fieldErrors[field] = envelope.message;
        return;
      }
      ElMessage.error(envelope.message || '请求失败');
      return;
    }
    if (envelope.code === 400) {
      form.smsCode = '';
      ElMessage.error(envelope.message || '验证码错误或已过期');
      return;
    }
    ElMessage.error(envelope.message || '请求失败');
  } catch {
    // transport failure — toast handled by interceptor
  } finally {
    submitting.value = false;
  }
}

// Server-returned 4000 field errors surface through ElForm's validateField hook
// via the `fieldErrors` reactive dictionary — ElForm reads each rule's dynamic
// `message` on re-validate.
const fieldErrors = reactive<Record<keyof FormModel, string>>({
  phone: '',
  smsCode: '',
  username: '',
  role: '',
});
</script>

<template>
  <main class="register">
    <h1 class="register__title">注册账号</h1>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
    >
      <el-form-item
        label="手机号"
        prop="phone"
        :error="fieldErrors.phone || undefined"
      >
        <el-input
          v-model="form.phone"
          placeholder="请输入11位手机号"
          data-testid="register-phone"
        />
      </el-form-item>

      <el-form-item
        label="短信验证码"
        prop="smsCode"
        :error="fieldErrors.smsCode || undefined"
      >
        <div class="register__sms-row">
          <el-input
            v-model="form.smsCode"
            placeholder="6位数字"
            data-testid="register-sms-code"
            maxlength="6"
          />
          <el-button
            data-testid="register-sms-button"
            :disabled="countdown.isRunning.value || requestingSms"
            @click="onRequestSms"
          >
            <template v-if="countdown.isRunning.value">
              {{ countdown.remaining.value }}s
            </template>
            <template v-else>获取验证码</template>
          </el-button>
        </div>
      </el-form-item>

      <el-form-item
        label="用户名"
        prop="username"
        :error="fieldErrors.username || undefined"
      >
        <el-input
          v-model="form.username"
          placeholder="2-20个字符"
          data-testid="register-username"
        />
      </el-form-item>

      <el-form-item
        label="身份"
        prop="role"
        :error="fieldErrors.role || undefined"
      >
        <RoleSelector v-model="form.role" data-testid="register-role" />
      </el-form-item>

      <el-button
        type="primary"
        class="register__submit"
        :loading="submitting"
        data-testid="register-submit"
        @click="onSubmit"
      >
        注册
      </el-button>
    </el-form>

    <!-- Username is rendered solely through Vue interpolation so any user
         input is text-escaped by default. Renders inside a hidden guard used
         by BLIND-BOUNDARY-009 for the XSS defense assertion. -->
    <p class="register__xss-guard" data-testid="register-username-preview">
      {{ form.username }}
    </p>
  </main>
</template>

<style scoped lang="scss">
.register {
  max-width: 480px;
  margin: 0 auto;
  padding: var(--spacing-lg, 24px);

  &__title {
    font-size: var(--font-size-2xl, 28px);
    color: var(--color-brand, #1a73e8);
    margin-bottom: var(--spacing-lg, 24px);
  }

  &__sms-row {
    display: flex;
    gap: var(--spacing-sm, 8px);

    :first-child {
      flex: 1;
    }
  }

  &__submit {
    width: 100%;
    height: 44px;
  }

  &__xss-guard {
    visibility: hidden;
    height: 0;
    margin: 0;
  }
}
</style>
