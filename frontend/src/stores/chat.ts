import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Message, Conversation, QuickQuestion } from '@/types'
import {
  mockChat,
  mockGetConversations,
  mockGetMessages,
  mockGetQuickQuestions
} from '@/mock'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<string | null>(null)
  const messages = ref<Message[]>([])
  const isSending = ref(false)
  const quickQuestions = ref<QuickQuestion[]>([])

  const currentConversation = computed(() =>
    conversations.value.find(c => c.id === currentConversationId.value)
  )

  async function loadConversations() {
    conversations.value = await mockGetConversations()
  }

  async function loadMessages(conversationId: string) {
    currentConversationId.value = conversationId
    messages.value = await mockGetMessages(conversationId)
  }

  async function sendMessage(content: string) {
    // Add user message immediately
    const userMessage: Message = {
      id: 'msg-' + Date.now(),
      role: 'user',
      content,
      createdAt: new Date().toISOString()
    }
    messages.value.push(userMessage)
    isSending.value = true

    try {
      const result = await mockChat(content, currentConversationId.value ?? undefined)
      if (!currentConversationId.value) {
        currentConversationId.value = result.conversationId
        // Add to conversations list
        const newConv: Conversation = {
          id: result.conversationId,
          title: content.substring(0, 30) + (content.length > 30 ? '...' : ''),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
        conversations.value.unshift(newConv)
      }
      messages.value.push(result.message)
    } finally {
      isSending.value = false
    }
  }

  function startNewConversation() {
    currentConversationId.value = null
    messages.value = []
  }

  async function loadQuickQuestions() {
    quickQuestions.value = await mockGetQuickQuestions()
  }

  return {
    conversations,
    currentConversationId,
    currentConversation,
    messages,
    isSending,
    quickQuestions,
    loadConversations,
    loadMessages,
    sendMessage,
    startNewConversation,
    loadQuickQuestions
  }
})
