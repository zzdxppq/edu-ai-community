import type { QuickQuestion, Citation, UserRole, KnowledgeItem, FeedbackItem, DocumentFile } from '@/types'

export const mockQuickQuestions: QuickQuestion[] = [
  { id: 1, text: '校共体的基本组织架构是什么？' },
  { id: 2, text: '如何开展城乡学校结对帮扶？' },
  { id: 3, text: '校共体教师交流轮岗政策有哪些？' },
  { id: 4, text: '如何评估校共体建设成效？' },
  { id: 5, text: '校共体数字化教学资源如何共享？' },
  { id: 6, text: '校共体经费管理有什么规定？' }
]

export const mockRoleRecommendations: Record<UserRole, string[]> = {
  REGIONAL_ADMIN: [
    '区域校共体建设的推进策略有哪些？',
    '如何制定校共体考核评估方案？',
    '区域教育资源统筹分配机制？',
    '校共体建设中的常见问题及对策？',
    '如何推动校共体教师交流轮岗？'
  ],
  LEAD_SCHOOL: [
    '牵头校如何发挥引领示范作用？',
    '校共体联合教研活动如何组织？',
    '如何建立校共体内部沟通机制？',
    '牵头校的管理职责有哪些？',
    '校共体课程资源共建共享方案？'
  ],
  MEMBER_URBAN: [
    '城镇学校如何参与校共体协同发展？',
    '校共体中城镇学校的优势资源共享？',
    '如何与乡村学校开展结对帮扶？',
    '城镇学校教师交流轮岗注意事项？',
    '校共体活动中的城镇学校角色定位？'
  ],
  MEMBER_RURAL: [
    '乡村学校如何利用校共体资源提升质量？',
    '校共体对乡村教师培训有哪些支持？',
    '乡村学校如何参与联合教研？',
    '数字化教学资源在乡村学校的应用？',
    '校共体建设中乡村学校的需求表达机制？'
  ],
  RESEARCHER: [
    '校共体建设的理论基础有哪些？',
    '城乡教育一体化发展的政策演变？',
    '校共体建设成效的评估指标体系？',
    '国内校共体建设的典型案例分析？',
    '校共体建设中的公平性问题研究？'
  ],
  OTHER: [
    '什么是城乡学校共同体？',
    '校共体建设的主要目标是什么？',
    '校共体有哪些主要工作内容？',
    '如何参与校共体建设？',
    '校共体建设的最新政策文件有哪些？'
  ]
}

interface QAPair {
  keywords: string[]
  answer: string
  citations: Citation[]
}

