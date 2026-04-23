<script setup lang="ts">
import { onMounted, ref } from 'vue';
import http from '@/api';

const PROJECT_NAME = 'edu-ai-community';
const status = ref<string>('检测中…');

onMounted(async () => {
  try {
    const response = await http.get<{ status?: string }>('/api/health');
    const reported = response.data?.status;
    status.value = reported === 'UP' ? 'UP' : `服务异常（${reported ?? '未知'}）`;
  } catch {
    // The response interceptor has already surfaced a user-visible toast.
    status.value = '无法连接到服务';
  }
});
</script>

<template>
  <main class="home">
    <h1 class="home__title">{{ PROJECT_NAME }}</h1>
    <p class="home__status" data-testid="health-status">服务状态：{{ status }}</p>
  </main>
</template>

<style scoped lang="scss">
.home {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--spacing-lg, 24px);
  font-family: var(--font-family-sans, system-ui);

  &__title {
    font-size: var(--font-size-2xl, 28px);
    color: var(--color-brand, #1a73e8);
    margin-bottom: var(--spacing-md, 16px);
  }

  &__status {
    color: var(--color-text-primary, #1f2937);
  }
}
</style>
