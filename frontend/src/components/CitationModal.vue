<template>
  <el-dialog
    v-model="visible"
    :title="'📄 ' + (citation?.source || '')"
    width="600px"
    :close-on-click-modal="true"
    class="citation-dialog"
  >
    <div class="citation-content" v-if="citation">
      <div class="citation-meta">
        <el-tag size="small" type="info">{{ citation.section }}</el-tag>
        <span class="citation-page">第 {{ citation.page }} 页</span>
      </div>

      <div class="document-preview">
        <p class="context-text">
          ......在推进城乡义务教育一体化发展过程中，各地积极探索建立城乡学校共同体机制，
          通过制度创新和实践探索，形成了一系列有效的工作模式和经验做法。
        </p>
        <p class="highlighted-text">
          <span class="citation-highlight">
            根据文件要求，校共体建设应坚持"优质带动、资源共享、合作共进、均衡发展"的基本原则，
            充分发挥牵头校的示范引领作用，促进成员校教育教学质量整体提升，
            逐步缩小城乡教育差距，实现区域教育优质均衡发展目标。
          </span>
        </p>
        <p class="context-text">
          各级教育行政部门应当加强对校共体建设的指导和监督，建立健全工作推进机制，
          确保各项工作任务落到实处......
        </p>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <span class="page-info" v-if="citation">来源：{{ citation.source }} · 第{{ citation.page }}页</span>
        <el-button @click="visible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Citation } from '@/types'

const props = defineProps<{
  modelValue: boolean
  citation: Citation | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
})
</script>

<style scoped>
.citation-content {
  padding: 8px 0;
}

.citation-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.citation-page {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.document-preview {
  background-color: var(--color-surface-alt);
  border-radius: 8px;
  padding: 20px;
  line-height: 1.8;
  font-size: 15px;
  color: var(--color-text-regular);
}

.context-text {
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.context-text:last-child {
  margin-bottom: 0;
  margin-top: 12px;
}

.highlighted-text {
  margin-bottom: 0;
}

.citation-highlight {
  background-color: #fff3cd;
  padding: 2px 4px;
  border-radius: 2px;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-info {
  font-size: 13px;
  color: var(--color-text-placeholder);
}
</style>