export const mockQAPairs: QAPair[] = [
  {
    keywords: ['组织架构', '架构', '组织'],
    answer: '校共体的基本组织架构主要包括以下几个层面：\n\n**一、核心架构**\n校共体采用"1+1+N"的基本组织模式，即1所城镇优质学校（牵头校）+1所城镇学校+N所乡村学校共同组建[1]。\n\n**二、管理层级**\n1. **决策层**：由校共体理事会负责，由各成员校校长组成，负责重大事项决策\n2. **执行层**：设立校共体办公室，负责日常管理和协调工作\n3. **业务层**：按学科或业务领域设立若干工作组，负责具体业务推进[2]\n\n**三、运行机制**\n校共体实行"统一管理、资源共享、师资互派、教学同步"的运行机制，确保各成员校在教育教学、教师发展、学校管理等方面实现深度融合[3]。',
    citations: [
      { index: 1, source: '《河南省县域结对型城乡学校共同体建设指南》', section: '第二章 组织架构', page: 12 },
      { index: 2, source: '《城乡义务教育一体化发展实施方案》', section: '第三章 管理体制', page: 28 },
      { index: 3, source: '《河南省教育厅关于推进校共体建设的通知》', section: '附件一 实施细则', page: 5 }
    ]
  },
  {
    keywords: ['结对', '帮扶', '结对帮扶'],
    answer: '城乡学校结对帮扶是校共体建设的核心内容，主要包括以下模式和措施：\n\n**一、结对模式**\n目前主要采用三种结对模式：\n1. **"1+1"模式**：一所城镇优质校与一所乡村学校结对\n2. **"1+1+N"模式**：一所城镇优质校联合一所城镇校帮扶N所乡村校\n3. **"1+N"模式**：一所优质校辐射带动多所薄弱校[1]\n\n**二、帮扶内容**\n结对帮扶重点涵盖以下领域：\n- **教学帮扶**：开展同课异构、联合教研、集体备课等活动\n- **管理帮扶**：输出先进管理理念和制度\n- **师资帮扶**：教师交流轮岗、跟岗学习、名师工作室共建\n- **资源帮扶**：数字教育资源共享、图书设备共用[2]\n\n**三、保障机制**\n县级教育行政部门负责统筹规划结对方案，建立考核评价机制，将帮扶成效纳入学校年度考核[3]。',
    citations: [
      { index: 1, source: '《河南省县域结对型城乡学校共同体建设指南》', section: '第三章 结对模式', page: 18 },
      { index: 2, source: '《城乡义务教育一体化发展实施方案》', section: '第二章 帮扶内容', page: 15 },
      { index: 3, source: '《河南省教育厅关于加强城乡学校结对帮扶的意见》', section: '第四章 保障措施', page: 8 }
    ]
  },
  {
    keywords: ['教师', '交流', '轮岗'],
    answer: '校共体教师交流轮岗是促进城乡师资均衡配置的重要举措，相关政策主要包括：\n\n**一、交流轮岗范围**\n校共体内各成员校之间的教师交流轮岗，重点推动城镇优质学校骨干教师到乡村学校任教，乡村学校青年教师到城镇学校跟岗学习[1]。\n\n**二、具体政策要求**\n1. **比例要求**：每年交流轮岗教师比例不低于符合条件教师总数的10%，其中骨干教师不低于交流总数的20%\n2. **期限规定**：交流轮岗期限一般为1-3年\n3. **待遇保障**：交流教师享受乡村教师补贴、交通补助等相关待遇\n4. **职称倾斜**：有农村学校任教经历的教师在职称评审中给予适当倾斜[2]\n\n**三、组织实施**\n各县（市、区）教育行政部门负责制定年度交流轮岗计划，建立教师交流轮岗档案，定期开展跟踪评估[3]。',
    citations: [
      { index: 1, source: '《河南省教师交流轮岗实施办法》', section: '第二章 交流范围', page: 6 },
      { index: 2, source: '《关于推进县域内义务教育学校校长教师交流轮岗的意见》', section: '第三章 政策保障', page: 12 },
      { index: 3, source: '《河南省县域结对型城乡学校共同体建设指南》', section: '第五章 师资交流', page: 35 }
    ]
  },
  {
    keywords: ['评估', '成效', '考核'],
    answer: '校共体建设成效评估是保障建设质量的关键环节，主要评估框架包括：\n\n**一、评估维度**\n1. **组织管理**（20分）：组织架构完善度、制度建设情况、运行机制有效性\n2. **教育教学**（30分）：联合教研活动开展、课程资源共建、教学质量提升\n3. **师资发展**（20分）：教师交流轮岗、培训研修、教师能力提升\n4. **资源共享**（15分）：数字化资源建设、设施设备共用、图书资料共享\n5. **发展成效**（15分）：学生素质提升、办学水平提高、社会满意度[1]\n\n**二、评估方式**\n采用"自评+县评+省抽查"三级评估方式，每年进行一次综合评估。评估结果分为优秀、良好、合格、不合格四个等次[2]。\n\n**三、结果运用**\n评估结果作为校共体成员校年度考核、评优评先、资金分配的重要依据。对评估不合格的校共体，限期整改[3]。',
    citations: [
      { index: 1, source: '《河南省校共体建设评估指标体系》', section: '第二章 评估维度', page: 8 },
      { index: 2, source: '《城乡学校共同体建设质量监测方案》', section: '第三章 评估方式', page: 15 },
      { index: 3, source: '《河南省教育厅关于加强校共体建设考核的通知》', section: '第四章 结果运用', page: 6 }
    ]
  },
  {
    keywords: ['数字化', '资源', '共享', '数字'],
    answer: '校共体数字化教学资源共享是推动城乡教育均衡发展的重要手段，主要措施包括：\n\n**一、平台建设**\n依托河南省基础教育资源公共服务平台，建设校共体专属资源空间，实现"一校上传、多校共享"的资源共建共享机制[1]。\n\n**二、资源类型**\n1. **课程资源**：优质课件、教学设计、微课视频等\n2. **教研资源**：集体备课记录、教研活动视频、教学反思等\n3. **评价资源**：试题库、评价量表、学业质量分析等\n4. **培训资源**：教师培训课程、专家讲座、名师示范课等[2]\n\n**三、技术支撑**\n推动"三个课堂"建设，通过专递课堂、名师课堂、名校网络课堂，实现优质教育资源的实时共享。鼓励有条件的校共体建设同步课堂，实现城乡学校同上一堂课[3]。',
    citations: [
      { index: 1, source: '《河南省教育信息化2.0行动计划》', section: '第三章 资源建设', page: 22 },
      { index: 2, source: '《校共体数字化资源建设与共享指南》', section: '第二章 资源分类', page: 10 },
      { index: 3, source: '《关于加强"三个课堂"应用的指导意见》', section: '第二章 建设要求', page: 8 }
    ]
  },
  {
    keywords: ['经费', '资金', '管理'],
    answer: '校共体经费管理是确保建设规范运行的重要保障，主要规定包括：\n\n**一、经费来源**\n1. **财政拨款**：县级以上财政安排专项经费支持校共体建设\n2. **项目资金**：国家和省级教育改革项目配套资金\n3. **学校自筹**：各成员校从公用经费中安排一定比例用于校共体活动\n4. **社会捐赠**：鼓励社会力量支持校共体建设[1]\n\n**二、使用范围**\n校共体经费主要用于：\n- 教师交流轮岗交通、食宿补贴\n- 联合教研、集体备课等活动经费\n- 数字化资源建设和信息化设备购置\n- 教师培训研修费用\n- 校共体管理运行费用[2]\n\n**三、管理要求**\n1. 实行专账管理，确保专款专用\n2. 建立经费使用审批制度，重大支出须经理事会审议\n3. 定期公开经费使用情况，接受审计监督\n4. 严禁截留、挪用校共体专项经费[3]。',
    citations: [
      { index: 1, source: '《河南省校共体建设专项资金管理办法》', section: '第二章 经费来源', page: 4 },
      { index: 2, source: '《城乡学校共同体经费使用指南》', section: '第三章 使用范围', page: 8 },
      { index: 3, source: '《河南省教育经费管理暂行规定》', section: '第五章 监督管理', page: 18 }
    ]
  }
]

