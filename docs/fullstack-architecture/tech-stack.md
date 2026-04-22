# Tech Stack

## 总览

| 层 | 技术 | 版本 | 说明 |
|----|------|------|------|
| **前端** | Vue 3 + Element Plus | 3.4+ | Composition API, `<script setup>` |
| | Pinia | 2.x | 状态管理 |
| | Vue Router | 4.x | SPA 路由 |
| | Axios | 1.x | HTTP 客户端 |
| | Vite | 5.x | 构建工具 |
| | TypeScript | 5.x | 类型安全 |
| **后端** | JDK 21 | 21 LTS | 长期支持版 |
| | Spring Boot | 3.2+ | 应用框架 |
| | Spring Cloud | 2023.x | 微服务框架 |
| | Spring Cloud Gateway | — | API 网关 |
| | Spring Cloud OpenFeign | — | 服务间调用 |
| | Spring Security | 6.x | 认证授权 |
| | Spring Data JPA | — | ORM (Hibernate 6) |
| | Spring Data Redis | — | 缓存客户端 |
| | Flyway | — | 数据库迁移 |
| | Maven | 3.9+ | 构建工具（多模块） |
| **数据库** | PostgreSQL | 16 | 主数据库 |
| | pgvector | 0.7+ | 向量检索扩展 |
| | zhparser | — | 中文全文搜索分词 |
| **缓存** | Redis | 7.x | 会话/缓存/锁 |
| **文档解析** | Apache PDFBox | 3.x | PDF 解�� |
| | Apache POI | 5.x | Word 解析 |
| **AI 模型** | Qwen3-32B | vLLM | 主推理 (5500 ctx) |
| | Qwen3-VL-235B | vLLM | 视觉理解 |
| | Qwen3-Embedding-8B | vLLM | 向量化 (dim=4096) |
| | Qwen3-Reranker-8B | vLLM | 重排序 |

## 微服务拆分

| 服务 | 端口 | 职责 |
|------|------|------|
| **edu-gateway** | 8080 | 路由、JWT鉴权、限流 |
| **edu-core** | 8081 | 用户认证、对话管理、反馈、运营配置 |
| **edu-ai** | 8082 | RAG引擎、文档解析、向量化、脱敏、检索、平台同步 |

## Maven 模块

```
backend/
├── pom.xml              ← 父POM
├── edu-common/          ← 共享Entity/DTO/异常/工具
├── edu-gateway/         ← Spring Cloud Gateway
├── edu-core/            ← 核心业务服务
└── edu-ai/              ← AI智能服务
```

## 外部依赖

| 依赖 | 地址 | 用途 |
|------|------|------|
| Qwen3 模型集群 | 10.91.28.4:22214/22237/22257/22205 | AI推理（内网vLLM，无API Key） |
| 校共体网站 API | edu.51xiaoping.com/api/content/v1/search | 平台数据同步 |
