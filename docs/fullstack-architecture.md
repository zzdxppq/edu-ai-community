# 校共体AI助手 全栈架构文档

## 变更日志

| 日期 | 版本 | 描述 | 作者 |
|------|------|------|------|
| 2026-04-08 | v1.0 | 初始架构文档，覆盖系统总览、服务拆分、RAG引擎、文档解析管道、脱敏管道、模型集成、数据库Schema、Redis设计、API规范、部署方案 | 鲁班（架构师） |
| 2026-04-21 | v1.1 | 模型对接实测适配：Qwen3-32B上下文5500→检索Top-3、对话3轮、Prompt精简；更新真实模型地址/ID/协议 | 鲁班（架构师） |
| 2026-04-22 | v1.2 | Qwen3-Embedding-8B恢复可用，恢复混合检索（向量+关键词+Rerank）、pgvector、语义缓存；保留5500 token适配 | 鲁班（架构师） |

---

## ⚠️ 关键架构约束（2026-04-22 更新）

| 约束 | 影响 | 适配策略 |
|------|------|---------|
| **Qwen3-32B max_model_len = 5500** | 上下文极其有限 | 检索Top-3, 对话3轮, Prompt精简至400tok, 生成≤1600tok |
| **Embedding向量维度待确认** | 影响pgvector字段定义 | 服务重启后需测试确认，暂用占位符 |
| **无需API Key** | 简化认证 | 内网vLLM直连，无鉴权头 |
| **所有模型同一IP不同端口** | 网络简单 | 10.91.28.4:22214/22237/22257/22205 |

---

## 1. 系统总览

### 1.1 架构风格

前后端分离 + Spring Cloud 微服务 + 智能中台，Monorepo 管理。后端按业务拆分为 2 个微服务：**核心业务服务（edu-core）** 和 **AI智能服务（edu-ai）**，通过 Spring Cloud OpenFeign 进行服务间调用，Spring Cloud Gateway 统一路由。前端 Vue 3 SPA 嵌入校共体网站。

```
┌──────────────────────────────────────────────────────────────────┐
│                      校共体网站 (iframe/嵌入)                      │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                Vue 3 + Element Plus SPA                    │  │
│  │   用户端（对话/反馈/历史） │ 运营后台（管理/审核/分析）        │  │
│  └───────────────────────────┬────────────────────────────────┘  │
└──────────────────────────────┼───────────────────────────────────┘
                               │ HTTPS
                               ▼
                 ┌──────────────────────────┐
                 │  Spring Cloud Gateway    │
                 │  统一路由 / 鉴权 / 限流   │
                 │  /api/core/** → edu-core │
                 │  /api/ai/**   → edu-ai   │
                 └─────────┬───────┬────────┘
                           │       │
              ┌────────────┘       └────────────┐
              ▼                                  ▼
┌──────────────────────────┐    ┌──────────────────────────────┐
│  edu-core (核心业务服务)   │    │  edu-ai (AI智能服务)          │
│  :8081                    │    │  :8082                        │
│                           │    │                               │
│  ┌────────┐ ┌──────────┐ │    │  ┌──────────┐ ┌───────────┐ │
│  │Auth/JWT│ │Chat/对话  │ │    │  │RAG Engine│ │Doc Parser │ │
│  └────────┘ └──────────┘ │    │  └──────────┘ └───────────┘ │
│  ┌────────┐ ┌──────────┐ │    │  ┌──────────┐ ┌───────────┐ │
│  │Feedback│ │Admin/运营 │ │    │  │Embedding │ │Rerank     │ │
│  └────────┘ └──────────┘ │    │  └──────────┘ └───────────┘ │
│  ┌────────┐ ┌──────────┐ │    │  ┌──────────┐ ┌───────────┐ │
│  │Config  │ │Knowledge │ │    │  │Desensit. │ │LLM Client │ │
│  │管理    │ │CRUD管理  │ │    │  └──────────┘ └───────────┘ │
│  └────────┘ └──────────┘ │    │  ┌──────────┐ ┌───────────┐ │
│                           │    │  │Sem.Cache │ │Sensitive  │ │
│    ──OpenFeign──→         │    │  │语义缓存  │ │Filter     │ │
│                           │    │  └──────────┘ └───────────┘ │
│                           │    │  ┌──────────────────────┐   │
│                           │    │  │Platform Sync 平台同步 │   │
│                           │    │  └──────────────────────┘   │
└──────┬──────────┬─────────┘    └──────┬──────────┬───────────┘
       │          │                     │          │
       ▼          ▼                     ▼          ▼
┌──────────┐ ┌────────┐          ┌──────────────────────────┐
│PostgreSQL│ │Redis 7 │          │    模型API (Qwen3系列)    │
│+pgvector │ │        │          │ 32B/VL-235B/Emb-8B/Re-8B│
└──────────┘ └────────┘          └──────────────────────────┘
   (共享DB，按Schema隔离)          (仅edu-ai调用)
```

### 1.2 微服务拆分策略

| 服务 | 端口 | 职责 | 拆分依据 |
|------|------|------|---------|
| **edu-core** | 8081 | 用户认证、对话管理、反馈处理、运营后台CRUD、配置管理 | 面向用户/运营的业务逻辑，I/O密集型 |
| **edu-ai** | 8082 | RAG引擎、文档解析、向量化、重排序、脱敏管道、语义缓存、敏感词过滤、平台数据同步 | AI/模型相关处理，CPU/计算密集型 |
| **gateway** | 8080 | 统一入口、路由分发、JWT鉴权、限流 | 网关层 |

**服务间通信：**

- edu-core → edu-ai：通过 **OpenFeign** 同步调用（如发送消息时调用RAG引擎）
- edu-ai → edu-core：通过 **OpenFeign** 回调（如文档解析完成通知、知识条目更新通知）
- 共享数据库：两个服务共享同一个 PostgreSQL 实例，按逻辑Schema隔离读写范围
- 共享 Redis：两个服务共享同一个 Redis 实例，按Key前缀隔离