export const mockDefaultAnswer = {
  answer: '感谢您的提问！根据河南省城乡学校共同体建设相关政策文件，校共体建设的核心目标是促进城乡义务教育优质均衡发展。\n\n校共体通过"城乡结对、资源共享、师资互派、教学协同"等方式，推动城镇优质教育资源向乡村学校辐射延伸[1]。\n\n如果您想了解更具体的信息，建议您可以尝试以下问题：\n- 校共体的组织架构\n- 教师交流轮岗政策\n- 结对帮扶模式\n- 建设成效评估\n\n我会尽力为您提供详细的政策解读和实践指导[2]。',
  citations: [
    { index: 1, source: '《河南省县域结对型城乡学校共同体建设指南》', section: '第一章 总则', page: 3 },
    { index: 2, source: '《城乡义务教育一体化发展实施方案》', section: '第一章 指导思想', page: 2 }
  ] as Citation[]
}

export const mockConversations = [
  {
    id: 'conv-1',
    title: '关于校共体组织架构的咨询',
    createdAt: '2026-03-15T10:30:00Z',
    updatedAt: '2026-03-15T10:35:00Z'
  },
  {
    id: 'conv-2',
    title: '教师交流轮岗政策解读',
    createdAt: '2026-03-14T14:20:00Z',
    updatedAt: '2026-03-14T14:28:00Z'
  },
  {
    id: 'conv-3',
    title: '数字化资源共享方案咨询',
    createdAt: '2026-03-12T09:15:00Z',
    updatedAt: '2026-03-12T09:22:00Z'
  }
]

