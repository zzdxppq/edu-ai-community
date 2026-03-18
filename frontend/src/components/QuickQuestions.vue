<template>
  <div class="welcome-container">
    <div class="welcome-message">
      <div class="welcome-icon">🤖</div>
      <div class="welcome-text">
        <p class="welcome-greeting">您好！我是校共体AI助手。</p>
        <p class="welcome-role">您当前身份：<strong>{{ roleLabel }}</strong></p>
        <p class="welcome-hint">以下是为您推荐的常见问题：</p>
      </div>
    </div>

    <div class="questions-grid">
      <button
        v-for="q in questions"
        :key="q.id"
        class="quick-question-btn"
        @click="emit('select', q.text)"
      >
        {{ q.text }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QuickQuestion } from '@/types'
import { ROLE_LABELS } from '@/types'
import { useAuthStore } from '@/stores/auth'

defineProps<{
  questions: QuickQuestion[]
}>()

const emit = defineEmits<{
  (e: 'select', text: string): void
}>()

const authStore = useAuthStore()

const roleLabel = computed(() => {
  if (authStore.user) {
    return ROLE_LABELS[authStore.user.role]
  }
  return '访客'
})
</script>

<style scoped>
.welcome-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 24px 40px;
  max-width: 600px;
  margin: 0 auto;
}

.welcome-message {
  text-align: center;
  margin-bottom: 32px;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-text {
  font-size: 16px;
  line-height: 1.8;
  color: var(--color-text-regular);
}

.welcome-greeting {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.welcome-role {
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.welcome-hint {
  color: var(--color-text-secondary);
}

.questions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

@media (max-width: 768px) {
  .questions-grid {
    grid-template-columns: 1fr;
  }

  .welcome-container {
    padding: 40px 16px 24px;
  }
}
</style>
