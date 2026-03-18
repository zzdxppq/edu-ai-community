<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import type { FeedbackItem } from '@/types'
import { ROLE_LABELS } from '@/types'
import { mockGetFeedbackItems, mockUpdateFeedbackStatus } from '@/mock'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const feedbackItems = ref<FeedbackItem[]>([])

// Filters
const filterRating = ref<number | ''>('')
const filterEscalated = ref('')
const filterStatus = ref('')

const filteredItems = computed(() => {
  let items = feedbackItems.value
  if (filterRating.value !== '') {
    items = items.filter(i => i.rating === filterRating.value)
  }
  if (filterEscalated.value !== '') {
    const esc = filterEscalated.value === 'yes'
    items = items.filter(i => i.escalated === esc)
  }
  if (filterStatus.value) {
    items = items.filter(i => {
      if (filterStatus.value === 'none') return !i.escalated
      return i.escalateStatus === filterStatus.value
    })
  }
  return items
})

// Stats
const totalCount = computed(() => feedbackItems.value.length)
const avgRating = computed(() => {
  if (feedbackItems.value.length === 0) return 0
  const sum = feedbackItems.value.reduce((a, b) => a + b.rating, 0)
  return (sum / feedbackItems.value.length).toFixed(1)
})
const pendingCount = computed(() => feedbackItems.value.filter(i => i.escalated && i.escalateStatus === 'pending').length)
const lowRatingCount = computed(() => feedbackItems.value.filter(i => i.rating <= 2).length)

// Detail dialog
const detailVisible = ref(false)
const detailItem = ref<FeedbackItem | null>(null)

function handleViewDetail(item: FeedbackItem) {
  detailItem.value = item
  detailVisible.value = true
}

function renderStars(rating: number) {
  return '★'.repeat(rating) + '☆'.repeat(5 - rating)
}

function roleLabel(role: string) {
  return ROLE_LABELS[role as keyof typeof ROLE_LABELS] || role
}

function roleTagType(role: string) {
  const map: Record<string, string> = {
    REGIONAL_ADMIN: 'danger',
    LEAD_SCHOOL: '',
    MEMBER_URBAN: 'success',
    MEMBER_RURAL: 'warning',
    RESEARCHER: 'info',
    OTHER: 'info'
  }
  return map[role] || 'info'
}

function escalateStatusLabel(item: FeedbackItem) {
  if (!item.escalated) return '未转专家'
  const map: Record<string, string> = { pending: '待处理', processing: '处理中', completed: '已完成' }
  return map[item.escalateStatus || ''] || '未知'
}

function escalateStatusType(item: FeedbackItem) {
  if (!item.escalated) return 'info'
  const map: Record<string, string> = { pending: 'warning', processing: '', completed: 'success' }
  return map[item.escalateStatus || ''] || 'info'
}

function formatTime(t: string) {
  return new Date(t).toLocaleString('zh-CN')
}

