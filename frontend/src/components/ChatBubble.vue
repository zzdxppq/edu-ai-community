<template>
  <div :class="['message-row', message.role]">
    <!-- Avatar -->
    <el-avatar :size="36" :class="['avatar', message.role]">
      <el-icon v-if="message.role === 'user'" :size="18"><User /></el-icon>
      <el-icon v-else :size="18"><Monitor /></el-icon>
    </el-avatar>

    <div class="message-content">
      <!-- Bubble -->
      <div :class="['message-bubble', message.role]">
        <div v-if="message.role === 'assistant'" class="markdown-body" v-html="renderedContent"></div>
        <div v-else>{{ message.content }}</div>
      </div>

      <!-- Time -->
      <div class="message-time">{{ formatTime(message.createdAt) }}</div>

      <!-- Feedback for assistant -->
      <FeedbackRating v-if="message.role === 'assistant'" :message-id="message.id" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, Monitor } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import type { Message, Citation } from '@/types'
import FeedbackRating from './FeedbackRating.vue'

const props = defineProps<{
  message: Message
}>()

const emit = defineEmits<{
  (e: 'citation-click', citation: Citation): void
}>()

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true
})

const renderedContent = computed(() => {
  let content = props.message.content
  // Replace [n] citation markers with clickable badges
  content = content.replace(/\[(\d+)\]/g, (_match, num) => {
    const index = parseInt(num)
    const citation = props.message.citations?.find(c => c.index === index)
    if (citation) {
      return `<span class="citation-badge" data-citation-index="${index}" title="${citation.source}">[${num}]</span>`
    }
    return `<span class="citation-badge">[${num}]</span>`
  })
  return md.render(content)
})

function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)

  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 800px;
  padding: 0 16px;
}

.message-row.user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-row.assistant {
  flex-direction: row;
  margin-right: auto;
}

.avatar.user {
  background-color: var(--color-primary);
  color: #ffffff;
  flex-shrink: 0;
}

.avatar.assistant {
  background-color: #13c2c2;
  color: #ffffff;
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.message-row.user .message-content {
  align-items: flex-end;
}

.message-row.assistant .message-content {
  align-items: flex-start;
}

.message-bubble {
  font-size: 16px;
}

.message-time {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin-top: 4px;
  padding: 0 4px;
}

.markdown-body :deep(p) {
  margin-bottom: 8px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin-bottom: 8px;
}

.markdown-body :deep(li) {
  margin-bottom: 4px;
}

.markdown-body :deep(strong) {
  font-weight: 600;
}

.markdown-body :deep(.citation-badge) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  margin: 0 2px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  background-color: var(--color-primary-bg);
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s;
  vertical-align: super;
  line-height: 20px;
}

.markdown-body :deep(.citation-badge:hover) {
  background-color: var(--color-primary);
  color: #ffffff;
}

@media (max-width: 768px) {
  .message-bubble {
    max-width: 85%;
  }
}

@media (min-width: 769px) {
  .message-bubble {
    max-width: 70%;
  }
}
</style>
