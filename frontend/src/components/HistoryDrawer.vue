<template>
  <el-drawer
    v-model="visible"
    title="历史对话"
    direction="rtl"
    size="360px"
    :show-close="true"
  >
    <div class="history-content">
      <el-button type="primary" class="new-chat-btn" @click="handleNewChat">
        <el-icon><Plus /></el-icon>
        开始新对话
      </el-button>

      <div v-if="conversations.length === 0" class="empty-state">
        <el-empty description="还没有对话记录，开始您的第一次提问吧" :image-size="80" />
      </div>

      <div v-else class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conversation-item', { active: conv.id === currentId }]"
          @click="handleSelect(conv.id)"
        >
          <div class="conv-title">{{ conv.title }}</div>
          <div class="conv-time">{{ formatRelativeTime(conv.updatedAt) }}</div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import type { Conversation } from '@/types'

const props = defineProps<{
  modelValue: boolean
  conversations: Conversation[]
  currentId: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', id: string): void
  (e: 'new-chat'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
})

function handleSelect(id: string) {
  emit('select', id)
  visible.value = false
}

function handleNewChat() {
  emit('new-chat')
  visible.value = false
}

function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 30) return `${diffDay}天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.history-content {
  padding: 0;
}

.new-chat-btn {
  width: 100%;
  margin-bottom: 16px;
}

.empty-state {
  padding: 40px 0;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conversation-item {
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.conversation-item:hover {
  background-color: var(--color-primary-bg);
}

.conversation-item.active {
  background-color: var(--color-primary-bg);
  border-left: 3px solid var(--color-primary);
}

.conv-title {
  font-size: 14px;
  color: var(--color-text);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 12px;
  color: var(--color-text-placeholder);
}
</style>
