# 阶段 B 一键部署验证记录

验证日期：2026-07-17
验证环境：Windows + Docker Desktop，执行目录为仓库根目录。

## 干净启动

验证前仅清理本项目的 Compose 容器、网络和数据卷，然后在暂时移开本机 `.env` 的情况下执行：

```powershell
docker compose up --build -d
```

本次缓存构建及健康依赖启动总耗时为 20.83 秒。最终状态：

```text
NAME                SERVICE    STATUS
worklens-postgres   postgres   Up (healthy)
worklens-backend    backend    Up (healthy)
worklens-frontend   frontend   Up (healthy)
```

真实启动日志摘录：

```text
worklens-postgres | database system is ready to accept connections
worklens-backend  | HikariPool-1 - Start completed.
worklens-backend  | Tomcat started on port(s): 8080 (http) with context path ''
worklens-backend  | Started WorklensBackendApplication in 4.48 seconds
```

恢复本机 `.env` 后，又删除本项目数据卷并按该文件中的数据库凭据执行了第二次干净启动；缓存构建及依赖启动耗时 14.37 秒，三项服务再次全部达到 `healthy`，登录回归也重新通过。

## HTTP 与登录回归

```text
GET  http://127.0.0.1:8080/health       -> 200 OK
GET  http://127.0.0.1:5173/api/health   -> 200 OK
POST http://127.0.0.1:5173/api/auth/login -> 200
GET  http://127.0.0.1:5173/api/employees  -> 200
```

登录测试使用临时写入干净数据库的本地管理者账号；该账号不属于镜像、迁移脚本或项目默认数据。

- [登录页面](01-login-page.png)
- [成功登录并加载员工列表](02-manager-login-success.png)

## 未配置 DeepSeek Key

Compose 未读取本机 `.env` 时，后端仍能正常启动。认证后调用 LLM 测试接口的真实结果如下：

```text
GET /api/llm/test-response -> 502
{"code":"LLM_PROVIDER_ERROR","message":"DeepSeek API request failed"}
GET /api/health -> 200 OK
```

这证明无真实 Key 时错误只影响 LLM 调用，不会导致整个系统退出。前端生产依赖审计结果为 `found 0 vulnerabilities`。

## 自动化检查

```text
后端：21 个测试套件，97 项测试通过，0 failures，0 errors
前端：10 个测试文件，32 项测试通过
前端：TypeScript 检查与 Vite 生产构建通过
Compose：docker compose config --quiet 通过
```