**Gateway 路由规则：**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: core-service
          uri: lb://edu-core
          predicates:
            - Path=/api/auth/**, /api/conversations/**, /api/feedback/**,
              /api/admin/feedback/**, /api/admin/config/**,
              /api/admin/recommended-questions/**, /api/recommended-questions/**,
              /api/quick-questions/**
        - id: ai-service
          uri: lb://edu-ai
          predicates:
            - Path=/api/chat/**, /api/search/**, /api/documents/**,
              /api/admin/documents/**, /api/admin/knowledge/**,
              /api/admin/cases/**, /api/admin/platform/**,
              /api/admin/qa/**, /api/admin/conversations/**,
              /api/admin/analytics/**
```

### 1.3 技术栈确认

| 层 | 技术选型 | 版本 |
|----|---------|------|
| 前端 | Vue 3 + Element Plus + Pinia + Vue Router + Axios | Vue 3.4+ |
| 后端框架 | JDK 21 + Spring Boot 3.x + Spring Cloud 2023.x | Spring Boot 3.2+ |
| 微服务 | Spring Cloud Gateway, Spring Cloud OpenFeign, Spring Cloud LoadBalancer | — |
| 服务注册 | Nacos / Consul（可选，初期可用静态地址直连） | — |
| ORM | Spring Data JPA (Hibernate 6) | — |
| 数据库 | PostgreSQL 16 + pgvector + zhparser | — |
| 缓存 | Redis 7 (Spring Data Redis) | — |
| 文档解析 | Apache PDFBox 3.x, Apache POI 5.x | — |
| 模型API | Qwen3系列（OpenAI兼容接口） | — |
| 认证 | JWT (access_token + refresh_token)，Gateway统一鉴权 | — |
| 构建工具 | Maven 多模块 (后端), Vite (前端) | — |
| 数据库迁移 | Flyway | — |

### 1.4 Monorepo 目录结构

```
edu-ai-community/
├── frontend/                        # Vue 3 前端
│   ├── src/
│   │   ├── api/                     # API 请求层
│   │   ├── components/              # 通用组件
│   │   ├── views/
│   │   │   ├── chat/                # 对话相关页面
│   │   │   ├── auth/                # 登录/注册
│   │   │   ├── feedback/            # 反馈记录
│   │   │   └── admin/               # 运营后台
│   │   ├── stores/                  # Pinia状态管理
│   │   ├── router/                  # 路由配置
│   │   ├── utils/                   # 工具函数
│   │   └── styles/                  # 全局样式
│   ├── package.json
│   └── vite.config.ts
│
├── backend/                         # Spring Cloud 后端 (Maven多模块)
│   ├── pom.xml                      # 父POM，统一依赖管理
│   │
│   ├── edu-common/                  # 公共模块（被其他模块依赖）
│   │   ├── src/main/java/com/edu/ai/common/
│   │   │   ├── entity/              # 共享实体类
│   │   │   ├── dto/                 # 共享DTO
│   │   │   ├── exception/           # 全局异常定义
│   │   │   ├── response/            # 统一响应封装 R<T>
│   │   │   ├── constant/            # 常量/枚举
│   │   │   └── util/                # 工具类
│   │   └── pom.xml
│   │
│   ├── edu-gateway/                 # Spring Cloud Gateway 网关
│   │   ├── src/main/java/com/edu/ai/gateway/
│   │   │   ├── GatewayApplication.java
│   │   │   ├── filter/              # JWT鉴权过滤器、限流过滤器
│   │   │   └── config/              # 路由配置
│   │   ├── src/main/resources/
│   │   │   └── application.yml      # 路由规则、限流策略
│   │   └── pom.xml
│   │
│   ├── edu-core/                    # 核心业务服务 (:8081)
│   │   ├── src/main/java/com/edu/ai/core/
│   │   │   ├── CoreApplication.java
│   │   │   ├── config/              # SecurityConfig, RedisConfig, JwtConfig
│   │   │   ├── auth/                # 用户认证模块
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── entity/
│   │   │   │   └── repository/
│   │   │   ├── chat/                # 对话管理模块（会话/消息CRUD）
│   │   │   │   ├── controller/      # ChatController (调用edu-ai的RAG接口)
│   │   │   │   ├── service/
│   │   │   │   ├── entity/
│   │   │   │   └── repository/
│   │   │   ├── feedback/            # 反馈模块
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   └── entity/
│   │   │   ├── admin/               # 运营后台（配置管理、反馈管理、推荐问题）
│   │   │   │   ├── controller/
│   │   │   │   └── service/
│   │   │   └── feign/               # OpenFeign 客户端（调用edu-ai）
│   │   │       └── AiServiceClient.java
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   └── db/migration/        # Flyway迁移（核心业务表）
│   │   └── pom.xml
│   │
│   └── edu-ai/                      # AI智能服务 (:8082)
│       ├── src/main/java/com/edu/ai/intelligence/
│       │   ├── AiApplication.java
│       │   ├── config/              # AsyncConfig, ModelConfig
│       │   ├── rag/                 # RAG引擎
│       │   │   ├── controller/      # 对外暴露RAG接口（供edu-core调用）
│       │   │   ├── engine/          # RagEngine, QueryRewriter, CitationValidator
│       │   │   └── prompt/          # Prompt模板管理
│       │   ├── embedding/           # 向量化服务 (Qwen3-Embedding-8B)
│       │   │   └── EmbeddingService.java
│       │   ├── rerank/              # 重排序服务 (Qwen3-Reranker /v1/score)
│       │   │   └── RerankService.java
│       │   ├── llm/                 # LLM调用封装
│       │   │   └── LlmClient.java
│       │   ├── cache/               # 语义缓存
│       │   │   └── SemanticCacheService.java
│       │   ├── filter/              # 敏感词过滤
│       │   │   └── SensitiveWordFilter.java
│       │   ├── knowledge/           # 知识库检索与管理
│       │   │   ├── controller/      # 知识CRUD API（运营后台）
│       │   │   ├── service/
│       │   │   ├── entity/
│       │   │   └── repository/      # VectorSearchRepository
│       │   ├── document/            # 文档解析管道
│       │   │   ├── controller/      # 文件上传/状态查询API
│       │   │   ├── parser/          # PdfParser, WordParser
│       │   │   ├── splitter/        # TextSplitter
│       │   │   └── service/
│       │   ├── desensitize/         # 脱敏管道
│       │   │   ├── controller/      # 脱敏审核API
│       │   │   ├── ner/             # NER识别
│       │   │   ├── rule/            # 规则引擎
│       │   │   └── service/
│       │   ├── platform/            # 平台数据同步
│       │   │   ├── controller/      # 手动同步触发API
│       │   │   ├── client/          # PlatformApiClient
│       │   │   └── sync/            # 定时调度
│       │   ├── analytics/           # 对话日志分析
│       │   │   ├── controller/
│       │   │   └── service/
│       │   └── feign/               # OpenFeign 客户端（回调edu-core）
│       │       └── CoreServiceClient.java
│       ├── src/main/resources/
│       │   ├── application.yml
│       │   └── db/migration/        # Flyway迁移（知识库/AI相关表）
│       └── pom.xml
│
├── docs/                            # 项目文档
├── demo/                            # 静态Demo
└── README.md
```

### 1.5 Maven 多模块结构

```xml
<!-- backend/pom.xml (父POM) -->
<groupId>com.edu.ai</groupId>
<artifactId>edu-ai-community</artifactId>
<packaging>pom</packaging>

<modules>
    <module>edu-common</module>
    <module>edu-gateway</module>
    <module>edu-core</module>
    <module>edu-ai</module>
</modules>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.x</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.x</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 1.6 服务间调用接口 (OpenFeign)

**edu-core → edu-ai (AiServiceClient)：**

```java
@FeignClient(name = "edu-ai")
public interface AiServiceClient {
    // 对话时调用RAG引擎
    @PostMapping("/internal/rag/chat")
    Flux<String> ragChat(RagChatRequest request);

    // 敏感词检测
    @PostMapping("/internal/filter/check-input")
    FilterResult checkInput(String text);

    // 知识条目变更通知（触发缓存失效）
    @PostMapping("/internal/cache/invalidate")
    void invalidateCache(List<String> chunkIds);
}
```

**edu-ai → edu-core (CoreServiceClient)：**

```java
@FeignClient(name = "edu-core")
public interface CoreServiceClient {
    // 文档解析完成回调
    @PostMapping("/internal/documents/{id}/parse-complete")
    void notifyParseComplete(UUID documentId, ParseStatus status);

    // 获取系统配置（免责声明、敏感词库）
    @GetMapping("/internal/config/{key}")
    String getConfig(String key);
}
```

**内部接口约定：** 所有服务间调用使用 `/internal/**` 前缀，Gateway 不对外暴露此路径，仅允许服务间直连访问。

---

## 2. 模块职责与依赖关系

### 2.1 服务间依赖图

```
                    ┌──────────────────────┐
                    │      Gateway         │
                    │  路由 / 鉴权 / 限流   │
                    └──────┬───────┬───────┘
                           │       │
           ┌───────────────┘       └───────────────┐
           ▼                                        ▼
┌─────────────────────┐              ┌─────────────────────────┐
│     edu-core        │   OpenFeign  │       edu-ai            │
│                     │ ──────────→  │                         │
│  auth               │              │  rag ──→ embedding      │
│  chat ──→ [Feign] ──┤              │   │  ──→ rerank         │
│  feedback           │  ←────────── │   │  ──→ llm            │
│  admin/config       │   OpenFeign  │   ▼                     │
│  admin/feedback     │              │  knowledge (检索/CRUD)   │
│  admin/recommend    │              │  document (解析/切分)    │
│                     │              │  desensitize (脱敏)      │
│                     │              │  cache (语义缓存)        │
│                     │              │  filter (敏感词)         │
│                     │              │  platform (数据同步)     │
│                     │              │  analytics (日志分析)    │
└─────────┬───────────┘              └──────────┬──────────────┘
          │                                     │
          ▼                                     ▼
     ┌────────┐  ┌────────┐              ┌──────────────┐
     │  PG    │  │ Redis  │              │  模型API      │
     │(共享)  │  │ (共享) │              │  (Qwen3系列)  │
     └────────┘  └────────┘              └──────────────┘
```

### 2.2 edu-core 模块职责

| 模块 | 职责 | 关键类 |
|------|------|--------|
| `auth` | 用户注册/登录、JWT签发刷新、角色管理、操作审计 | `AuthController`, `JwtProvider`, `UserService` |
| `chat` | 对话会话管理、消息CRUD、SSE流式透传、上下文管理（Redis） | `ChatController`, `ConversationService`, `MessageService` |
| `feedback` | 评分收集、反馈提交、运营回复、用户反馈记录查看 | `FeedbackController`, `FeedbackService` |
| `admin/config` | 快捷问题配置、角色推荐内容配置、系统配置（免责声明/敏感词） | `ConfigController` |
| `admin/feedback` | 反馈管理（列表/状态/回复/知识库同步触发） | `AdminFeedbackController` |
| `admin/recommend` | 推荐问题CRUD（按角色管理一问一答） | `RecommendedQuestionController` |
| `feign` | 调用edu-ai的RAG引擎、敏感词过滤、缓存失效等接口 | `AiServiceClient` |

### 2.3 edu-ai 模块职责

| 模块 | 职责 | 关键类 |
|------|------|--------|
| `rag` | RAG编排：查询改写→混合检索→重排→生成→引用校验 | `RagEngine`, `RagController`, `QueryRewriter`, `CitationValidator` |
| `embedding` | 文本向量化（调用Qwen3-Embedding-8B） | `EmbeddingService` |
| `rerank` | 检索结果重排序（调用Qwen3-Reranker-8B `/v1/score`） | `RerankService` |
| `llm` | LLM调用封装、流式响应、Prompt模板管理 | `LlmClient`, `PromptTemplate` |
| `cache` | 语义缓存：Embedding相似度匹配、按角色分组、自动失效 | `SemanticCacheService` |
| `filter` | 敏感词双向过滤（输入侧+输出侧） | `SensitiveWordFilter` |
| `knowledge` | 知识条目CRUD、向量检索、关键词检索、角色可见性过滤 | `KnowledgeController`, `KnowledgeService`, `VectorSearchRepository` |
| `document` | PDF/Word解析、表格提取、图片识别、语义切分、文件上传 | `DocumentController`, `PdfParser`, `WordParser`, `TextSplitter` |
| `desensitize` | NER实体识别、规则替换、映射表管理、审核流程 | `DesensitizeController`, `DesensitizeService`, `NerService` |
| `platform` | 校共体网站数据同步、增量拉取、向量化入库 | `PlatformSyncController`, `PlatformSyncService`, `PlatformApiClient` |
| `analytics` | 对话日志查看、基础统计分析（日活/日对话数/热门问题） | `AnalyticsController`, `AnalyticsService` |
| `feign` | 回调edu-core（解析完成通知、获取系统配置） | `CoreServiceClient` |

### 2.4 对话流程中的服务协作

```
用户发送消息
    │
    ▼
[Gateway] → JWT鉴权 → 路由至 edu-core
    │
    ▼
[edu-core] ChatController.sendMessage()
    │  1. 创建/获取会话
    │  2. 保存用户消息
    │  3. 读取Redis对话上下文
    │
    │── OpenFeign ──→ [edu-ai] RagController.chat()
    │                     │  1. 敏感词过滤(输入)
    │                     │  2. 语义缓存查询
    │                     │  3. 查询改写
    │                     │  4. 混合检索 + 重排
    │                     │  5. LLM生成(SSE流式)
    │                     │  6. 敏感词过滤(输出)
    │                     │  7. 引用校验 + 免责声明
    │  ←── SSE流式返回 ───┘
    │
    │  4. 保存助手消息
    │  5. 更新Redis上下文
    │  6. SSE透传给前端
    ▼
用户收到流式回答
```

---

## 3. RAG 检索引擎设计

### 3.1 RAG Pipeline 流程

```
用户提问
    │
    ▼
┌─────────────────────────────┐
│ 1. 输入侧敏感词过滤 (FR39)  │ ← 命中则拒绝，返回安全提示
└──────────────┬──────────────┘
               │ 通过
               ▼
┌─────────────────────────────┐
│ 2. 语义缓存查询 (FR41)      │ ← Embedding→与缓存池相似度≥0.95
│    按角色分组匹配            │    命中→直接返回缓存答案
└──────────────┬──────────────┘
               │ 未命中
               ▼
┌─────────────────────────────┐
│ 3. 查询改写 (Query Rewrite) │ ← 结合对话上下文，将追问改写为独立查询
│    多轮上下文注入            │    如"上面的"→补全完整指代
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│ 4. 混合检索 + Rerank                     │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │ 向量语义检索  │  │ 关键词全文检索    │  │
│  │ pgvector     │  │ zhparser FTS     │  │
│  │ Top-20       │  │ Top-20           │  │
│  └──────┬──────┘  └────────┬─────────┘  │
│         │    加权融合 (RRF)  │            │
│         └────────┬─────────┘            │
│                  ▼                       │
│       ┌─────────────────┐               │
│       │ 角色可见性过滤   │ ← 按用户角色过滤知识条目 │
│       └────────┬────────┘               │
│                ▼                        │
│       ┌─────────────────┐               │
│       │ Rerank 重排序    │ ← Qwen3-Reranker-8B │
│       │ 取 Top-3        │   POST /v1/score     │
│       └─────────────────┘               │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│ 5. 置信度评估                            │
│    最高Rerank分数 < 阈值?                │
│    YES → 声明"暂无相关政策依据"           │
│    NO  → 继续生成                        │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│ 6. LLM 生成 (Qwen3-32B)                │
│    ⚠️ 上下文限制: 5500 tokens            │
│    System Prompt: ~400 tokens            │
│    - 角色信息注入                         │
│    - 强制引用指令                         │
│    Context: Top-3 检索片段 (~1500 tok)   │
│    History: 最近3轮对话 (~1500 tok)       │
│    生成空间: ~1600 tokens (~700字)        │
│    输出: SSE流式 + 引用标注 [1][2]...    │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│ 7. 后处理                                │
│    - 输出侧敏感词过滤 (FR39)             │
│    - 引用标注校验 (NFR11)                │
│    - 条款编号/数字正则校验 (NFR11)        │
│    - 免责声明追加 (FR38)                  │
│    - 缓存写入（高置信度+非多轮追问）      │
└──────────────────┬──────────────────────┘
                   │
                   ▼
              返回用户
```

### 3.2 检索策略细节

**向量检索（稠密检索）**

```sql
-- pgvector HNSW 索引近似最近邻查询
SELECT id, content, metadata, 1 - (embedding <=> $query_vector) AS score
FROM knowledge_chunks
WHERE visible_roles @> ARRAY[$user_role]::varchar[]
  AND deleted = false
ORDER BY embedding <=> $query_vector
LIMIT 20;
```

**关键词检索（稀疏检索）**

```sql
-- PostgreSQL 全文搜索，使用 zhparser 中文分词
SELECT id, content, metadata,
       ts_rank(search_vector, plainto_tsquery('zhcfg', $query)) AS score
FROM knowledge_chunks
WHERE search_vector @@ plainto_tsquery('zhcfg', $query)
  AND visible_roles @> ARRAY[$user_role]::varchar[]
  AND deleted = false
ORDER BY score DESC
LIMIT 20;
```

**融合策略：Reciprocal Rank Fusion (RRF)**

```
RRF_score(d) = Σ 1/(k + rank_i(d))
其中 k=60 (常量), rank_i 为各检索通道中文档d的排名
```

两路检索结果合并后交给 Qwen3-Reranker-8B 重排序（POST /v1/score），取 Top-3。

**Rerank 调用协议**：对候选逐条评分（可并发），按 score 降序取 Top-3 作为 LLM 上下文。

### 3.3 语义缓存设计 (FR41)

```
缓存键: role + embedding_hash
缓存值: { question, answer, citations, created_at }
TTL: 7天（配置项 app.cache.semantic-ttl）

写入条件:
  - 非多轮追问（is_followup = false）
  - Rerank最高分 ≥ 置信阈值
  - 生成回答通过敏感词过滤

读取条件:
  - 用户问题 Embedding 与缓存池中问题的余弦相似度 ≥ 0.95
  - 角色匹配

失效策略:
  - 知识库变更（文件上传/条目编辑/删除）时清除关联缓存
  - 基于文件ID和知识条目ID关联追踪
```

**Redis 存储结构:**

```
semantic_cache:{role}:{embedding_bucket}  →  Hash {
    question: "...",
    answer: "...",
    citations: "[...]",
    chunk_ids: "[...]",       # 关联的知识片段ID，用于失效追踪
    created_at: timestamp
}
```

### 3.4 Prompt 模板

**系统提示词 (System Prompt)** — 精简版，~400 tokens 预算

```
你是"校共体AI助手"，基于河南省校共体政策文件回答问题。
用户角色：{role_name}

回答规则：
1. 仅基于【参考资料】回答，无依据时说明"暂无相关政策依据"
2. 关键观点标注引用 [1][2]，末尾列出来源：[编号]《文件名》章节
3. 简洁专业，300-600字，分点阐述
4. 角色侧重：{role_focus}

## 参考资料
{retrieved_chunks}
```

> **Token 预算分配 (总 5500)**：
> - System Prompt: ~400
> - 对话历史 (最近3轮): ~1500
> - 检索片段 (Top-3, 每条≤500 tok): ~1500
> - 生成空间: ~1600 (~700字)

`{role_focus}` 按角色注入一行文本：
- 管理人员 → "侧重宏观政策与决策建议"
- 牵头校 → "侧重结对指导与管理经验"
- 成员校 → "侧重实践操作与案例"
- 研究人员 → "侧重深度分析与文献引用"

**查询改写提示词**

```
根据对话历史，将用户最新问题改写为一个独立、完整的检索查询。
不要回答问题，只输出改写后的查询文本。

对话历史：
{conversation_history}

最新问题：{current_question}

改写后的查询：
```

---

## 4. 文档解析管道

### 4.1 管道流程

```
文件上传 (PDF/DOCX)
    │
    ▼
┌───────────────────────┐
│ 1. 格式校验            │ ← 仅允许 .pdf / .docx，≤50MB
│    文件持久化存储       │    → MinIO/本地文件系统
└───────────┬───────────┘
            │ (异步任务)
            ▼
┌───────────────────────┐
│ 2. 文本提取            │
│  PDF → PDFBox          │ ← 提取文本、表格、图片
│  DOCX → POI            │ ← 提取文本、标题层级、表格
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│ 3. 图片/流程图识别      │ ← Qwen3-VL-235B (>100x100px)
│    生成文字描述         │    标注 source_type=image_ocr
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│ 4. 语义切分             │
│  按标题层级优先切分      │ ← 章→条→款
│  回退：按段落切分        │
│  目标：300-500 token    │ ← 适配5500上下文(Top-3×500=1500)
│  片段重叠：50 token     │
│  表格作独立片段         │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│ 5. 向量化               │ ← Qwen3-Embedding-8B
│    批量处理             │    batch_size=32
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│ 6. 入库                 │
│  向量 + 元数据 → pgvector│
│  全文索引 → tsvector     │ ← zhparser 自动分词
│  位置映射 → chunk_location│
└───────────────────────┘
```

### 4.2 元数据模型

每个知识片段(chunk)携带的元数据：

```json
{
  "document_id": "uuid",
  "document_name": "河南省校共体建设指南.pdf",
  "chunk_index": 12,
  "section_title": "第三章 结对模式",
  "page_number": 15,
  "paragraph_index": 3,
  "source_type": "text|table|image_ocr|platform",
  "visible_roles": ["all"],
  "platform_module": null,
  "platform_url": null,
  "created_at": "2026-04-08T10:00:00Z"
}
```

### 4.3 异步任务管理

文档解析为耗时操作，采用异步任务模式：

- 上传接口立即返回 `document_id` + `status=PARSING`
- 后台 `@Async` 线程池执行解析管道
- 前端轮询 `GET /api/admin/documents/{id}/status` 查询进度
- 状态流转：`UPLOADING → PARSING → EMBEDDING → COMPLETED / FAILED`

---

## 5. 脱敏管道

### 5.1 管道流程

```
原始案例文献
    │
    ▼
┌───────────────────────────────┐
│ 1. NER实体识别 (Qwen3-32B)    │
│    Prompt驱动零样本识别        │
│    识别：人名/校名/地名/机构名  │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 2. 正则规则匹配               │
│    手机号: 1\d{10}            │
│    身份证: \d{17}[\dXx]       │
│    邮箱: \S+@\S+\.\S+        │
│    银行卡: \d{16,19}          │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 3. 替换规则引擎               │
│    人名 → 某教师/某校长        │
│    校名 → 某小学/某中学        │
│    地名 → 某县/某镇(省级保留)  │
│    数字 → 量级近似(103→近百)   │
│    联系方式 → 删除             │
│    日期 → 季度/年份            │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 4. 映射表生成                  │
│    原始 → 脱敏 的对应关系表    │
│    仅运营可查看                │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 5. 人工审核                    │
│    左右对比（原文 vs 脱敏）     │
│    脱敏实体高亮                 │
│    操作：通过 / 修改后通过 / 拒绝│
└───────────────┬───────────────┘
                │ 审核通过
                ▼
┌───────────────────────────────┐
│ 6. 向量化入库                  │
│    脱敏文本用于展示             │
│    原始文本用于检索索引 (FR42)  │
└───────────────────────────────┘
```

### 5.2 检索与展示分离 (FR42)

```
检索层：使用原始文本的向量（保证召回率）
展示层：通过映射表实时替换为脱敏文本

API响应中仅包含脱敏后内容，原始文本不得出现在响应体中。
替换在服务端完成（DesensitizeService.applyMapping）。
```

### 5.3 NER Prompt 模板

```
请从以下文本中识别所有命名实体，按JSON格式返回。

识别类型：
- PERSON: 人名
- SCHOOL: 学校名称
- LOCATION: 地名（区分省级和省级以下）
- ORGANIZATION: 机构名称
- NUMBER: 精确数字（需模糊化的）
- DATE: 具体日期
- CONTACT: 联系方式

文本：
{input_text}

输出格式：
[{"text": "原文", "type": "PERSON", "start": 0, "end": 3}]
```

---

## 6. 模型API集成方案

### 6.1 模型清单与部署信息

> **部署环境**：内网机房，vLLM on Ascend NPU，OpenAI 兼容 API，无需 API Key。

| 模型 | Model ID | 地址 | 用途 | max_model_len | 超时 |
|------|----------|------|------|---------------|------|
| Qwen3-32B | `Qwen3-32B` | http://10.91.28.4:22214 | 主推理（问答/改写/脱敏） | **5500** ⚠️ | 60s |
| Qwen3-VL-235B | `Qwen3-VL-235B` | http://10.91.28.4:22237 | 文档图片/流程图识别 | 40960 | 120s |
| Qwen3-Embedding-8B | `Qwen3-Embedding-8B`（待确认） | http://10.91.28.4:22257 | 文档/查询向量化、语义缓存 | — | 30s |
| Qwen3-Reranker-8B | `Qwen3-Reranker-8B` | http://10.91.28.4:22205 | 检索结果精排 | 40960 | 15s |

**关键约束：Qwen3-32B 上下文仅 5500 tokens**，直接影响：
- 检索片段从 Top-5 降为 **Top-3**
- 对话历史从 5 轮降为 **3 轮**
- 切分片段从 500-1000 降为 **300-500 tokens**
- System Prompt 精简至 **~400 tokens**

### 6.2 统一调用封装

所有模型API采用 OpenAI 兼容接口格式（vLLM），封装为 `LlmClient`：

```java
@Component
public class LlmClient {
    // Chat Completion (Qwen3-32B) — POST /v1/chat/completions
    Flux<String> streamChat(ChatRequest request);         // SSE流式
    String chat(ChatRequest request);                     // 同步

    // Embedding (Qwen3-Embedding-8B) — POST /v1/embeddings
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);         // 批量

    // Rerank (Qwen3-Reranker-8B) — POST /v1/score
    List<RerankResult> rerank(String query, List<String> documents);

    // Vision (Qwen3-VL-235B) — POST /v1/chat/completions (multimodal)
    String describeImage(byte[] imageData, String prompt);
}
```

### 6.3 配置

```yaml
# application.yml
app:
  ai:
    # 无需 API Key（内网 vLLM 无鉴权）
    chat:
      base-url: http://10.91.28.4:22214/v1
      model: Qwen3-32B
      max-tokens: 1600          # 生成上限（5500 - prompt 占用）
      timeout: 60000
    embedding:
      base-url: http://10.91.28.4:22257/v1
      model: Qwen3-Embedding-8B  # Model ID 待服务启动后确认
      dimensions: 0              # 向量维度待确认（启动后测试）
      timeout: 30000
      batch-size: 32
    vision:
      base-url: http://10.91.28.4:22237/v1
      model: Qwen3-VL-235B
      timeout: 120000
    rerank:
      base-url: http://10.91.28.4:22205/v1
      model: Qwen3-Reranker-8B
      endpoint: /v1/score       # vLLM rerank 端点
      timeout: 15000
```

### 6.4 Rerank 调用协议

```
POST http://10.91.28.4:22205/v1/score
Content-Type: application/json

请求体:
{
  "model": "Qwen3-Reranker-8B",
  "text_1": "用户查询文本",
  "text_2": "候选文档片段"
}

响应体:
{
  "data": [{"index": 0, "score": 0.84}],
  "usage": {"prompt_tokens": 18, "total_tokens": 18}
}
```

对 Top-30 候选逐条评分（可并发），按 score 降序取 Top-3。

### 6.5 降级策略

| 组件 | 失败处理 |
|------|---------|
| Qwen3-32B | 重试1次 → 返回友好提示"智能助手暂时繁忙" |
| Qwen3-Embedding-8B | 重试3次 → 标记为pending，后台补偿任务重试 |
| Qwen3-Reranker-8B | 重试1次 → 降级使用RRF融合分数排序 |
| Qwen3-VL-235B | 不重试 → 跳过该图片，记录日志 |

---

## 7. 数据库 Schema 设计

### 7.1 PostgreSQL 扩展

```sql
CREATE EXTENSION IF NOT EXISTS vector;      -- pgvector
CREATE EXTENSION IF NOT EXISTS zhparser;    -- 中文分词
CREATE TEXT SEARCH CONFIGURATION zhcfg (PARSER = zhparser);
```

### 7.2 核心表结构

#### 用户表

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(11) NOT NULL UNIQUE,
    username        VARCHAR(20) NOT NULL,
    role            VARCHAR(30) NOT NULL,   -- 6种角色枚举
    user_type       VARCHAR(10) NOT NULL DEFAULT 'USER',  -- USER / ADMIN
    region          VARCHAR(100),
    school          VARCHAR(100),
    password_hash   VARCHAR(255),           -- 预留，当前用验证码登录
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMPTZ
);

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);
```

#### 会话与消息表

```sql
CREATE TABLE conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    title           VARCHAR(100),           -- 取首条消息前30字符
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_user ON conversations(user_id, created_at DESC);

CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    role            VARCHAR(10) NOT NULL,   -- user / assistant
    content         TEXT NOT NULL,           -- 用户消息存脱敏后版本(FR3)
    raw_content     TEXT,                    -- 原始内容(仅user角色，加密存储)
    citations       JSONB,                  -- 引用信息 [{file_name, section, page, chunk_id}]
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);
```

#### 知识库表

```sql
-- 文档表
CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,  -- 存储路径
    file_type       VARCHAR(10) NOT NULL,   -- pdf / docx
    file_size       BIGINT NOT NULL,
    visible_roles   VARCHAR[] NOT NULL DEFAULT '{"all"}',
    parse_status    VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    -- UPLOADING / PARSING / EMBEDDING / COMPLETED / FAILED
    error_message   TEXT,
    uploaded_by     UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 知识片段表（核心检索表）
CREATE TABLE knowledge_chunks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID REFERENCES documents(id),
    content         TEXT NOT NULL,           -- 片段文本
    content_raw     TEXT,                    -- 原始文本（脱敏案例用）
    embedding       vector,                  -- 向量（维度待Embedding服务确认后设置）
    search_vector   tsvector,               -- 全文搜索向量（zhparser自动分词）
    metadata        JSONB NOT NULL,          -- 元数据
    visible_roles   VARCHAR[] NOT NULL DEFAULT '{"all"}',
    source_type     VARCHAR(20) NOT NULL DEFAULT 'document',
    -- document / qa / platform / image_ocr
    platform_module VARCHAR(30),             -- 平台数据模块
    platform_id     VARCHAR(100),            -- 平台原始ID
    platform_url    VARCHAR(500),            -- 平台原文URL
    deleted         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- HNSW向量索引（维度确认后创建）
-- CREATE INDEX idx_chunks_embedding ON knowledge_chunks
--     USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 200);

-- 全文搜索索引
CREATE INDEX idx_chunks_search ON knowledge_chunks USING gin(search_vector);

-- 角色可见性索引
CREATE INDEX idx_chunks_roles ON knowledge_chunks USING gin(visible_roles);

-- 平台数据去重索引
CREATE UNIQUE INDEX idx_chunks_platform ON knowledge_chunks(platform_module, platform_id)
    WHERE platform_id IS NOT NULL AND deleted = false;

-- 自动更新 tsvector
CREATE TRIGGER tsvector_update BEFORE INSERT OR UPDATE ON knowledge_chunks
    FOR EACH ROW EXECUTE FUNCTION
    tsvector_update_trigger(search_vector, 'public.zhcfg', content);
```

#### 脱敏映射表

```sql
CREATE TABLE desensitize_mappings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id),
    original_text   TEXT NOT NULL,           -- 原始文本
    masked_text     TEXT NOT NULL,           -- 脱敏文本
    entity_type     VARCHAR(20) NOT NULL,    -- PERSON/SCHOOL/LOCATION/...
    start_offset    INT NOT NULL,
    end_offset      INT NOT NULL,
    review_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING / APPROVED / MODIFIED / REJECTED
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_desensitize_doc ON desensitize_mappings(document_id);
CREATE INDEX idx_desensitize_status ON desensitize_mappings(review_status);
```

#### 反馈表

```sql
CREATE TABLE feedback (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    message_id      UUID NOT NULL REFERENCES messages(id),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    rating          SMALLINT,               -- 1-5星
    reason_tags     VARCHAR[],              -- 预设原因标签
    reason_text     VARCHAR(500),           -- 自由文本
    status          VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    -- SUBMITTED / PENDING / PROCESSING / COMPLETED
    admin_reply     TEXT,                    -- 运营回复内容
    replied_by      UUID REFERENCES users(id),
    replied_at      TIMESTAMPTZ,
    synced_to_kb    BOOLEAN NOT NULL DEFAULT false,  -- 是否已同步至知识库
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_feedback_user ON feedback(user_id, created_at DESC);
CREATE INDEX idx_feedback_status ON feedback(status);
CREATE INDEX idx_feedback_rating ON feedback(rating);
```

#### 推荐问题表

```sql
CREATE TABLE recommended_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role            VARCHAR(30) NOT NULL,   -- 目标角色
    question        TEXT NOT NULL,           -- 问题文本
    answer          TEXT NOT NULL,           -- 预设标准答案
    sort_order      INT NOT NULL DEFAULT 0,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rq_role ON recommended_questions(role, sort_order);
```

#### 配置表

```sql
CREATE TABLE system_configs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    TEXT NOT NULL,
    description     VARCHAR(255),
    updated_by      UUID REFERENCES users(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 初始配置
INSERT INTO system_configs (config_key, config_value, description) VALUES
('disclaimer', '以上内容由AI基于公开政策文件生成，仅供参考，不构成官方解读。如需确认，请咨询当地教育行政部门。', '免责声明文案'),
('sensitive_words', '[]', '敏感词列表JSON');
```

#### 快捷问题与角色推荐内容

```sql
CREATE TABLE quick_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question        TEXT NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE role_recommendations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role            VARCHAR(30) NOT NULL,
    content_type    VARCHAR(20) NOT NULL,   -- policy_news / case_study
    title           VARCHAR(255) NOT NULL,
    summary         TEXT,
    link_url        VARCHAR(500),
    sort_order      INT NOT NULL DEFAULT 0,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_role_rec ON role_recommendations(role, content_type, sort_order);
```

#### 平台数据同步记录

```sql
CREATE TABLE platform_sync_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sync_type       VARCHAR(20) NOT NULL,   -- FULL / INCREMENTAL
    status          VARCHAR(20) NOT NULL,    -- RUNNING / COMPLETED / FAILED
    total_fetched   INT DEFAULT 0,
    new_count       INT DEFAULT 0,
    updated_count   INT DEFAULT 0,
    failed_count    INT DEFAULT 0,
    last_publish_time TIMESTAMPTZ,           -- 本次同步最大publishTime
    error_message   TEXT,
    triggered_by    VARCHAR(20) NOT NULL,    -- SCHEDULE / MANUAL
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);
```

#### 操作审计日志

```sql
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id),
    action          VARCHAR(50) NOT NULL,    -- LOGIN / UPLOAD / DELETE / ...
    resource_type   VARCHAR(50),
    resource_id     VARCHAR(100),
    detail          JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_user ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit_logs(action, created_at DESC);
```

### 7.3 ER 关系概览

```
users ──1:N──→ conversations ──1:N──→ messages
  │                                      │
  │                                      │1:N
  │                                      ▼
  ├──1:N──→ feedback ←────────────── messages
  │
  └──1:N──→ audit_logs

documents ──1:N──→ knowledge_chunks
    │
    └──1:N──→ desensitize_mappings

system_configs (独立)
recommended_questions (独立)
quick_questions (独立)
role_recommendations (独立)
platform_sync_logs (独立)
```

---

## 8. Redis 设计

### 8.1 数据结构规划

| Key Pattern | 类型 | 用途 | TTL |
|-------------|------|------|-----|
| `session:{user_id}:{conversation_id}` | List/String | 对话上下文（最近3轮） | 24h |
| `token:blacklist:{jti}` | String | JWT黑名单（登出/刷新后） | 与Token剩余有效期一致 |
| `sms:code:{phone}` | String | 短信验证码 | 5min |
| `sms:limit:{phone}` | String | 短信发送频率限制 | 60s |
| `login:limit:{phone}` | String(计数器) | 登录频率限制 | 5min |
| `semantic_cache:{role}:{bucket}` | Hash | 语义缓存（Embedding相似度匹配） | 7d |
| `cache:invalidate:chunks` | Set | 待失效的chunk_id集合 | — |
| `platform:sync:lock` | String | 平台同步分布式锁 | 30min |
| `platform:sync:last_time` | String | 上次同步的最大publishTime | 永久 |

### 8.2 对话上下文管理 (NFR13 适配)

```
Key: session:{user_id}:{conversation_id}
Value: JSON序列化的消息列表

结构:
[
  {"role": "user", "content": "..."},
  {"role": "assistant", "content": "..."},
  ... (最多6条 = 3轮)
]

约束（适配 Qwen3-32B 5500 token 限制）:
- 传入LLM的对话历史: 最近 3 轮（6条消息），Token ≤ 1500
- 单次对话最大轮数 ≤ 30轮（超出提示开启新对话）
- 超出3轮窗口的历史仍保存在数据库，但不传入LLM
- TTL = 24小时
- Redis不可用时降级从PostgreSQL messages表读取
```

### 8.3 JWT Token 管理

```
登录流程:
1. 验证手机号+验证码
2. 签发 access_token (2h) + refresh_token (7d)
3. access_token 含 {user_id, role, user_type, jti, exp}

刷新流程:
1. 前端在 access_token 剩余5分钟时调用 /api/auth/refresh
2. 验证 refresh_token 有效性
3. 签发新 access_token + 新 refresh_token（滑动过期）
4. 旧 refresh_token 的 jti 加入 Redis 黑名单

登出流程:
1. 当前 access_token 和 refresh_token 的 jti 加入黑名单
2. 黑名单 TTL = Token剩余有效期
```

---

## 9. API 接口规范

### 9.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-04-08T10:00:00Z"
}
```

错误响应：

```json
{
  "code": 400,
  "message": "请输入正确的手机号",
  "data": null,
  "timestamp": "2026-04-08T10:00:00Z"
}
```

### 9.2 完整 API 列表

> **服务标注：** `C` = edu-core, `A` = edu-ai

#### 认证模块 (edu-core)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/auth/send-sms` | 发送短信验证码 | 无 | C |
| POST | `/api/auth/register` | 手机号注册 | 无 | C |
| POST | `/api/auth/login` | 手机号登录 | 无 | C |
| POST | `/api/auth/refresh` | 刷新Token | refresh_token | C |
| POST | `/api/auth/logout` | 登出 | access_token | C |

#### 对话模块

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/chat` | 发送消息（SSE流式返回，core→ai Feign） | USER | A |
| GET | `/api/conversations` | 获取历史会话列表 | USER | C |
| GET | `/api/conversations/{id}/messages` | 获取会话消息 | USER | C |
| DELETE | `/api/conversations/{id}` | 删除会话 | USER | C |

#### 知识检索 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/internal/rag/chat` | RAG问答（服务间调用） | INTERNAL | A |
| GET | `/api/documents/{id}/preview` | 文档在线预览 | USER | A |

#### 推荐与配置（用户端，edu-core）

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/recommended-questions` | 获取当前角色推荐问题 | USER | C |
| GET | `/api/recommendations/{role}` | 获取角色推荐内容 | USER | C |
| GET | `/api/quick-questions` | 获取快捷问题 | USER | C |

#### 反馈模块（用户端，edu-core）

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/feedback` | 提交评分/反馈 | USER | C |
| POST | `/api/feedback/escalate` | 提交至运营处理 | USER | C |
| GET | `/api/feedback/my-records` | 查看我的反馈记录 | USER | C |

#### 运营后台 — 知识库管理 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/knowledge` | 知识条目列表（分页/搜索/筛选） | ADMIN | A |
| POST | `/api/admin/knowledge` | 新增知识条目 | ADMIN | A |
| PUT | `/api/admin/knowledge/{id}` | 编辑知识条目 | ADMIN | A |
| DELETE | `/api/admin/knowledge/{id}` | 删除知识条目（软删） | ADMIN | A |
| POST | `/api/admin/documents/upload` | 上传文件 | ADMIN | A |
| GET | `/api/admin/documents/{id}/status` | 查询解析状态 | ADMIN | A |

#### 运营后台 — 脱敏审核 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/cases/pending` | 待审核脱敏列表 | ADMIN | A |
| PUT | `/api/admin/cases/{id}/review` | 审核脱敏结果 | ADMIN | A |

#### 运营后台 — 反馈管理 (edu-core)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/admin/auth/login` | 运营登录 | 无 | C |
| GET | `/api/admin/feedback` | 反馈列表 | ADMIN | C |
| PUT | `/api/admin/feedback/{id}/status` | 更新反馈状态 | ADMIN | C |
| PUT | `/api/admin/feedback/{id}/resolve` | 回复并完成反馈 | ADMIN | C |

#### 运营后台 — 问答审核 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/qa/pending` | 待审核问答列表 | ADMIN | A |
| POST | `/api/admin/qa/approve` | 审核入库 | ADMIN | A |
| GET | `/api/admin/questions/pending` | 用户问题待审列表 | ADMIN | A |

#### 运营后台 — 对话日志与分析 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/conversations` | 对话日志列表 | ADMIN | A |
| GET | `/api/admin/analytics` | 统计分析数据 | ADMIN | A |

#### 运营后台 — 配置管理 (edu-core)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/config/quick-questions` | 快捷问题配置 | ADMIN | C |
| PUT | `/api/admin/config/quick-questions` | 更新快捷问题 | ADMIN | C |
| GET | `/api/admin/config/role-recommendations/{role}` | 角色推荐内容 | ADMIN | C |
| PUT | `/api/admin/config/role-recommendations/{role}` | 更新角色推荐 | ADMIN | C |
| GET | `/api/admin/config/system` | 系统配置（免责声明/敏感词） | ADMIN | C |
| PUT | `/api/admin/config/system` | 更新系统配置 | ADMIN | C |

#### 运营后台 — 推荐问题管理 (edu-core)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/recommended-questions` | 推荐问题列表 | ADMIN | C |
| GET | `/api/admin/recommended-questions/{role}` | 按角色获取 | ADMIN | C |
| POST | `/api/admin/recommended-questions` | 新增推荐问题 | ADMIN | C |
| PUT | `/api/admin/recommended-questions/{id}` | 编辑推荐问题 | ADMIN | C |
| DELETE | `/api/admin/recommended-questions/{id}` | 删除推荐问题 | ADMIN | C |

#### 运营后台 — 平台数据同步 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/admin/platform/sync` | 手动触发同步 | ADMIN | A |
| GET | `/api/admin/platform/sync-status` | 同步状态查询 | ADMIN | A |

#### 服务间内部接口 (不经Gateway)

| 方法 | 路径 | 说明 | 方向 |
|------|------|------|------|
| POST | `/internal/rag/chat` | RAG问答（SSE流式） | C → A |
| POST | `/internal/filter/check-input` | 敏感词输入检测 | C → A |
| POST | `/internal/cache/invalidate` | 语义缓存失效 | C → A |
| POST | `/internal/documents/{id}/parse-complete` | 文档解析完成回调 | A → C |
| GET | `/internal/config/{key}` | 获取系统配置 | A → C |

### 9.3 SSE 流式接口 (POST /api/chat)

**请求：**

```json
{
  "conversation_id": "uuid | null",
  "message": "校共体怎么结对？"
}
```

**SSE 事件流：**

```
event: message_start
data: {"conversation_id": "uuid", "message_id": "uuid"}

event: content_delta
data: {"delta": "根据"}

event: content_delta
data: {"delta": "《河南省"}

...

event: citations
data: {"citations": [{"index": 1, "file_name": "...", "section": "...", "page": 15, "chunk_id": "uuid"}]}

event: message_end
data: {"finish_reason": "stop", "usage": {"prompt_tokens": 1200, "completion_tokens": 450}}
```

### 9.4 认证与权限

```
所有API请求头:
Authorization: Bearer {access_token}

权限层级:
- 无认证: /api/auth/*
- USER: 普通用户接口（user_type = USER 或 ADMIN）
- ADMIN: 运营后台接口（user_type = ADMIN）
- INTERNAL: 服务间调用（/internal/**，Gateway不暴露，仅服务直连）

认证链路:
1. Gateway — JwtAuthGlobalFilter 统一解析Token、校验黑名单
2. Gateway — 将 X-User-Id、X-User-Role、X-User-Type 注入请求头透传给下游服务
3. edu-core / edu-ai — 从Header读取用户信息，@PreAuthorize 方法级权限控制
4. /internal/** — 不经过Gateway，服务间通过网络隔离保障安全

API 归属:
- edu-core: /api/auth/**, /api/conversations/**, /api/feedback/**,
            /api/recommended-questions/**, /api/quick-questions/**,
            /api/admin/feedback/**, /api/admin/config/**,
            /api/admin/recommended-questions/**
- edu-ai:   /api/chat/**, /api/search/**, /api/documents/**,
            /api/admin/documents/**, /api/admin/knowledge/**,
            /api/admin/cases/**, /api/admin/platform/**,
            /api/admin/qa/**, /api/admin/conversations/**,
            /api/admin/analytics/**
```

---

## 10. 敏感词过滤设计 (FR39)

### 10.1 双向过滤架构

```
输入侧:
  用户消息 → SensitiveWordFilter.checkInput(text)
    ├── 命中 → 拒绝处理，返回安全提示
    └── 通过 → 进入RAG Pipeline

输出侧:
  LLM生成内容 → SensitiveWordFilter.checkOutput(text)
    ├── 命中 → 替换为安全回答
    └── 通过 → 正常返回
```

### 10.2 实现方案

- 使用 **DFA（确定有限自动机）** 或 **Aho-Corasick** 算法实现高性能多模式匹配
- 敏感词库从 `system_configs` 表加载，Redis 缓存
- 运营后台可增删改敏感词，修改后自动刷新缓存

---

## 11. 隐私信息检测 (FR40)

### 11.1 前端实时检测

```typescript
// 正则模式匹配
const PRIVACY_PATTERNS = [
  { name: '身份证号', pattern: /\d{17}[\dXx]/ },
  { name: '手机号', pattern: /1[3-9]\d{9}/ },
  { name: '银行卡号', pattern: /\d{16,19}/ },
];

// 检测到时弹出提示，用户选择继续或修改
```

### 11.2 后端日志脱敏 (FR3)

用户消息入库前，对敏感信息执行掩码：

```
身份证号: 4101**********1234
手机号: 138****8000
银行卡号: 6222****1234
```

原始消息加密存储于 `messages.raw_content`（仅系统内部使用，运营后台不展示）。

---

## 12. 平台数据同步 (FR33-FR37)

### 12.1 同步架构

```
┌──────────────────────────────────┐
│    Spring @Scheduled (每6小时)    │
│    或运营手动触发                  │
└──────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│  Redis分布式锁 (防并发)           │
│  Key: platform:sync:lock         │
└──────────────┬───────────────────┘
               │ 获取锁
               ▼
┌──────────────────────────────────┐
│  读取上次同步时间                  │
│  Key: platform:sync:last_time    │
└──────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│  分页拉取平台API                  │
│  {base_url}/api/content/v1/search│
│  增量: publishTime > last_time   │
│  全量: 无时间过滤                 │
│  每页 size=50                    │
└──────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│  去重 (module + platform_id)      │
│  新记录→插入，已有→更新            │
└──────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│  向量化 (Qwen3-Embedding-8B)     │
│  文本: [模块类型] title\n\ndesc  │
│  source_type = 'platform'        │
└──────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│  写入 knowledge_chunks            │
│  记录 platform_sync_logs          │
│  更新 last_time                   │
└──────────────────────────────────┘
```

### 12.2 配置项

```yaml
app:
  platform:
    base-url: ${PLATFORM_BASE_URL:https://edu.51xiaoping.com}
    sync-cron: "0 0 */6 * * ?"     # 每6小时
    page-size: 50
    modules:
      - model_school
      - news
      - policy
      - resource
      - activity
      - monthly_report
