<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Plus, Upload, Search, View, Edit, Delete } from '@element-plus/icons-vue'
import type { KnowledgeItem, DocumentFile } from '@/types'
import {
  mockGetKnowledgeItems,
  mockAddKnowledgeItem,
  mockUpdateKnowledgeItem,
  mockDeleteKnowledgeItem,
  mockGetDocumentFiles,
  mockUploadDocument
} from '@/mock'
import { ElMessage } from 'element-plus'
import type { UploadProps } from 'element-plus'

const loading = ref(false)
const knowledgeItems = ref<KnowledgeItem[]>([])
const documentFiles = ref<DocumentFile[]>([])
const searchText = ref('')
const filterSource = ref('')
const filterStatus = ref('')

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)

const filteredItems = computed(() => {
  let items = knowledgeItems.value
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    items = items.filter(i => i.title.toLowerCase().includes(kw) || i.content.toLowerCase().includes(kw))
  }
  if (filterSource.value) {
    items = items.filter(i => i.source === filterSource.value)
  }
  if (filterStatus.value) {
    items = items.filter(i => i.status === filterStatus.value)
  }
  return items
})

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

// Dialogs
const uploadDialogVisible = ref(false)
const addDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const isEditing = ref(false)
const viewingItem = ref<KnowledgeItem | null>(null)

const formData = reactive({
  id: '',
  title: '',
  content: '',
  source: 'manual' as string
})

function resetForm() {
  formData.id = ''
  formData.title = ''
  formData.content = ''
  formData.source = 'manual'
}

