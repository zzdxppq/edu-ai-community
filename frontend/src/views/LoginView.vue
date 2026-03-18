<template>
  <div class="login-page">
    <!-- Top Bar -->
    <div class="top-bar">
      <div class="top-bar-content">
        <div class="brand">
          <span class="brand-icon">🔵</span>
          <span class="brand-text">河南城乡校共体发展平台</span>
        </div>
        <el-button text class="back-link">返回主站</el-button>
      </div>
    </div>

    <!-- Login Card -->
    <div class="login-container">
      <div class="login-card">
        <div class="card-header">
          <div class="card-icon">🤖</div>
          <h2 class="card-title">AI智能助手</h2>
          <p class="card-subtitle">{{ isRegister ? '注册新账号' : '登录您的账号' }}</p>
        </div>

        <el-form :model="form" @submit.prevent="handleSubmit" class="login-form">
          <!-- Phone -->
          <el-form-item>
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              size="large"
              maxlength="11"
              :prefix-icon="Iphone"
            />
          </el-form-item>

          <!-- SMS Code -->
          <el-form-item>
            <div class="sms-row">
              <el-input
                v-model="form.code"
                placeholder="请输入验证码"
                size="large"
                maxlength="6"
                :prefix-icon="Lock"
                class="sms-input"
              />
              <el-button
                size="large"
                :disabled="smsCountdown > 0"
                @click="handleSendSms"
                class="sms-btn"
              >
                {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <!-- Register fields -->
          <template v-if="isRegister">
            <el-form-item>
              <el-input
                v-model="form.username"
                placeholder="请输入您的姓名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>

            <!-- Role Selection -->
            <el-form-item>
              <div class="role-label">选择您的身份</div>
              <div class="role-grid">
                <div
                  v-for="(label, key) in ROLE_LABELS"
                  :key="key"
                  :class="['role-card', { selected: form.role === key }]"
                  @click="form.role = key as UserRole"
                >
                  <div class="role-card-name">{{ label }}</div>
                  <div class="role-card-desc">{{ ROLE_DESCRIPTIONS[key as UserRole] }}</div>
                </div>
              </div>
            </el-form-item>
          </template>

          <!-- Demo Hint -->
          <div class="demo-hint">
            <el-icon><InfoFilled /></el-icon>
            Demo 验证码: 123456
          </div>

          <!-- Submit -->
          <el-button
            type="primary"
            size="large"
            native-type="submit"
            :loading="loading"
            class="submit-btn"
          >
            {{ isRegister ? '注册' : '登录' }}
          </el-button>
        </el-form>

        <!-- Toggle -->
        <div class="toggle-row">
          <span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
          <el-button text type="primary" @click="isRegister = !isRegister">
            {{ isRegister ? '立即登录' : '注册账号' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, User, InfoFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { ROLE_LABELS, ROLE_DESCRIPTIONS } from '@/types'
import type { UserRole } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const isRegister = ref(false)
const loading = ref(false)
const smsCountdown = ref(0)

const form = reactive({
  phone: '',
  code: '',
  username: '',
  role: 'MEMBER_RURAL' as UserRole
})

let smsTimer: ReturnType<typeof setInterval> | null = null

async function handleSendSms() {
  if (!form.phone || form.phone.length !== 11) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  try {
    await authStore.sendSms(form.phone)
    ElMessage.success('验证码已发送')
    smsCountdown.value = 60
    smsTimer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0 && smsTimer) {
        clearInterval(smsTimer)
        smsTimer = null
      }
    }, 1000)
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  }
}

async function handleSubmit() {
  if (!form.phone || form.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!form.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      if (!form.username) {
        ElMessage.warning('请输入姓名')
        loading.value = false
        return
      }
      await authStore.register(form.phone, form.code, form.username, form.role)
      ElMessage.success('注册成功')
    } else {
      await authStore.login(form.phone, form.code)
      ElMessage.success('登录成功')
    }
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-color: var(--color-surface);
}

.top-bar {
  background-color: #ffffff;
  border-bottom: 1px solid var(--color-border);
  height: 56px;
  display: flex;
  align-items: center;
}

.top-bar-content {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-icon {
  font-size: 20px;
}

.brand-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.back-link {
  color: var(--color-text-secondary);
}

.login-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 60px 16px;
}

.login-card {
  width: 100%;
  max-width: 460px;
  background: #ffffff;
  border-radius: 12px;
  padding: 40px 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.card-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.login-form {
  width: 100%;
}

.sms-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.sms-input {
  flex: 1;
}

.sms-btn {
  min-width: 120px;
}

.role-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-regular);
  margin-bottom: 12px;
  width: 100%;
}

.role-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

.role-card {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.role-card:hover {
  border-color: var(--color-primary-light);
}

.role-card.selected {
  border-color: var(--color-primary);
  background-color: var(--color-primary-bg);
}

.role-card-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 4px;
}

.role-card-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.demo-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-placeholder);
  margin-bottom: 16px;
  padding: 8px 12px;
  background-color: var(--color-surface-alt);
  border-radius: 6px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.toggle-row {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

@media (max-width: 480px) {
  .login-card {
    padding: 32px 20px;
  }

  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
