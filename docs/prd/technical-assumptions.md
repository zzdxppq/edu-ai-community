# 4. Technical Assumptions

## 4.1 Repository Structure: Monorepo

采用Monorepo结构，前后端在同一仓库中管理，简化部署和版本协同。

## 4.2 Service Architecture

前后端分离 + 智能中台设计：

- **前端：** Vue 3 + Element Plus SPA，嵌入校共体网站
- **后端：** JDK 21 + Spring Cloud 微服务框架，按业务拆分为1-2个服务模块（如：核心业务服务、AI智能服务），提供REST API
- **智能中台：** RAG检索引擎（混合检索+重排序）、文档解析管道、脱敏管道
- **模型层：** 通过API调用Qwen3系列模型

**模型分工：**（2026-04-21 实测确认）

| 模型 | 用途 | 部署地址 | 备注 |
|------|------|---------|------|
| Qwen3-32B | 主推理（问答生成、多轮对话、NER脱敏） | http://10.91.28.4:22214 | max_model_len=5500 ⚠️ |
| Qwen3-VL-235B | 文档中表格/流程图的视觉理解 | http://10.91.28.4:22237 | max_model_len=40960 |
| Qwen3-Embedding-8B | 文档和查询向量化 | http://10.91.28.4:22257 | 向量维度4096 |
| Qwen3-Reranker-8B | 检索结果重排序 | http://10.91.28.4:22205 | POST /v1/score |

> **关键约束**：Qwen3-32B 上下文仅 5500 tokens，检索片段限 Top-3、对话历史限 3 轮、System Prompt 精简至 ~400 tokens。所有模型通过内网 vLLM (Ascend NPU) 部署，无需 API Key。

## 4.3 Testing Requirements

Unit + Integration — 后端API接口测试 + RAG检索质量测试 + 前端组件测试

## 4.4 Additional Technical Assumptions and Requests

- **数据库：** PostgreSQL + pgvector扩展 + zhparser扩展（业务数据+向量数据+全文搜索统一管理）
- **缓存：** Redis 7（用户会话上下文、热点知识缓存、分布式Session）
- **文档解析：** PDF解析（Apache PDFBox）、Word解析（Apache POI）、流程图识别（Qwen3-VL）
- **文件存储：** 原始文件本地/对象存储，需保留用于原文跳转预览和在线预览
- **认证方案：** JWT Token（手机号+验证码登录），Spring Security + JWT 实现
- **部署：** 本地服务器或云主机均可，无需GPU（模型通过API调用）
- **前端框架：** Vue 3 + Element Plus（已确认）
- **后端框架：** JDK 21 + Spring Boot 3.x + Spring Cloud（已确认），构建工具 Maven/Gradle
- **外部数据源：** 校共体网站搜索API（默认域名 `https://edu.51xiaoping.com`，通过 `app.platform.base-url` 配置项动态调整）；API路径 `/api/content/v1/search`，支持 `keyword`、`page`、`size` 参数；返回结构包含 `data.list[]`（含 module/id/title/description/publishTime/url）和分页信息（total/page/size）；内容模块包括：示范校、新闻资讯、政策文件、资源共享、活动、月报

---