```

---

## 13. 部署方案

### 13.1 部署架构

```
┌──────────────────────────────────────────────────────────┐
│                    Nginx (反向代理 + SSL终止)              │
│  /              → Vue 3 静态文件                          │
│  /api/          → Gateway (:8080)                        │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   Gateway (:8080)    │
              │   路由 / 鉴权 / 限流  │
              └─────┬──────────┬─────┘
                    │          │
         ┌──────────┘          └──────────┐
         ▼                                ▼
┌──────────────────┐           ┌──────────────────┐
│  edu-core (:8081)│  Feign →  │  edu-ai (:8082)  │
│  核心业务服务     │  ← Feign  │  AI智能服务       │
└────────┬─────────┘           └────────┬─────────┘
         │                              │
         ▼                              ▼
    ┌────────┐  ┌────────┐     ┌──────────────────┐
    │  PG    │  │ Redis  │     │  文件存储          │
    │ :5432  │  │ :6379  │     │  /data/documents/ │
    └────────┘  └────────┘     └──────────────────┘
      (共享)      (共享)
```

### 13.2 环境要求

| 组件 | 最低配置 | 推荐配置 |
|------|---------|---------|
| 应用服务器（3个JVM进程） | 4C 8G | 8C 16G |
| — Gateway | ~256MB heap | ~512MB heap |
| — edu-core | ~512MB heap | ~1GB heap |
| — edu-ai | ~1GB heap | ~2GB heap |
| PostgreSQL | 2C 4G, 50GB SSD | 4C 8G, 100GB SSD |
| Redis | 1C 2G | 2C 4G |
| 模型API | 外部调用（无需GPU） | — |

### 13.3 构建与启动

```bash
# 全量构建（Maven多模块）
cd backend
mvn clean package -DskipTests