async function loadData() {
  loading.value = true
  try {
    feedbackItems.value = await mockGetFeedbackItems()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

async function handleUpdateStatus(id: string, status: string) {
  loading.value = true
  try {
    await mockUpdateFeedbackStatus(id, status)
    // Update local
    const item = feedbackItems.value.find(i => i.id === id)
    if (item) {
      item.escalateStatus = status as any
      item.escalated = true
    }
    if (detailItem.value && detailItem.value.id === id) {
      detailItem.value.escalateStatus = status as any
      detailItem.value.escalated = true
    }
    ElMessage.success('状态更新成功')
  } finally {
    loading.value = false
  }
}

function handleAddToKnowledge() {
  ElMessage.success('已加入知识库待审核列表')
  detailVisible.value = false
}
</script>

<template>
  <div>
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/" style="margin-bottom: 16px;">
      <el-breadcrumb-item>运营后台</el-breadcrumb-item>
      <el-breadcrumb-item>反馈管理</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- Stats Cards -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" style="text-align: center;">
          <div style="font-size: 28px; font-weight: 700; color: #1283e9;">{{ totalCount }}</div>
          <div style="font-size: 13px; color: #999; margin-top: 4px;">总反馈数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" style="text-align: center;">
          <div style="font-size: 28px; font-weight: 700; color: #e6a23c;">{{ avgRating }} <span style="font-size: 16px;">★</span></div>
          <div style="font-size: 13px; color: #999; margin-top: 4px;">平均评分</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" style="text-align: center;">
          <div style="font-size: 28px; font-weight: 700; color: #e6a23c;">{{ pendingCount }}</div>
          <div style="font-size: 13px; color: #999; margin-top: 4px;">待处理</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" style="text-align: center;">
          <div style="font-size: 28px; font-weight: 700; color: #f56c6c;">{{ lowRatingCount }}</div>
          <div style="font-size: 13px; color: #999; margin-top: 4px;">低分反馈</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Filters + Table -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;">
          <span style="font-size: 16px; font-weight: 600;">反馈列表</span>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <el-select v-model="filterRating" placeholder="评分" clearable style="width: 100px;">
              <el-option v-for="r in 5" :key="r" :label="r + ' 星'" :value="r" />
            </el-select>
            <el-select v-model="filterEscalated" placeholder="是否转专家" clearable style="width: 130px;">
              <el-option label="已转专家" value="yes" />
              <el-option label="未转专家" value="no" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="处理状态" clearable style="width: 120px;">
              <el-option label="待处理" value="pending" />
              <el-option label="处理中" value="processing" />
              <el-option label="已完成" value="completed" />
              <el-option label="未转专家" value="none" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table :data="filteredItems" v-loading="loading" stripe style="width: 100%;" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding: 12px 24px;">
              <div style="margin-bottom: 8px;"><strong>用户提问：</strong></div>
              <div style="background: #f0f7ff; padding: 10px 14px; border-radius: 8px; margin-bottom: 12px; color: #333;">{{ row.question }}</div>
              <div style="margin-bottom: 8px;"><strong>AI 回答：</strong></div>
              <div style="background: #f5f5f5; padding: 10px 14px; border-radius: 8px; color: #555;">{{ row.answer.length > 200 ? row.answer.substring(0, 200) + '...' : row.answer }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="160">
          <template #default="{ row }">
            <div>
              <span style="font-weight: 500;">{{ row.username }}</span>
              <br />
              <el-tag :type="roleTagType(row.userRole)" size="small" style="margin-top: 4px;">{{ roleLabel(row.userRole) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="120">
          <template #default="{ row }">
            <span style="color: #e6a23c; font-size: 16px; letter-spacing: 1px;">{{ renderStars(row.rating) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="反馈原因" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="r in row.reasons" :key="r" size="small" style="margin: 2px;" type="info">{{ r }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评论" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.comment }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="escalateStatusType(row)" size="small">{{ escalateStatusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleViewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="700px" destroy-on-close>
      <template v-if="detailItem">
        <!-- User Info -->
        <div style="margin-bottom: 20px;">
          <div style="font-size: 15px; font-weight: 600; margin-bottom: 10px; color: #333;">用户信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="用户名">{{ detailItem.username }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag :type="roleTagType(detailItem.userRole)" size="small">{{ roleLabel(detailItem.userRole) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="反馈时间">{{ formatTime(detailItem.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="对话ID">{{ detailItem.conversationId }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Rating -->
        <div style="margin-bottom: 20px;">
          <div style="font-size: 15px; font-weight: 600; margin-bottom: 10px; color: #333;">评分与反馈</div>
          <div style="margin-bottom: 8px;">
            <span style="color: #e6a23c; font-size: 22px; letter-spacing: 2px;">{{ renderStars(detailItem.rating) }}</span>
            <span style="margin-left: 8px; color: #666; font-size: 14px;">{{ detailItem.rating }} / 5</span>
          </div>
          <div style="margin-bottom: 8px;">
            <el-tag v-for="r in detailItem.reasons" :key="r" size="small" style="margin-right: 6px;">{{ r }}</el-tag>
          </div>
          <div style="background: #fafafa; padding: 12px; border-radius: 6px; color: #555; line-height: 1.6;">{{ detailItem.comment }}</div>
        </div>

        <!-- Original Conversation -->
        <div style="margin-bottom: 20px;">
          <div style="font-size: 15px; font-weight: 600; margin-bottom: 10px; color: #333;">原始对话</div>
          <!-- User question bubble -->
          <div style="display: flex; justify-content: flex-end; margin-bottom: 12px;">
            <div style="background: #1283e9; color: #fff; padding: 10px 14px; border-radius: 12px 12px 2px 12px; max-width: 80%; line-height: 1.6;">
              {{ detailItem.question }}
            </div>
          </div>
          <!-- AI answer bubble -->
          <div style="display: flex; justify-content: flex-start; margin-bottom: 12px;">
            <div style="background: #f5f5f5; color: #333; padding: 10px 14px; border-radius: 12px 12px 12px 2px; max-width: 80%; line-height: 1.6;">
              {{ detailItem.answer }}
            </div>
          </div>
        </div>

        <!-- Processing Status -->
        <div style="margin-bottom: 16px;">
          <div style="font-size: 15px; font-weight: 600; margin-bottom: 10px; color: #333;">处理状态</div>
          <el-tag :type="escalateStatusType(detailItem)" size="default">{{ escalateStatusLabel(detailItem) }}</el-tag>
        </div>
      </template>

      <template #footer>
        <div style="display: flex; gap: 8px; justify-content: flex-end;">
          <el-button
            v-if="detailItem && (!detailItem.escalated || detailItem.escalateStatus === 'pending')"
            type="primary"
            @click="handleUpdateStatus(detailItem!.id, 'processing')"
            :loading="loading"
          >标记处理中</el-button>
          <el-button
            v-if="detailItem && detailItem.escalated && detailItem.escalateStatus === 'processing'"
            type="success"
            @click="handleUpdateStatus(detailItem!.id, 'completed')"
            :loading="loading"
          >标记已完成</el-button>
          <el-button type="warning" @click="handleAddToKnowledge">加入知识库</el-button>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