export const mockHistoryMessages: Record<string, Array<{ role: 'user' | 'assistant'; content: string; citations?: Citation[] }>> = {
  'conv-1': [
    { role: 'user', content: '校共体的基本组织架构是什么？' },
    {
      role: 'assistant',
      content: mockQAPairs[0].answer,
      citations: mockQAPairs[0].citations
    }
  ],
  'conv-2': [
    { role: 'user', content: '校共体教师交流轮岗政策有哪些？' },
    {
      role: 'assistant',
      content: mockQAPairs[2].answer,
      citations: mockQAPairs[2].citations
    }
  ],
  'conv-3': [
    { role: 'user', content: '校共体数字化教学资源如何共享？' },
    {
      role: 'assistant',
      content: mockQAPairs[4].answer,
      citations: mockQAPairs[4].citations
    }
  ]
}

export const mockPolicyNews = [
  { id: 1, title: '河南省印发校共体建设三年行动计划', date: '2026-03-10' },
  { id: 2, title: '教育部：推动城乡义务教育一体化发展', date: '2026-03-05' },
  { id: 3, title: '全省校共体建设推进会在郑州召开', date: '2026-02-28' }
]

export const mockCases = [
  { id: 1, title: '洛阳市"1+1+N"校共体建设典型经验' },
  { id: 2, title: '信阳市乡村学校数字化教学资源应用案例' }
]

