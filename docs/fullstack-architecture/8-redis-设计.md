# 8. Redis 设计

## 8.1 数据结构规划

| Key Pattern | 类型 | 用途 | TTL |
|-------------|------|------|-----|
| `session:{user_id}:{conversation_id}` | List/String | 对话上下文（最近3轮） | 24h |
| `token:blacklist:{jti}` | String | JWT黑名单（登出/刷新后） | 与Token剩余有效期一致 |
| `sms:code:{phone}` | String | 短信验证码 | 5min |
| `sms:limit:{phone}` | String | 短信发送频率限制 | 60s |
| `login:limit:{phone}` | String(计数器) | 登录频率限制 | 5min |
| `semantic_cache:{role}:{bucket}` | Hash | 语义缓存（Embedding相似度匹配） | 7d |
| `cache:invalidate:chunks` | Set | 待失效的chunk_id集合 | — |
| `platform:sync:lock` | String | 平台同步分布式锁 | 30min |
| `platform:sync:last_time` | String | 上次同步的最大publishTime | 永久 |

## 8.2 对话上下文管理 (NFR13 适配)

```
Key: session:{user_id}:{conversation_id}
Value: JSON序列化的消息列表

结构:
[
  {"role": "user", "content": "..."},
  {"role": "assistant", "content": "..."},
  ... (最多6条 = 3轮)
]

约束（适配 Qwen3-32B 5500 token 限制）:
- 传入LLM的对话历史: 最近 3 轮（6条消息），Token ≤ 1500
- 单次对话最大轮数 ≤ 30轮（超出提示开启新对话）
- 超出3轮窗口的历史仍保存在数据库，但不传入LLM
- TTL = 24小时
- Redis不可用时降级从PostgreSQL messages表读取
```

## 8.3 JWT Token 管理

```
登录流程:
1. 验证手机号+验证码
2. 签发 access_token (2h) + refresh_token (7d)
3. access_token 含 {user_id, role, user_type, jti, exp}

刷新流程:
1. 前端在 access_token 剩余5分钟时调用 /api/auth/refresh
2. 验证 refresh_token 有效性
3. 签发新 access_token + 新 refresh_token（滑动过期）
4. 旧 refresh_token 的 jti 加入 Redis 黑名单

登出流程:
1. 当前 access_token 和 refresh_token 的 jti 加入黑名单
2. 黑名单 TTL = Token剩余有效期
```

---
