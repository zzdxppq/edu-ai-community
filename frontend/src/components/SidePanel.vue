<template>
  <div class="side-panel">
    <!-- Recommendations -->
    <div class="panel-section">
      <div class="section-title">📌 推荐问题</div>
      <div class="section-list">
        <div
          v-for="(item, idx) in recommendations"
          :key="'rec-' + idx"
          class="list-item"
          @click="emit('select', item)"
        >
          {{ item }}
        </div>
        <div v-if="recommendations.length === 0" class="list-empty">
          暂无推荐问题
        </div>
      </div>
    </div>

    <!-- Policy News -->
    <div class="panel-section">
      <div class="section-title">📢 政策动态</div>
      <div class="section-list">
        <div
          v-for="news in policyNews"
          :key="news.id"
          class="list-item"
        >
          <div class="news-title">{{ news.title }}</div>
          <div class="news-date">{{ news.date }}</div>
        </div>
      </div>
    </div>

    <!-- Cases -->
    <div class="panel-section">
      <div class="section-title">📚 相关案例</div>
      <div class="section-list">
        <div
          v-for="c in cases"
          :key="c.id"
          class="list-item"
        >
          {{ c.title }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { mockPolicyNews, mockCases } from '@/mock/data'

defineProps<{
  recommendations: string[]
}>()

const emit = defineEmits<{
  (e: 'select', text: string): void
}>()

const policyNews = mockPolicyNews
const cases = mockCases
</script>

<style scoped>
.side-panel {
  height: 100%;
  overflow-y: auto;
  background-color: var(--color-surface-alt);
  padding: 16px 0;
}

.panel-section {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  border-left: 3px solid var(--color-primary);
  margin: 0 12px 8px;
}

.section-list {
  padding: 0 12px;
}

.list-item {
  padding: 8px 12px;
  font-size: 14px;
  color: var(--color-text-regular);
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s;
  line-height: 1.5;
}

.list-item:hover {
  background-color: var(--color-primary-bg);
}

.list-empty {
  padding: 8px 12px;
  font-size: 14px;
  color: var(--color-text-placeholder);
}

.news-title {
  font-size: 14px;
  color: var(--color-text-regular);
  margin-bottom: 2px;
}

.news-date {
  font-size: 12px;
  color: var(--color-text-placeholder);
}
</style>