export const mockKnowledgeItems: KnowledgeItem[] = [
  { id: 'ki-001', title: '校共体基本组织架构说明', content: '校共体采用"1+1+N"的基本组织模式，即1所城镇优质学校（牵头校）+1所城镇学校+N所乡村学校共同组建。管理层级包括决策层（理事会）、执行层（办公室）、业务层（工作组）。', source: 'file', sourceFile: '河南省校共体建设指南.pdf', status: 'active', createdAt: '2026-02-10T08:00:00Z', updatedAt: '2026-03-01T10:00:00Z' },
  { id: 'ki-002', title: '城乡结对帮扶三种模式', content: '主要采用三种结对模式：1+1模式（一对一）、1+1+N模式（联合帮扶）、1+N模式（一校辐射多校）。帮扶内容涵盖教学、管理、师资、资源四个方面。', source: 'file', sourceFile: '河南省校共体建设指南.pdf', status: 'active', createdAt: '2026-02-10T08:30:00Z', updatedAt: '2026-03-01T10:30:00Z' },
  { id: 'ki-003', title: '教师交流轮岗比例与期限要求', content: '每年交流轮岗教师比例不低于符合条件教师总数的10%，其中骨干教师不低于交流总数的20%。交流轮岗期限一般为1-3年。', source: 'file', sourceFile: '教师交流轮岗实施办法.docx', status: 'active', createdAt: '2026-02-12T09:00:00Z', updatedAt: '2026-02-28T14:00:00Z' },
  { id: 'ki-004', title: '校共体建设评估五维度指标', content: '评估维度包括：组织管理（20分）、教育教学（30分）、师资发展（20分）、资源共享（15分）、发展成效（15分）。采用自评+县评+省抽查三级评估方式。', source: 'manual', status: 'active', createdAt: '2026-02-15T11:00:00Z', updatedAt: '2026-03-05T09:00:00Z' },
  { id: 'ki-005', title: '数字化资源共享"三个课堂"建设', content: '通过专递课堂、名师课堂、名校网络课堂实现优质教育资源实时共享。鼓励有条件的校共体建设同步课堂，实现城乡学校同上一堂课。', source: 'qa', status: 'active', createdAt: '2026-02-18T14:00:00Z', updatedAt: '2026-03-08T16:00:00Z' },
  { id: 'ki-006', title: '校共体经费使用范围与管理', content: '经费主要用于教师交流轮岗补贴、联合教研活动经费、数字化资源建设、教师培训研修费用、管理运行费用等。实行专账管理，专款专用。', source: 'file', sourceFile: '校共体专项资金管理办法.pdf', status: 'active', createdAt: '2026-02-20T10:00:00Z', updatedAt: '2026-03-10T11:00:00Z' },
  { id: 'ki-007', title: '乡村学校教师培训支持政策', content: '校共体为乡村教师提供跟岗学习、名师工作室共建、集体备课、线上研修等多种培训方式，促进乡村教师专业成长。', source: 'qa', status: 'pending', createdAt: '2026-03-01T09:00:00Z', updatedAt: '2026-03-12T10:00:00Z' },
  { id: 'ki-008', title: '牵头校引领示范职责', content: '牵头校负责统筹校共体年度工作计划、组织联合教研活动、输出管理经验、协调资源分配。需发挥示范引领作用，带动成员校共同发展。', source: 'manual', status: 'pending', createdAt: '2026-03-05T08:00:00Z', updatedAt: '2026-03-14T09:00:00Z' },
  { id: 'ki-009', title: '校共体课程资源共建方案', content: '各成员校按学科分工共建课程资源库，包括优质课件、教学设计、微课视频、试题库等。依托省级资源平台实现"一校上传、多校共享"。', source: 'file', sourceFile: '课程资源共建方案.docx', status: 'inactive', createdAt: '2026-01-20T10:00:00Z', updatedAt: '2026-02-15T14:00:00Z' },
  { id: 'ki-010', title: '校共体内部沟通协调机制', content: '建立定期会议制度（月度例会、学期总结会）、信息通报制度、问题反馈机制。利用微信群、钉钉等工具保持日常沟通，确保信息畅通。', source: 'manual', status: 'active', createdAt: '2026-03-08T11:00:00Z', updatedAt: '2026-03-15T16:00:00Z' }
]

