# 8. Next Steps

## 8.1 UX Expert Prompt

本项目包含完整的前端界面需求（用户端对话界面+运营后台），请基于本PRD创建前端设计规范文档（Front-End Spec），重点关注：对话界面交互、原文预览跳转、角色化侧边栏、运营后台布局。

## 8.2 Architect Prompt

请基于本PRD创建系统架构文档（Architecture），重点关注：RAG检索引擎设计（混合检索+Rerank）、文档解析管道、脱敏管道、模型API集成方案（vLLM内网部署，Qwen3-32B上下文5500限制）、数据库Schema设计（PostgreSQL+pgvector+zhparser）、Redis会话管理、API接口规范。
