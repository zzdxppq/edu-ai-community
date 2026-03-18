export type UserRole = 'REGIONAL_ADMIN' | 'LEAD_SCHOOL' | 'MEMBER_URBAN' | 'MEMBER_RURAL' | 'RESEARCHER' | 'OTHER'

export interface User {
  id: number
  username: string
  phone: string
  role: UserRole
}

export interface Citation {
  index: number
  source: string
  section: string
  page: number
}

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations?: Citation[]
  createdAt: string
}

export interface Conversation {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface Feedback {
  id: string
  messageId: string
  rating: number
  reasons: string[]
  comment: string
  escalated: boolean
}

export interface QuickQuestion {
  id: number
  text: string
}

export const ROLE_LABELS: Record<UserRole, string> = {
  REGIONAL_ADMIN: '区域教育管理人员',
  LEAD_SCHOOL: '校共体核心牵头校人员',
  MEMBER_URBAN: '校共体成员校人员（城镇）',
  MEMBER_RURAL: '校共体成员校人员（乡村）',
  RESEARCHER: '研究人员',
  OTHER: '其他人员'
}

export const ROLE_DESCRIPTIONS: Record<UserRole, string> = {
  REGIONAL_ADMIN: '负责区域教育统筹管理与政策落实',
  LEAD_SCHOOL: '作为核心校统筹校共体各项工作',
  MEMBER_URBAN: '城镇学校参与校共体协同发展',
  MEMBER_RURAL: '乡村学校借助校共体提升教育质量',
  RESEARCHER: '从事教育政策研究与学术分析',
  OTHER: '关注校共体建设的其他人员'
}

export interface KnowledgeItem {
  id: string
  title: string
  content: string
  source: 'file' | 'qa' | 'manual'
  sourceFile?: string
  status: 'active' | 'pending' | 'inactive'
  createdAt: string
  updatedAt: string
}

export interface FeedbackItem {
  id: string
  messageId: string
  userId: number
  username: string
  userRole: UserRole
  rating: number
  reasons: string[]
  comment: string
  escalated: boolean
  escalateStatus: 'pending' | 'processing' | 'completed' | null
  question: string
  answer: string
  conversationId: string
  createdAt: string
}

export interface DocumentFile {
  id: string
  fileName: string
  fileSize: number
  fileType: 'pdf' | 'docx'
  status: 'parsing' | 'completed' | 'failed'
  itemCount: number
  uploadedAt: string
}