export const mockFeedbackItems: FeedbackItem[] = [
  { id: 'fb-001', messageId: 'msg-101', userId: 1, username: '张老师', userRole: 'LEAD_SCHOOL', rating: 5, reasons: ['内容准确'], comment: '回答非常详细，对校共体组织架构的描述很清晰。', escalated: false, escalateStatus: null, question: '校共体的基本组织架构是什么？', answer: '校共体采用"1+1+N"的基本组织模式...', conversationId: 'conv-1', createdAt: '2026-03-15T10:30:00Z' },
  { id: 'fb-002', messageId: 'msg-102', userId: 2, username: '李主任', userRole: 'REGIONAL_ADMIN', rating: 4, reasons: ['内容准确', '引用可靠'], comment: '信息比较全面，但希望能提供更多地方案例。', escalated: false, escalateStatus: null, question: '区域校共体建设的推进策略有哪些？', answer: '区域校共体建设需要从顶层设计、资源配置、考核评价等方面系统推进...', conversationId: 'conv-2', createdAt: '2026-03-14T14:20:00Z' },
  { id: 'fb-003', messageId: 'msg-103', userId: 3, username: '王校长', userRole: 'MEMBER_RURAL', rating: 2, reasons: ['内容不准确', '缺少引用'], comment: '关于乡村学校经费补助的回答与实际政策不符，建议核实后更新。', escalated: true, escalateStatus: 'pending', question: '乡村学校参与校共体能获得哪些经费支持？', answer: '乡村学校可以获得交通补贴、培训经费等支持...', conversationId: 'conv-3', createdAt: '2026-03-13T09:15:00Z' },
  { id: 'fb-004', messageId: 'msg-104', userId: 4, username: '赵研究员', userRole: 'RESEARCHER', rating: 3, reasons: ['不够详细'], comment: '对校共体建设理论基础的阐述不够深入，缺少学术文献支撑。', escalated: false, escalateStatus: null, question: '校共体建设的理论基础有哪些？', answer: '校共体建设主要基于教育公平理论、协同发展理论等...', conversationId: 'conv-4', createdAt: '2026-03-12T16:45:00Z' },
  { id: 'fb-005', messageId: 'msg-105', userId: 5, username: '刘老师', userRole: 'MEMBER_URBAN', rating: 1, reasons: ['答非所问', '内容不准确'], comment: '我问的是城镇学校如何参与结对帮扶，回答的却是乡村学校的内容，完全不对。', escalated: true, escalateStatus: 'processing', question: '城镇学校如何参与校共体结对帮扶？', answer: '城镇学校在校共体中主要承担资源输出的角色...', conversationId: 'conv-5', createdAt: '2026-03-11T11:00:00Z' },
  { id: 'fb-006', messageId: 'msg-106', userId: 6, username: '陈副校长', userRole: 'LEAD_SCHOOL', rating: 5, reasons: ['内容准确', '引用可靠', '很有帮助'], comment: '关于联合教研活动的组织方式解释得非常好，已经在学校推广使用了。', escalated: false, escalateStatus: null, question: '校共体联合教研活动如何组织？', answer: '联合教研活动可通过同课异构、集体备课、主题研讨等方式开展...', conversationId: 'conv-6', createdAt: '2026-03-10T13:30:00Z' },
  { id: 'fb-007', messageId: 'msg-107', userId: 7, username: '孙主任', userRole: 'REGIONAL_ADMIN', rating: 2, reasons: ['信息过时'], comment: '引用的文件已经更新了新版本，建议及时更新知识库。', escalated: true, escalateStatus: 'completed', question: '校共体考核评估方案如何制定？', answer: '根据2024年版评估方案，考核维度包括...', conversationId: 'conv-7', createdAt: '2026-03-09T08:20:00Z' },
  { id: 'fb-008', messageId: 'msg-108', userId: 8, username: '周老师', userRole: 'MEMBER_RURAL', rating: 4, reasons: ['很有帮助'], comment: '对数字化资源使用的说明很实用，帮助我们学校快速上手了在线教学平台。', escalated: false, escalateStatus: null, question: '数字化教学资源在乡村学校如何应用？', answer: '乡村学校可通过省级资源平台获取优质课件、微课视频等资源...', conversationId: 'conv-8', createdAt: '2026-03-08T15:10:00Z' }
]

export const mockDocumentFiles: DocumentFile[] = [
  { id: 'doc-001', fileName: '河南省校共体建设指南.pdf', fileSize: 2458624, fileType: 'pdf', status: 'completed', itemCount: 15, uploadedAt: '2026-02-10T08:00:00Z' },
  { id: 'doc-002', fileName: '教师交流轮岗实施办法.docx', fileSize: 1245184, fileType: 'docx', status: 'completed', itemCount: 8, uploadedAt: '2026-02-12T09:00:00Z' },
  { id: 'doc-003', fileName: '校共体专项资金管理办法.pdf', fileSize: 1867776, fileType: 'pdf', status: 'completed', itemCount: 12, uploadedAt: '2026-02-20T10:00:00Z' },
  { id: 'doc-004', fileName: '课程资源共建方案.docx', fileSize: 983040, fileType: 'docx', status: 'completed', itemCount: 6, uploadedAt: '2026-01-20T10:00:00Z' },
  { id: 'doc-005', fileName: '2026年校共体工作计划.pdf', fileSize: 3145728, fileType: 'pdf', status: 'parsing', itemCount: 0, uploadedAt: '2026-03-16T14:00:00Z' }
]
