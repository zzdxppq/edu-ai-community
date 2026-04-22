# 14. 安全设计

| 安全层 | 措施 |
|--------|------|
| 传输层 | 全链路HTTPS，HSTS头 |
| 认证 | Gateway统一JWT鉴权（JwtAuthGlobalFilter），未认证请求直接拒绝 |
| Token管理 | JWT + refresh_token滑动过期 + Redis登出黑名单 |
| 授权 | RBAC (USER / ADMIN)，Gateway解析角色后透传Header，服务内@PreAuthorize |
| 服务间安全 | `/internal/**` 路径Gateway不对外暴露，仅允许服务间直连 |
| 输入校验 | Spring Validation + 自定义校验注解 |
| SQL注入 | JPA参数化查询，禁止拼接SQL |
| XSS | 前端输出编码，CSP头 |
| 敏感数据 | 用户消息脱敏存储，原始内容加密，运营不可见 |
| 频率限制 | Gateway层Redis限流：短信60s/次，登录5min锁定，全局API限流 |
| CORS | Gateway统一CORS配置，仅允许校共体网站域名 |
| 审计 | 关键操作记录 audit_logs |

---
