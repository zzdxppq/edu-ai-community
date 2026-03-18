<template>
  <div class="chat-layout">
    <!-- Header -->
    <header class="chat-header shadow-header">
      <div class="header-content">
        <div class="header-left">
          <!-- Mobile hamburger -->
          <el-button
            class="mobile-menu-btn"
            text
            @click="sidebarDrawerVisible = true"
          >
            <el-icon :size="22"><Operation /></el-icon>
          </el-button>

          <div class="brand">
            <span class="brand-icon">🔵</span>
            <span class="brand-text desktop-only">河南城乡校共体发展平台</span>
            <span class="brand-text mobile-only">校共体平台</span>
          </div>
          <el-tag type="primary" size="small" class="ai-tag desktop-only">AI助手</el-tag>
        </div>

        <div class="header-right">
          <el-button text @click="historyDrawerVisible = true" class="desktop-only">
            <el-icon><Clock /></el-icon>
            <span>历史</span>
          </el-button>
          <el-button text @click="handleNewChat" class="desktop-only">
            <el-icon><Plus /></el-icon>
            <span>新对话</span>
          </el-button>
          <!-- Mobile new chat button -->
          <el-button text @click="handleNewChat" class="mobile-only">
            <el-icon :size="20"><Plus /></el-icon>
          </el-button>

          <el-button text @click="router.push('/admin')" class="desktop-only">
            <el-icon><Setting /></el-icon>
            <span>运营后台</span>
          </el-button>
          <div class="user-info desktop-only">
            <el-avatar :size="32" class="user-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span class="user-name">{{ authStore.user?.username || '用户' }}</span>
          </div>
          <el-button text @click="handleLogout" class="desktop-only">
            <el-icon><SwitchButton /></el-icon>
          </el-button>

          <!-- Mobile: history + logout in one -->
          <el-button text @click="historyDrawerVisible = true" class="mobile-only">
            <el-icon :size="20"><Clock /></el-icon>
          </el-button>
        </div>
      </div>
    </header>

    <!-- Body -->
    <div class="chat-body">
      <!-- Desktop Sidebar -->
      <aside class="sidebar desktop-only">
        <SidePanel :recommendations="recommendations" @select="handleQuestionSelect" />
      </aside>

      <!-- Mobile Sidebar Drawer -->
      <el-drawer
        v-model="sidebarDrawerVisible"
        direction="ltr"
        size="280px"
        :show-close="true"
        title="菜单"
        class="mobile-only-drawer"
      >
        <SidePanel :recommendations="recommendations" @select="handleSidebarQuestionSelect" />
        <template #footer>
          <div class="drawer-footer">
            <div class="drawer-user-info">
              <el-avatar :size="28" class="user-avatar-small">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span>{{ authStore.user?.username || '用户' }}</span>
            </div>
            <el-button text type="danger" @click="handleLogout">退出登录</el-button>
          </div>
        </template>
      </el-drawer>

      <!-- Main Chat Area -->
      <main class="chat-main">
        <div class="messages-area" ref="messagesAreaRef">
          <!-- Welcome / Quick Questions -->
          <QuickQuestions
            v-if="chatStore.messages.length === 0 && !chatStore.isSending"
            :questions="chatStore.quickQuestions"
            @select="handleQuestionSelect"
          />

          <!-- Messages -->
          <div v-else class="messages-list">
            <ChatBubble
              v-for="msg in chatStore.messages"
              :key="msg.id"
              :message="msg"
              @citation-click="handleCitationClick"
            />

            <!-- Typing indicator -->
            <div v-if="chatStore.isSending" class="message-row assistant">
              <el-avatar :size="36" class="avatar assistant">
                <el-icon :size="18"><Monitor /></el-icon>
              </el-avatar>
              <div class="typing-wrapper">
                <div class="typing-indicator">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
                <div class="typing-text">正在思考...</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="input-area">
          <div class="input-container">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="1"
              :autosize="{ minRows: 1, maxRows: 4 }"
              placeholder="输入您的问题..."
              resize="none"
              @keydown="handleKeydown"
              :disabled="chatStore.isSending"
              class="chat-input"
            />
            <el-button
              type="primary"
              :icon="Promotion"
              circle
              :disabled="!inputText.trim() || chatStore.isSending"
              @click="handleSend"
              class="send-btn"
            />
          </div>
          <div class="input-hint">按 Enter 发送，Shift + Enter 换行</div>
        </div>
      </main>
    </div>

    <!-- History Drawer -->
    <HistoryDrawer
      v-model="historyDrawerVisible"
      :conversations="chatStore.conversations"
      :current-id="chatStore.currentConversationId"
      @select="handleLoadConversation"
      @new-chat="handleNewChat"
    />

    <!-- Citation Modal -->
    <CitationModal
      v-model="citationModalVisible"
      :citation="selectedCitation"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, Plus, User, SwitchButton, Operation, Monitor, Promotion, Setting } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { mockGetRecommendations } from '@/mock'
