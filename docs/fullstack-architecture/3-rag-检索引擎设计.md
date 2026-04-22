# 3. RAG 检索引擎设计

## 3.1 RAG Pipeline 流程

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

## 3.2 检索策略细节

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

## 3.3 语义缓存设计 (FR41)

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

## 3.4 Prompt 模板

**系统提示词 (System Prompt)** — 精简版，~400 tokens 预算

```
你是"校共体AI助手"，基于河南省校共体政策文件回答问题。
用户角色：{role_name}

回答规则：
1. 仅基于【参考资料】回答，无依据时说明"暂无相关政策依据"
2. 关键观点标注引用 [1][2]，末尾列出来源：[编号]《文件名》章节
3. 简洁专业，300-600字，分点阐述
4. 角色侧重：{role_focus}

# 参考资料
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
