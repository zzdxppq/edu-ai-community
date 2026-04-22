# 7. 数据库 Schema 设计

## 7.1 PostgreSQL 扩展

```sql
CREATE EXTENSION IF NOT EXISTS vector;      -- pgvector
CREATE EXTENSION IF NOT EXISTS zhparser;    -- 中文分词
CREATE TEXT SEARCH CONFIGURATION zhcfg (PARSER = zhparser);
```

## 7.2 核心表结构

### 用户表

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

### 会话与消息表

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

### 知识库表

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
    embedding       vector(4096),             -- Qwen3-Embedding-8B 输出维度4096
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

-- HNSW向量索引
CREATE INDEX idx_chunks_embedding ON knowledge_chunks
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 200);

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

### 脱敏映射表

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

### 反馈表

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

### 推荐问题表

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

### 配置表

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

### 快捷问题与角色推荐内容

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

### 平台数据同步记录

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

### 操作审计日志

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

## 7.3 ER 关系概览

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