import type { Citation } from '@/types'
import SidePanel from '@/components/SidePanel.vue'
import QuickQuestions from '@/components/QuickQuestions.vue'
import ChatBubble from '@/components/ChatBubble.vue'
import HistoryDrawer from '@/components/HistoryDrawer.vue'
import CitationModal from '@/components/CitationModal.vue'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

const inputText = ref('')
const messagesAreaRef = ref<HTMLElement | null>(null)
const historyDrawerVisible = ref(false)
const sidebarDrawerVisible = ref(false)
const citationModalVisible = ref(false)
const selectedCitation = ref<Citation | null>(null)
const recommendations = ref<string[]>([])

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  await Promise.all([
    chatStore.loadQuickQuestions(),
    chatStore.loadConversations(),
    loadRecommendations()
  ])
})

async function loadRecommendations() {
  if (authStore.user) {
    recommendations.value = await mockGetRecommendations(authStore.user.role)
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesAreaRef.value) {
      messagesAreaRef.value.scrollTop = messagesAreaRef.value.scrollHeight
    }
  })
}

watch(() => chatStore.messages.length, () => {
  scrollToBottom()
})

watch(() => chatStore.isSending, () => {
  scrollToBottom()
})

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || chatStore.isSending) return
  inputText.value = ''
  await chatStore.sendMessage(text)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleQuestionSelect(text: string) {
  inputText.value = text
  handleSend()
}

function handleSidebarQuestionSelect(text: string) {
  sidebarDrawerVisible.value = false
  inputText.value = text
  handleSend()
}

function handleNewChat() {
  chatStore.startNewConversation()
}

async function handleLoadConversation(id: string) {
  await chatStore.loadMessages(id)
  scrollToBottom()
}

function handleCitationClick(citation: Citation) {
  selectedCitation.value = citation
  citationModalVisible.value = true
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--color-background);
}

/* Header */
.chat-header {
  height: 56px;
  background-color: #ffffff;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  z-index: 100;
}

.header-content {
  max-width: 100%;
  height: 100%;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
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
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text);
}

.ai-tag {
  margin-left: 4px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
}

.user-avatar {
  background-color: var(--color-primary);
  color: #ffffff;
}

.user-avatar-small {
  background-color: var(--color-primary);
  color: #ffffff;
}

.user-name {
  font-size: 14px;
  color: var(--color-text-regular);
}

.mobile-menu-btn {
  display: none;
}

/* Body */
.chat-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* Sidebar */
.sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--color-border);
  overflow-y: auto;
}

/* Main Chat */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--color-surface);
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
}

.messages-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px;
}

/* Typing indicator in chat */
.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 0 16px;
}

.message-row.assistant {
  flex-direction: row;
}

.avatar.assistant {
  background-color: #13c2c2;
  color: #ffffff;
  flex-shrink: 0;
}

.typing-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.typing-text {
  font-size: 12px;
  color: var(--color-text-placeholder);
  padding-left: 4px;
}

/* Input Area */
.input-area {
  flex-shrink: 0;
  padding: 12px 16px 16px;
  background-color: #ffffff;
  border-top: 1px solid var(--color-border);
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.chat-input {
  flex: 1;
}

.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
}

.input-hint {
  max-width: 800px;
  margin: 6px auto 0;
  font-size: 12px;
  color: var(--color-text-placeholder);
}

/* Drawer footer */
.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.drawer-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text-regular);
}

/* Responsive */
.mobile-only {
  display: none !important;
}

.desktop-only {
  display: flex;
}

@media (max-width: 768px) {
  .desktop-only {
    display: none !important;
  }

  .mobile-only {
    display: flex !important;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .sidebar {
    display: none;
  }

  .brand-text {
    font-size: 15px;
  }
}
</style>
