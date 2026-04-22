# Testing Strategy

## 测试金字塔

```
        ╱╲
       ╱  ╲        E2E / Smoke Test（少量，Phase C 执行）
      ╱────╲
     ╱      ╲      集成测试（API + DB + Redis）
    ╱────────╲
   ╱          ╲    单元测试（业务逻辑，最多）
  ╱────────────╲
```

## 后端测试

### 单元测试

| 项 | 方案 |
|----|------|
| 框架 | JUnit 5 + Mockito |
| 覆盖率目标 | ≥ 70%（核心业务 Service 层） |
| 重点覆盖 | RAG Pipeline、文本切分、脱敏规则、敏感词过滤、JWT签发/校验 |
| Mock 策略 | 外部依赖（模型API、Redis、DB）全部 Mock |

**命名约定：**
```java
@Test
void should_return_top3_chunks_when_search_with_valid_query() { ... }

@Test
void should_reject_input_when_sensitive_word_detected() { ... }
```

### 集成测试

| 项 | 方案 |
|----|------|
| 框架 | Spring Boot Test + Testcontainers |
| 数据库 | Testcontainers PostgreSQL（含 pgvector + zhparser） |
| 缓存 | Testcontainers Redis |
| 重点覆盖 | API 端点全链路、Flyway 迁移、全文搜索、向量检索 |

**测试数据：**
- 使用 `@Sql` 注解加载测试数据集
- 每个测试方法独立事务，测试后自动回滚

### RAG 质量测试

| 指标 | 目标 | 测试方法 |
|------|------|---------|
| 检索召回率 | ≥ 80% | 50-100 条黄金问答对，验证 Top-3 是否包含正确片段 |
| 引用准确率 | ≥ 90% | 验证回答中的 [1][2] 引用标注在检索结果中可追溯 |
| 置信度阈值 | 无幻觉 | 对 20 条域外问题，验证系统声明"暂无相关政策依据" |
| 脱敏完整性 | 100% | 验证 API 响应中不含原始文本（人名/校名/地名） |

**黄金问答集建设：**
- 初始 50 条，覆盖 6 个 Epic 的核心政策问题
- 运营配合标注：问题 → 期望引用文件 → 期望回答要点
- 随知识库扩展持续补充

## 前端测试

| 项 | 方案 |
|----|------|
| 框架 | Vitest + Vue Test Utils |
| 覆盖率目标 | ≥ 60%（核心组件） |
| 重点覆盖 | 对话组件、评分组件、引用跳转、隐私检测、SSE流式渲染 |
| Mock 策略 | API 请求全部 Mock（MSW 或 vi.mock） |

## Phase C: Smoke Test

由 QA agent 在 Phase C 按 Epic 执行：

```
FOR EACH epic:
  QA *smoke-test {epic_id}
    → 启动应用
    → 执行预定义场景（注册→登录→对话→评分→反馈）
    → 验证核心功能可用
    → PASS / FAIL
  IF FAIL → Dev *quick-fix → 重测（最多 3 轮）
```

## CI 集成

```yaml
# 构建流水线中的测试阶段
test:
  steps:
    - mvn test                    # 单元测试
    - mvn verify -Pintegration    # 集成测试（Testcontainers）
    - cd frontend && npm run test # 前端测试
  coverage:
    - jacoco:report               # 后端覆盖率
    - vitest --coverage           # 前端覆盖率
```