// Load data
async function loadData() {
  loading.value = true
  try {
    const [items, docs] = await Promise.all([mockGetKnowledgeItems(), mockGetDocumentFiles()])
    knowledgeItems.value = items
    documentFiles.value = docs
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

// Source & status helpers
function sourceLabel(s: string) {
  const map: Record<string, string> = { file: '文件上传', qa: '问答沉淀', manual: '手动添加' }
  return map[s] || s
}
function sourceTagType(s: string) {
  const map: Record<string, string> = { file: '', qa: 'success', manual: 'warning' }
  return map[s] || ''
}
function statusLabel(s: string) {
  const map: Record<string, string> = { active: '已启用', pending: '待审核', inactive: '已停用' }
  return map[s] || s
}
function statusTagType(s: string) {
  const map: Record<string, string> = { active: 'success', pending: 'warning', inactive: 'info' }
  return map[s] || ''
}
function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}
function formatTime(t: string) {
  return new Date(t).toLocaleString('zh-CN')
}
function fileStatusLabel(s: string) {
  const map: Record<string, string> = { parsing: '解析中', completed: '已完成', failed: '失败' }
  return map[s] || s
}
function fileStatusType(s: string) {
  const map: Record<string, string> = { parsing: 'warning', completed: 'success', failed: 'danger' }
  return map[s] || ''
}

// Actions
function handleView(item: KnowledgeItem) {
  viewingItem.value = item
  viewDialogVisible.value = true
}

function handleAdd() {
  isEditing.value = false
  resetForm()
  addDialogVisible.value = true
}

function handleEdit(item: KnowledgeItem) {
  isEditing.value = true
  formData.id = item.id
  formData.title = item.title
  formData.content = item.content
  formData.source = item.source
  addDialogVisible.value = true
}

async function handleSave() {
  if (!formData.title.trim() || !formData.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  loading.value = true
  try {
    if (isEditing.value) {
      await mockUpdateKnowledgeItem(formData.id, { title: formData.title, content: formData.content, source: formData.source })
      ElMessage.success('更新成功')
    } else {
      await mockAddKnowledgeItem({ title: formData.title, content: formData.content, source: formData.source })
      ElMessage.success('添加成功')
    }
    addDialogVisible.value = false
    await loadData()
  } finally {
    loading.value = false
  }
}

async function handleDelete(id: string) {
  loading.value = true
  try {
    await mockDeleteKnowledgeItem(id)
    ElMessage.success('删除成功')
    await loadData()
  } finally {
    loading.value = false
  }
}

// Upload
const uploading = ref(false)

const handleUploadChange: UploadProps['onChange'] = async (uploadFile) => {
  if (!uploadFile.raw) return
  const file = uploadFile.raw
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (ext !== 'pdf' && ext !== 'docx') {
    ElMessage.error('仅支持 PDF 和 DOCX 格式')
    return
  }
  uploading.value = true
  try {
    await mockUploadDocument(file.name, file.size, ext)
    ElMessage.success('上传成功，文件解析完成')
    uploadDialogVisible.value = false
    await loadData()
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div>
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/" style="margin-bottom: 16px;">
      <el-breadcrumb-item>运营后台</el-breadcrumb-item>
      <el-breadcrumb-item>知识库管理</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- Knowledge Items Card -->
    <el-card shadow="never" style="margin-bottom: 20px;">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;">
          <span style="font-size: 16px; font-weight: 600;">知识条目</span>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <el-input v-model="searchText" :prefix-icon="Search" placeholder="搜索标题或内容" clearable style="width: 200px;" />
            <el-select v-model="filterSource" placeholder="来源" clearable style="width: 120px;">
              <el-option label="文件上传" value="file" />
              <el-option label="问答沉淀" value="qa" />
              <el-option label="手动添加" value="manual" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px;">
              <el-option label="已启用" value="active" />
              <el-option label="待审核" value="pending" />
              <el-option label="已停用" value="inactive" />
            </el-select>
            <el-button type="primary" :icon="Upload" @click="uploadDialogVisible = true">上传文件</el-button>
            <el-button type="success" :icon="Plus" @click="handleAdd">新增条目</el-button>
          </div>
        </div>
      </template>

      <el-table :data="pagedItems" v-loading="loading" stripe style="width: 100%;">
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.source)" size="small">{{ sourceLabel(row.source) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" size="small" @click="handleView(row)">查看</el-button>
            <el-button text type="warning" :icon="Edit" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除此条目？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button text type="danger" :icon="Delete" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredItems.length"
          layout="total, prev, pager, next"
          background
          small
        />
      </div>
    </el-card>

    <!-- Document Files Card -->
    <el-card shadow="never">
      <template #header>
        <span style="font-size: 16px; font-weight: 600;">已上传文件</span>
      </template>
      <el-table :data="documentFiles" v-loading="loading" stripe style="width: 100%;">
        <el-table-column prop="fileName" label="文件名" min-width="250" show-overflow-tooltip />
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="fileStatusType(row.status)" size="small">{{ fileStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提取条目数" width="110" prop="itemCount" />
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">{{ formatTime(row.uploadedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Upload Dialog -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="500px" destroy-on-close>
      <div style="margin-bottom: 12px; color: #666; font-size: 14px;">
        支持上传 PDF、DOCX 格式的文件，系统将自动解析并提取知识条目。
      </div>
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept=".pdf,.docx"
        @change="handleUploadChange"
      >
        <div v-if="uploading" style="padding: 20px;">
          <el-icon class="el-icon--loading" :size="40" style="color: #1283e9;"><Loading /></el-icon>
          <div style="margin-top: 12px; color: #666;">正在上传并解析文件...</div>
        </div>
        <div v-else style="padding: 20px;">
          <el-icon :size="40" style="color: #1283e9;"><Upload /></el-icon>
          <div style="margin-top: 12px; color: #666;">将文件拖到此处，或<em style="color: #1283e9;">点击上传</em></div>
          <div style="margin-top: 8px; color: #999; font-size: 12px;">支持 PDF、DOCX 格式</div>
        </div>
      </el-upload>
    </el-dialog>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="addDialogVisible" :title="isEditing ? '编辑条目' : '新增条目'" width="600px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="formData.title" placeholder="请输入知识条目标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="formData.content" type="textarea" :rows="6" placeholder="请输入知识条目内容" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="formData.source" style="width: 100%;">
            <el-option label="手动添加" value="manual" />
            <el-option label="问答沉淀" value="qa" />
            <el-option label="文件上传" value="file" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="loading">保存</el-button>
      </template>
    </el-dialog>

    <!-- View Dialog -->
    <el-dialog v-model="viewDialogVisible" title="知识条目详情" width="650px" destroy-on-close>
      <template v-if="viewingItem">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ viewingItem.id }}</el-descriptions-item>
          <el-descriptions-item label="来源">
            <el-tag :type="sourceTagType(viewingItem.source)" size="small">{{ sourceLabel(viewingItem.source) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(viewingItem.status)" size="small">{{ statusLabel(viewingItem.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源文件" v-if="viewingItem.sourceFile">{{ viewingItem.sourceFile }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(viewingItem.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(viewingItem.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ viewingItem.title }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">
            <div style="white-space: pre-wrap; line-height: 1.6;">{{ viewingItem.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>
