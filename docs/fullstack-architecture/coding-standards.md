# Coding Standards

## 后端 (Java / Spring Boot)

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 包名 | 全小写，按模块划分 | `com.edu.ai.core.auth.service` |
| 类名 | UpperCamelCase | `ConversationService`, `RagEngine` |
| 方法名 | lowerCamelCase，动词开头 | `findByRole()`, `parseDocument()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_CONTEXT_TOKENS = 1500` |
| 数据库字段 | lower_snake_case | `created_at`, `visible_roles` |
| REST 路径 | kebab-case，资源名复数 | `/api/conversations/{id}/messages` |

### 分层约定

```
controller/   ← 仅做参数校验、DTO转换、调用service
service/      ← 业务逻辑，事务边界
repository/   ← 数据访问，JPA Repository 或自定义SQL
dto/          ← 请求/响应DTO，不暴露Entity
entity/       ← JPA Entity，映射数据库表
```

- Controller 不写业务逻辑，不直接调用 Repository
- Service 之间可互调，但禁止循环依赖
- Entity 不出现在 Controller 层（通过 DTO 隔离）

### 异常处理

- 使用自定义业务异常：`BizException(code, message)`
- 全局异常处理器 `@RestControllerAdvice` 统一返回 `R<T>` 格式
- 禁止 catch Exception 后静默吞掉（必须记录日志或重新抛出）
- 外部服务调用（模型API/平台API）必须设超时 + 降级

### Spring Boot 约定

- 配置优先使用 `application.yml`，环境差异用 profile（dev/prod）
- 敏感配置（API地址、密钥）通过环境变量注入，不硬编码
- 使用 `@Value` 或 `@ConfigurationProperties` 读取配置
- 异步任务使用 `@Async` + 自定义线程池（不用默认池）
- 数据库迁移使用 Flyway，脚本命名：`V{版本号}__{描述}.sql`

### Lombok 使用

- Entity 用 `@Data` 或 `@Getter @Setter`
- DTO 用 `@Data` 或 `@Builder`
- Service/Controller 用 `@RequiredArgsConstructor`（构造器注入）
- 禁止在 Entity 上用 `@Builder`（与 JPA 默认构造器冲突时需 `@NoArgsConstructor`）

### 日志规范

- 使用 SLF4J（`@Slf4j` Lombok 注解）
- 日志级别：ERROR（异常）、WARN（降级）、INFO（关键业务）、DEBUG（调试）
- 禁止使用 `System.out.println`
- 模型API调用必须记录：耗时、token用量、成功/失败

---

## 前端 (Vue 3 / TypeScript)

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件文件 | PascalCase.vue | `ChatBubble.vue`, `AdminLayout.vue` |
| 组合式函数 | use 前缀 | `useChat.ts`, `useAuth.ts` |
| 路由路径 | kebab-case | `/admin/knowledge-manage` |
| 事件名 | camelCase | `@updateRating`, `@submitFeedback` |
| CSS 类名 | kebab-case (BEM可选) | `.chat-bubble`, `.sidebar-item` |

### Vue 3 约定

- **必须使用 Composition API**（`<script setup lang="ts">`），禁止 Options API
- 状态管理使用 Pinia（按功能模块拆分 store）
- HTTP 请求统一封装在 `src/api/` 目录，使用 Axios 实例
- 组件 props 必须定义 TypeScript 类型
- 响应式数据优先使用 `ref()`，复杂对象用 `reactive()`

### 目录结构

```
src/
├── api/          ← API 请求函数（按模块）
├── components/   ← 通用组件（跨页面复用）
├── views/        ← 页面组件（按路由）
├── stores/       ← Pinia stores
├── router/       ← 路由配置
├── utils/        ← 工具函数
├── types/        ← TypeScript 类型定义
└── styles/       ← 全局样式
```

### Element Plus 使用

- 按需引入（`unplugin-vue-components`），不全量导入
- 表单校验使用 Element Plus 内置 `rules`
- 表格分页使用 `el-pagination`，统一分页参数（page/size）

---

## Git 规范

### 提交信息格式

```
<type>(<scope>): <description>

[optional body]
```

**type 枚举：**

| type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构（不改变行为） |
| `docs` | 文档变更 |
| `style` | 格式调整（不影响逻辑） |
| `test` | 测试相关 |
| `chore` | 构建/工具变更 |

**scope 示例：** `auth`, `chat`, `rag`, `knowledge`, `admin`, `frontend`

### 分支策略

- `main` — 稳定分支，保护
- `feat/{story-id}-{short-desc}` — 功能分支，如 `feat/1.1-project-init`
- 完成后 merge 到 main
