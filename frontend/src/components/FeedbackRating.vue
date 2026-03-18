<template>
  <div class="feedback-wrapper">
    <!-- Stars -->
    <div class="star-row" v-if="state !== 'escalated'">
      <div class="star-rating">
        <svg
          v-for="i in 5"
          :key="i"
          :class="['star', { active: i <= rating, hover: i <= hoverRating && hoverRating > 0 }]"
          @mouseenter="hoverRating = i"
          @mouseleave="hoverRating = 0"
          @click="handleRate(i)"
          viewBox="0 0 24 24"
          fill="currentColor"
          width="20"
          height="20"
        >
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14 2 9.27l6.91-1.01L12 2z" />
        </svg>
      </div>
      <el-tag v-if="state === 'rated' && !showForm" size="small" type="info">已评分</el-tag>
      <el-tag v-if="state === 'submitted'" size="small" type="success">已提交反馈</el-tag>
      <el-button
        v-if="state === 'rated' && !showForm"
        text
        size="small"
        type="primary"
        @click="showForm = true"
      >
        反馈
      </el-button>
    </div>
    <el-tag v-if="state === 'escalated'" size="small" type="warning">已提交专家</el-tag>

    <!-- Feedback Form -->
    <div v-if="showForm" class="feedback-form">
      <div class="form-label">请选择反馈原因：</div>
      <el-checkbox-group v-model="reasons" class="reason-group">
        <el-checkbox label="回答不准确" value="回答不准确" />
        <el-checkbox label="未引用出处" value="未引用出处" />
        <el-checkbox label="答非所问" value="答非所问" />
        <el-checkbox label="内容过于笼统" value="内容过于笼统" />
        <el-checkbox label="其他" value="其他" />
      </el-checkbox-group>

      <el-input
        v-model="comment"
        type="textarea"
        placeholder="请输入补充说明（选填）"
        :maxlength="500"
        show-word-limit
        :rows="3"
        class="comment-input"
      />

      <div class="form-actions">
        <el-button type="primary" size="small" @click="handleSubmitFeedback" :loading="submitting">
          提交反馈
        </el-button>
        <el-button size="small" type="warning" plain @click="handleEscalate" :loading="escalating">
          提交给专家处理
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { mockSubmitFeedback, mockEscalate } from '@/mock'

const props = defineProps<{
  messageId: string
}>()

type FeedbackState = 'none' | 'rated' | 'submitted' | 'escalated'

const rating = ref(0)
const hoverRating = ref(0)
const state = ref<FeedbackState>('none')
const showForm = ref(false)
const reasons = ref<string[]>([])
const comment = ref('')
const submitting = ref(false)
const escalating = ref(false)

function handleRate(value: number) {
  rating.value = value
  state.value = 'rated'
  if (value <= 2) {
    showForm.value = true
  }
}

async function handleSubmitFeedback() {
  submitting.value = true
  try {
    await mockSubmitFeedback(props.messageId, rating.value, reasons.value, comment.value)
    state.value = 'submitted'
    showForm.value = false
    ElMessage.success('感谢您的反馈')
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function handleEscalate() {
  escalating.value = true
  try {
    await mockEscalate(props.messageId)
    state.value = 'escalated'
    showForm.value = false
    ElMessage.success('已提交给专家处理')
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    escalating.value = false
  }
}
</script>

<style scoped>
.feedback-wrapper {
  margin-top: 8px;
}

.star-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.feedback-form {
  margin-top: 12px;
  padding: 12px;
  background: var(--color-surface-alt);
  border-radius: 8px;
  max-width: 400px;
}

.form-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.reason-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 12px;
}

.comment-input {
  margin-bottom: 12px;
}

.form-actions {
  display: flex;
  gap: 8px;
}
</style>
