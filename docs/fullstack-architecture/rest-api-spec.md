# 9. API 接口规范

## 9.1 统一响应格式

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

## 9.2 完整 API 列表

> **服务标注：** `C` = edu-core, `A` = edu-ai

### 认证模块 (edu-core)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/auth/send-sms` | 发送短信验证码 | 无 | C |
| POST | `/api/auth/register` | 手机号注册 | 无 | C |
| POST | `/api/auth/login` | 手机号登录 | 无 | C |
| POST | `/api/auth/refresh` | 刷新Token | refresh_token | C |
| POST | `/api/auth/logout` | 登出 | access_token | C |

### 对话模块

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/chat` | 发送消息（SSE流式返回，core→ai Feign） | USER | A |
| GET | `/api/conversations` | 获取历史会话列表 | USER | C |
| GET | `/api/conversations/{id}/messages` | 获取会话消息 | USER | C |
| DELETE | `/api/conversations/{id}` | 删除会话 | USER | C |

### 知识检索 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/internal/rag/chat` | RAG问答（服务间调用） | INTERNAL | A |
| GET | `/api/documents/{id}/preview` | 文档在线预览 | USER | A |

### 推荐与配置（用户端，edu-core）

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/recommended-questions` | 获取当前角色推荐问题 | USER | C |
| GET | `/api/recommendations/{role}` | 获取角色推荐内容 | USER | C |
| GET | `/api/quick-questions` | 获取快捷问题 | USER | C |

### 反馈模块（用户端，edu-core）

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/feedback` | 提交评分/反馈 | USER | C |
| POST | `/api/feedback/escalate` | 提交至运营处理 | USER | C |
| GET | `/api/feedback/my-records` | 查看我的反馈记录 | USER | C |

### 运营后台 — 知识库管理 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/knowledge` | 知识条目列表（分页/搜索/筛选） | ADMIN | A |
| POST | `/api/admin/knowledge` | 新增知识条目 | ADMIN | A |
| PUT | `/api/admin/knowledge/{id}` | 编辑知识条目 | ADMIN | A |
| DELETE | `/api/admin/knowledge/{id}` | 删除知识条目（软删） | ADMIN | A |
| POST | `/api/admin/documents/upload` | 上传文件 | ADMIN | A |
| GET | `/api/admin/documents/{id}/status` | 查询解析状态 | ADMIN | A |

### 运营后台 — 脱敏审核 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/cases/pending` | 待审核脱敏列表 | ADMIN | A |
| PUT | `/api/admin/cases/{id}/review` | 审核脱敏结果 | ADMIN | A |

### 运营后台 — 反馈管理 (edu-core)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/admin/auth/login` | 运营登录 | 无 | C |
| GET | `/api/admin/feedback` | 反馈列表 | ADMIN | C |
| PUT | `/api/admin/feedback/{id}/status` | 更新反馈状态 | ADMIN | C |
| PUT | `/api/admin/feedback/{id}/resolve` | 回复并完成反馈 | ADMIN | C |

### 运营后台 — 问答审核 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/qa/pending` | 待审核问答列表 | ADMIN | A |
| POST | `/api/admin/qa/approve` | 审核入库 | ADMIN | A |
| GET | `/api/admin/questions/pending` | 用户问题待审列表 | ADMIN | A |

### 运营后台 — 对话日志与分析 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/conversations` | 对话日志列表 | ADMIN | A |
| GET | `/api/admin/analytics` | 统计分析数据 | ADMIN | A |

### 运营后台 — 配置管理 (edu-core)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/config/quick-questions` | 快捷问题配置 | ADMIN | C |
| PUT | `/api/admin/config/quick-questions` | 更新快捷问题 | ADMIN | C |
| GET | `/api/admin/config/role-recommendations/{role}` | 角色推荐内容 | ADMIN | C |
| PUT | `/api/admin/config/role-recommendations/{role}` | 更新角色推荐 | ADMIN | C |
| GET | `/api/admin/config/system` | 系统配置（免责声明/敏感词） | ADMIN | C |
| PUT | `/api/admin/config/system` | 更新系统配置 | ADMIN | C |

### 运营后台 — 推荐问题管理 (edu-core)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| GET | `/api/admin/recommended-questions` | 推荐问题列表 | ADMIN | C |
| GET | `/api/admin/recommended-questions/{role}` | 按角色获取 | ADMIN | C |
| POST | `/api/admin/recommended-questions` | 新增推荐问题 | ADMIN | C |
| PUT | `/api/admin/recommended-questions/{id}` | 编辑推荐问题 | ADMIN | C |
| DELETE | `/api/admin/recommended-questions/{id}` | 删除推荐问题 | ADMIN | C |

### 运营后台 — 平台数据同步 (edu-ai)

| 方法 | 路径 | 说明 | 认证 | 服务 |
|------|------|------|------|------|
| POST | `/api/admin/platform/sync` | 手动触发同步 | ADMIN | A |
| GET | `/api/admin/platform/sync-status` | 同步状态查询 | ADMIN | A |

### 服务间内部接口 (不经Gateway)

| 方法 | 路径 | 说明 | 方向 |
|------|------|------|------|
| POST | `/internal/rag/chat` | RAG问答（SSE流式） | C → A |
| POST | `/internal/filter/check-input` | 敏感词输入检测 | C → A |
| POST | `/internal/cache/invalidate` | 语义缓存失效 | C → A |
| POST | `/internal/documents/{id}/parse-complete` | 文档解析完成回调 | A → C |
| GET | `/internal/config/{key}` | 获取系统配置 | A → C |

## 9.3 SSE 流式接口 (POST /api/chat)

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

## 9.4 认证与权限

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
