# Source Tree

## 项目根目录

```
edu-ai-community/
├── frontend/                        # Vue 3 前端
├── backend/                         # Spring Cloud 后端（Maven 多模块）
├── docs/                            # 项目文档（已切片）
│   ├── prd/                         # PRD 切片
│   ├── fullstack-architecture/      # 架构文档切片
│   ├── front-end-spec.md            # 前端UI/UX规范
│   ├── project-proposal.md          # 建设方案
│   └── project-proposal.docx        # 建设方案(Word)
├── demo/                            # 静态Demo页面
├── scripts/                         # 工具脚本
├── .orchestrix-core/                # Orchestrix配置
└── README.md
```

## 后端目录 (backend/)

```
backend/
├── pom.xml                          # 父POM（统一依赖管理）
│
├── edu-common/                      # 公共模块
│   └── src/main/java/com/edu/ai/common/
│       ├── entity/                  # 共享Entity
│       ├── dto/                     # 共享DTO
│       ├── exception/               # BizException + GlobalExceptionHandler
│       ├── response/                # R<T> 统一响应
│       ├── constant/                # 枚举/常量（UserRole等）
│       └── util/                    # 工具类
│
├── edu-gateway/                     # API网关 (:8080)
│   └── src/main/java/com/edu/ai/gateway/
���       ├── GatewayApplication.java
│       ├── filter/                  # JwtAuthGlobalFilter, RateLimitFilter
│       └── config/                  # RouteConfig, CorsConfig
│
├── edu-core/                        # 核心业务服务 (:8081)
│   └── src/main/java/com/edu/ai/core/
│       ├── CoreApplication.java
│       ├── config/                  # SecurityConfig, RedisConfig, JwtConfig
│       ├── auth/                    # 认证: controller/service/entity/repository
│       ├── chat/                    # 对话: controller/service/entity/repository
│       ├── feedback/                # 反馈: controller/service/entity
│       ├── admin/                   # 运营后台: controller/service (config/feedback/recommend)
│       └── feign/                   # AiServiceClient (调用edu-ai)
│
└── edu-ai/                          # AI智能服务 (:8082)
    └── src/main/java/com/edu/ai/intelligence/
        ├── AiApplication.java
        ├── config/                  # AsyncConfig, ModelConfig
        ├── rag/                     # RAG引擎: controller/engine/prompt
        ├── embedding/               # EmbeddingService (Qwen3-Embedding)
        ├── rerank/                  # RerankService (Qwen3-Reranker /v1/score)
        ├── llm/                     # LlmClient (Qwen3-32B /v1/chat/completions)
        ├── cache/                   # SemanticCacheService
        ├── filter/                  # SensitiveWordFilter
        ├── knowledge/               # 知识库CRUD + 检索: controller/service/entity/repository
        ├── document/                # 文档解析: controller/parser/splitter/service
        ├── desensitize/             # 脱敏: controller/ner/rule/service
        ├── platform/                # 平台同步: controller/client/sync
        ��── analytics/               # 日志分析: controller/service
        └── feign/                   # CoreServiceClient (回调edu-core)
```

## 前端目录 (frontend/)

```
frontend/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── src/
    ├── main.ts                      # 入口
    ├── App.vue                      # 根组件
    ├── api/                         # API请求层（按模块拆分）
    │   ├── auth.ts                  # 登录/注册/刷新
    │   ├── chat.ts                  # 对话/消息/SSE
    │   ├── feedback.ts              # 评分/反馈/记录
    │   ├── knowledge.ts             # 知识库管理
    │   └── admin.ts                 # 运营后台
    ├── components/                  # 通用组件
    │   ├── ChatBubble.vue           # 对话气泡
    │   ├── CitationLink.vue         # 引用链接
    │   ├── FeedbackRating.vue       # 评分组件
    │   ├── DocumentPreview.vue      # 文档预览弹窗
    │   └── SidePanel.vue            # 侧边栏
    ├── views/                       # 页面
    │   ├── auth/                    # 登录/注册
    │   ├── chat/                    # 主对话界面
    │   ├── feedback/                # 反馈记录
    │   └── admin/                   # 运营后台（知识/反馈/日志/配置）
    ├── stores/                      # Pinia stores
    │   ├── auth.ts                  # 用户/Token状态
    │   └── chat.ts                  # 对话/会话状态
    ├── router/                      # 路由配置 + 权限守卫
    ├── utils/                       # 工具函数（SSE解析/隐私检测/格式化）
    ├── types/                       # TypeScript类型定义
    └── styles/                      # 全局样式 + CSS变量
```

## 数据库迁移 (Flyway)

```
backend/edu-core/src/main/resources/db/migration/
├── V1__create_users.sql
├── V2__create_conversations_messages.sql
├── V3__create_feedback.sql
├── V4__create_system_configs.sql
├── V5__create_recommended_questions.sql
└── V6__create_audit_logs.sql

backend/edu-ai/src/main/resources/db/migration/
├── V1__create_documents.sql
├── V2__create_knowledge_chunks.sql
├── V3__create_desensitize_mappings.sql
├── V4__create_platform_sync_logs.sql
└── V5__create_indexes.sql
```
