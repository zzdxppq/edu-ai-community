import type { User, Message, Conversation, Citation, UserRole } from '@/types'
import {
  mockQuickQuestions,
  mockRoleRecommendations,
  mockQAPairs,
  mockDefaultAnswer,
  mockConversations,
  mockHistoryMessages,
  mockKnowledgeItems,
  mockDocumentFiles,
  mockFeedbackItems
} from './data'

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function generateId(): string {
  return 'id-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9)
}

function findAnswer(message: string): { answer: string; citations: Citation[] } {
  for (const qa of mockQAPairs) {
    if (qa.keywords.some(kw => message.includes(kw))) {
      return { answer: qa.answer, citations: qa.citations }
    }
  }
  return mockDefaultAnswer
}

export async function mockLogin(phone: string, code: string): Promise<{ user: User; accessToken: string; refreshToken: string }> {
  await delay(300)
  if (code !== '123456') {
    throw new Error('验证码错误')
  }
  const stored = localStorage.getItem('edu_registered_user_' + phone)
  if (stored) {
    const user = JSON.parse(stored) as User
    return {
      user,
      accessToken: 'mock-access-token-' + Date.now(),
      refreshToken: 'mock-refresh-token-' + Date.now()
    }
  }
  return {
    user: {
      id: 1,
      username: '演示用户',
      phone,
      role: 'MEMBER_RURAL'
    },
    accessToken: 'mock-access-token-' + Date.now(),
    refreshToken: 'mock-refresh-token-' + Date.now()
  }
}

export async function mockRegister(phone: string, code: string, username: string, role: UserRole): Promise<{ user: User; accessToken: string; refreshToken: string }> {
  await delay(300)
  if (code !== '123456') {
    throw new Error('验证码错误')
  }
  const user: User = {
    id: Date.now(),
    username,
    phone,
    role
  }
  localStorage.setItem('edu_registered_user_' + phone, JSON.stringify(user))
  return {
    user,
    accessToken: 'mock-access-token-' + Date.now(),
    refreshToken: 'mock-refresh-token-' + Date.now()
  }
}

export async function mockSendSms(phone: string): Promise<{ success: boolean }> {
  await delay(100)
  if (!phone || phone.length !== 11) {
    throw new Error('请输入正确的手机号')
  }
  return { success: true }
}

export async function mockChat(message: string, _conversationId?: string): Promise<{ message: Message; conversationId: string }> {
  const chatDelay = 800 + Math.random() * 700
  await delay(chatDelay)
  const { answer, citations } = findAnswer(message)
  const convId = _conversationId || 'conv-' + generateId()
  return {
    message: {
      id: generateId(),
      role: 'assistant',
      content: answer,
      citations,
      createdAt: new Date().toISOString()
    },
    conversationId: convId
  }
}

export async function mockGetConversations(): Promise<Conversation[]> {
  await delay(100)
  return [...mockConversations]
}

export async function mockGetMessages(conversationId: string): Promise<Message[]> {
  await delay(100)
  const history = mockHistoryMessages[conversationId]
  if (!history) return []
  return history.map((msg, idx) => ({
    id: `${conversationId}-msg-${idx}`,
    role: msg.role,
    content: msg.content,
    citations: msg.citations,
    createdAt: new Date(Date.now() - (history.length - idx) * 60000).toISOString()
  }))
}

export async function mockGetQuickQuestions() {
  await delay(100)
  return [...mockQuickQuestions]
}

export async function mockSubmitFeedback(
  _messageId: string,
  _rating: number,
  _reasons: string[],
  _comment: string
): Promise<{ success: boolean }> {
  await delay(300)
  return { success: true }
}

export async function mockEscalate(_messageId: string): Promise<{ success: boolean }> {
  await delay(300)
  return { success: true }
}

export async function mockGetRecommendations(role: UserRole): Promise<string[]> {
  await delay(100)
  return mockRoleRecommendations[role] || mockRoleRecommendations.OTHER
}

export async function mockGetKnowledgeItems() {
  await delay(200)
  return [...mockKnowledgeItems]
}

export async function mockAddKnowledgeItem(data: { title: string; content: string; source: string }) {
  await delay(300)
  return { id: 'ki-' + Date.now(), ...data, status: 'active', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }
}

export async function mockUpdateKnowledgeItem(id: string, data: any) {
  await delay(300)
  return { id, ...data, updatedAt: new Date().toISOString() }
}

export async function mockDeleteKnowledgeItem(_id: string) {
  await delay(200)
}

export async function mockGetDocumentFiles() {
  await delay(200)
  return [...mockDocumentFiles]
}

export async function mockUploadDocument(fileName: string, fileSize: number, fileType: string) {
  await delay(1500)
  return { id: 'doc-' + Date.now(), fileName, fileSize, fileType, status: 'completed', itemCount: Math.floor(Math.random() * 20) + 5, uploadedAt: new Date().toISOString() }
}

export async function mockGetFeedbackItems() {
  await delay(200)
  return [...mockFeedbackItems]
}

export async function mockUpdateFeedbackStatus(_id: string, _status: string) {
  await delay(300)
}
