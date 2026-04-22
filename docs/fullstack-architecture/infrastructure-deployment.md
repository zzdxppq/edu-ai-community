# 13. 部署方案

## 13.1 部署架构

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

## 13.2 环境要求

| 组件 | 最低配置 | 推荐配置 |
|------|---------|---------|
| 应用服务器（3个JVM进程） | 4C 8G | 8C 16G |
| — Gateway | ~256MB heap | ~512MB heap |
| — edu-core | ~512MB heap | ~1GB heap |
| — edu-ai | ~1GB heap | ~2GB heap |
| PostgreSQL | 2C 4G, 50GB SSD | 4C 8G, 100GB SSD |
| Redis | 1C 2G | 2C 4G |
| 模型API | 外部调用（无需GPU） | — |

## 13.3 构建与启动

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
