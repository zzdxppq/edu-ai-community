# 6. 模型API集成方案

## 6.1 模型清单与部署信息

> **部署环境**：内网机房，vLLM on Ascend NPU，OpenAI 兼容 API，无需 API Key。

| 模型 | Model ID | 地址 | 用途 | max_model_len | 超时 |
|------|----------|------|------|---------------|------|
| Qwen3-32B | `Qwen3-32B` | http://10.91.28.4:22214 | 主推理（问答/改写/脱敏） | **5500** ⚠️ | 60s |
| Qwen3-VL-235B | `Qwen3-VL-235B` | http://10.91.28.4:22237 | 文档图片/流程图识别 | 40960 | 120s |
| Qwen3-Embedding-8B | `Qwen3-Embedding-8B` | http://10.91.28.4:22257 | 文档/查询向量化、语义缓存 | 40960 | 30s |
| Qwen3-Reranker-8B | `Qwen3-Reranker-8B` | http://10.91.28.4:22205 | 检索结果精排 | 40960 | 15s |

**关键约束：Qwen3-32B 上下文仅 5500 tokens**，直接影响：
- 检索片段从 Top-5 降为 **Top-3**
- 对话历史从 5 轮降为 **3 轮**
- 切分片段从 500-1000 降为 **300-500 tokens**
- System Prompt 精简至 **~400 tokens**

## 6.2 统一调用封装

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

## 6.3 配置

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
      dimensions: 4096            # 已确认
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

## 6.4 Rerank 调用协议

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

## 6.5 降级策略

| 组件 | 失败处理 |
|------|---------|
| Qwen3-32B | 重试1次 → 返回友好提示"智能助手暂时繁忙" |
| Qwen3-Embedding-8B | 重试3次 → 标记为pending，后台补偿任务重试 |
| Qwen3-Reranker-8B | 重试1次 → 降级使用RRF融合分数排序 |
| Qwen3-VL-235B | 不重试 → 跳过该图片，记录日志 |

---