# 启动 Gateway
java -jar edu-gateway/target/edu-gateway.jar \
  --spring.profiles.active=prod &

# 启动 edu-core
java -jar edu-core/target/edu-core.jar \
  --spring.profiles.active=prod &

# 启动 edu-ai
java -jar edu-ai/target/edu-ai.jar \
  --spring.profiles.active=prod &

# 前端
cd frontend
npm install
npm run build
# dist/ 部署至 Nginx 静态目录
```

---

## 14. 安全设计

| 安全层 | 措施 |
|--------|------|
| 传输层 | 全链路HTTPS，HSTS头 |
| 认证 | Gateway统一JWT鉴权（JwtAuthGlobalFilter），未认证请求直接拒绝 |
| Token管理 | JWT + refresh_token滑动过期 + Redis登出黑名单 |
| 授权 | RBAC (USER / ADMIN)，Gateway解析角色后透传Header，服务内@PreAuthorize |
| 服务间安全 | `/internal/**` 路径Gateway不对外暴露，仅允许服务间直连 |
| 输入校验 | Spring Validation + 自定义校验注解 |
| SQL注入 | JPA参数化查询，禁止拼接SQL |
| XSS | 前端输出编码，CSP头 |
| 敏感数据 | 用户消息脱敏存储，原始内容加密，运营不可见 |
| 频率限制 | Gateway层Redis限流：短信60s/次，登录5min锁定，全局API限流 |
| CORS | Gateway统一CORS配置，仅允许校共体网站域名 |
| 审计 | 关键操作记录 audit_logs |

---

## 15. 可观测性

| 维度 | 方案 |
|------|------|
| 日志 | SLF4J + Logback，结构化JSON日志，按日轮转 |
| 健康检查 | Spring Boot Actuator `/actuator/health` |
| 指标 | Micrometer → Prometheus（可选） |
| 链路 | 请求ID贯穿日志（MDC），便于排查 |
| 业务监控 | 对话量、平均评分、缓存命中率、模型API延迟 |
