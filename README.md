# WorkLens

WorkLens 是一个用于采集员工桌面应用使用情况、向员工展示个人使用视图、向管理者展示团队聚合视图，并自动生成日报/周报/月报的原型系统。

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、PostgreSQL
- 前端：Vue 3、TypeScript、Vite、Vue Router、Vitest
- 桌面客户端：Python、Windows 前台应用检测、系统托盘、本地 SQLite 重试缓存
- 数据库：PostgreSQL 16，本地通过 Docker Compose 启动
- LLM：后端通过 `LlmProvider` 接入 DeepSeek 兼容的 Chat Completion API

## 项目结构

```text
.
|-- compose.yml                     # 本地 PostgreSQL 服务
|-- .env.example                    # 本地数据库环境变量示例
|-- docs/                           # 设计说明和阶段记录
|-- worklens_backend/               # Spring Boot API、报告生成、数据库 schema
|-- worklens_frontend/              # 员工和管理者使用的 Vue Web 应用
`-- worklens_desktop_client/        # Windows 桌面采集和同步客户端
```

## 本地启动

### 1. 启动 PostgreSQL

在仓库根目录执行：

```powershell
docker compose --env-file .env.example -f compose.yml up -d
```

查看容器状态：

```powershell
docker ps
```

如果需要重建本地数据库卷：

```powershell
docker compose -f compose.yml down -v
docker compose --env-file .env.example -f compose.yml up -d
```

### 2. 启动后端

在启动 Spring Boot 的同一个 shell 中设置数据库环境变量：

```powershell
cd worklens_backend
$env:WORKLENS_DB_HOST='127.0.0.1'
$env:WORKLENS_DB_PORT='5432'
$env:WORKLENS_DB_NAME='worklens'
$env:WORKLENS_DB_USERNAME='worklens'
$env:WORKLENS_DB_PASSWORD='change-me'
$env:WORKLENS_DEEPSEEK_API_KEY='your-deepseek-api-key'
.\mvnw.cmd spring-boot:run
```

其余可选 LLM 配置：

```powershell
$env:WORKLENS_DEEPSEEK_BASE_URL='https://api.deepseek.com'
$env:WORKLENS_DEEPSEEK_MODEL='deepseek-v4-flash'
$env:WORKLENS_DEEPSEEK_CONNECT_TIMEOUT='5s'
$env:WORKLENS_DEEPSEEK_READ_TIMEOUT='15s'
```

健康检查：

```text
GET http://localhost:8080/health
```

预期返回：

```text
ok
```

后端启动时会执行 `schema.sql`，自动创建或补齐所需表结构。

### 3. 启动前端

```powershell
cd worklens_frontend
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

Vite 开发服务器会把 `/api/*` 代理到 `http://127.0.0.1:8080`。

### 4. 启动桌面客户端

安装 Python 依赖：

```powershell
python -m pip install -r worklens_desktop_client/requirements.txt
```

以系统托盘方式运行：

```powershell
pythonw -m worklens_desktop_client.tray_app
```

在控制台中持续采集并同步：

```powershell
python -m worklens_desktop_client.run_sync_client --base-url http://localhost:8080
```

手动上传一条验证记录：

```powershell
python -m worklens_desktop_client.manual_report --base-url http://localhost:8080
```

只在本地采集并输出合并结果，不上传：

```powershell
python -m worklens_desktop_client.collect_activity
```

## 核心功能

### 登录与账号体系

- 员工使用员工工号登录。
- 新员工和被重置密码的员工会获得独立、安全随机的临时密码。
- 新账号或重置后的账号会被标记为 `mustChangePassword=true`。
- `mustChangePassword=true` 的账号可以登录，但在 Web 端改密前不能访问业务 API。
- 员工新设置的密码不会在改密成功页回显。
- 管理者创建账号或重置密码后，只展示后端本次生成的临时密码。

### 员工视角

员工只能查看自己的数据。

“我的使用明细”页面有两种展示模式：

- 当天尚未生成日报前，展示实时应用卡片。每张卡片按应用名聚合，展示累计时长，并可展开查看使用时间段。
- 历史日期展示覆盖该日期的报告。随着滚动清理推进，可能展示日报、周报或月报。

实时卡片视图规则：

- 按应用聚合；
- 所有使用片段都计入累计值；
- 写入记录时沿用 15 秒合并规则；
- 每页最多展示 10 个应用卡片。

### 管理者视角

管理者默认只能查看团队聚合数据。

团队报告和团队摘要不得暴露个体明细。团队报告的 `detail_json` 只包含：

- 应用名；
- 周期内累计时长；
- 周期内占比。

团队报告不得包含员工姓名、员工工号、用户名、原始记录 ID、单条记录开始/结束时间，或任何按员工拆分的行。

### 审计授权流程

管理者不能直接浏览员工个人明细。

授权流程：

1. 管理者提交明细查看申请并填写原因。
2. 目标员工查看申请。
3. 员工批准或拒绝申请。
4. 批准后，管理者获得一次性查看权限。
5. 管理者实际查看时写入审计日志。
6. 授权使用后立即失效。

审计授权后的页面也遵循同一展示规则：

- 当天尚未归档：实时应用卡片；
- 历史日期：覆盖该日期的日报、周报或月报。

### 桌面采集客户端

Windows 桌面客户端当前行为：

- 每 5 秒采样一次当前前台进程；
- 只采集应用名或进程名；
- 不采集窗口标题；
- 不采集浏览器 URL 或域名；
- 连续 5 分钟没有键鼠输入时记为 `Idle`；
- `Idle` 作为独立应用桶记录；
- 每 5 分钟批量上传；
- 上传失败时写入本地 SQLite，恢复后自动重试；
- 托盘状态显示运行中/已停止，并显示当前登录用户姓名。

员工应先在 Web 端完成首次登录和改密，再启动桌面客户端。如果后端返回需要改密，桌面客户端会保留本地缓存，并在日志中提示必须先到 Web 端完成改密。

## 自动报告体系

报告由系统自动生成，员工和管理者不再点击按钮生成报告。

### 日报

- 生成时间：每天 `23:55`，时区 `Asia/Hong_Kong`。
- 来源数据：当天的 `usage_records`。
- 生成内容：员工日报和团队日报。
- 清理规则：报告插入成功后，删除对应原始 `usage_records`。

### 周报

- 生成时间：每周日 `23:55`，时区 `Asia/Hong_Kong`。
- 周期：自然周，周一到周日。
- 来源数据：该周日报。
- 生成内容：员工周报和团队周报。
- 清理规则：报告插入成功后，删除对应日报。

### 月报

- 生成时间：每月最后一天 `23:55`，时区 `Asia/Hong_Kong`。
- 来源数据：当月内已经生成的周报。
- 生成内容：员工月报和团队月报。
- 清理规则：报告插入成功后，删除对应周报。

月报采用严格层级汇总：月报只汇总已经生成的周报，不回查原始 `usage_records`，也不从日报补齐缺口。因此，当周报边界和自然月边界不完全一致时，月报覆盖范围可能和自然月有少量出入。

### 报告内容

每份报告保存：

- 结构化 `detail_json`：应用名、累计秒数、累计分钟数、占比；
- LLM 生成的自然语言总结；
- 报告范围：`EMPLOYEE` 或 `TEAM`；
- 报告周期：`DAILY`、`WEEKLY` 或 `MONTHLY`；
- 周期开始/结束日期；
- 来源层级和来源数量。

LLM 调用发生在数据库事务之外。短事务只负责插入报告并删除来源数据。如果 LLM 生成失败，不删除来源数据，也不插入半成品报告。

### 手动生成入口

旧手动生成接口仅作为兼容路由保留：

- `POST /llm/employee-report`
- `POST /llm/team-report`

这两个接口仍执行认证和角色校验，但认证通过的用户会收到 `410 Gone`，错误码为 `MANUAL_REPORT_GENERATION_DISABLED`。

## API 概览

除特别说明外，业务 API 都需要有效 token。

### Auth

Login security rules:

- Unknown usernames and incorrect passwords both return `401` with code `INVALID_CREDENTIALS`.
- Five consecutive failed attempts for the same normalized username lock login for 15 minutes and return `429` with code `LOGIN_LOCKED`.
- A successful login clears the failed-attempt state.
- Each account has one active session: a new login revokes all previously issued tokens for that account.
- Accounts with `mustChangePassword=true` receive a token for the web password-change flow, but the desktop collector refuses to start until the password is changed.

- `POST /auth/login`
- `GET /auth/me`
- `POST /auth/change-password`
- `GET /health`

### Employees

仅管理者可访问：

- `POST /employees`
- `GET /employees`
- `GET /employees/{id}`
- `PUT /employees/{id}`
- `DELETE /employees/{id}`
- `POST /employees/{id}/reset-password`

新增员工时会同步创建登录账号，账号名等于员工工号。

### Usage

- `POST /usage-records`：员工上传使用记录。服务端从 token 解析员工身份，不信任客户端传入的员工 ID。
- `GET /usage-records`：员工查看自己的原始记录列表。
- `GET /usage-records/view?date=YYYY-MM-DD&page=1&pageSize=10`：员工使用视图，返回实时卡片或覆盖报告。
- `GET /team-usage-summary`：管理者查看团队聚合摘要。

### Audit Requests

- `POST /detail-access-requests`
- `GET /detail-access-requests`
- `GET /detail-access-requests/targeting-me`
- `PATCH /detail-access-requests/{id}/decision`
- `GET /detail-access-requests/{id}/usage-records`
- `GET /detail-access-requests/{id}/usage-view?date=YYYY-MM-DD&page=1&pageSize=10`
- `GET /detail-access-requests/{id}/access-logs`

### LLM and Report History

- `GET /llm/test-response`：检查 DeepSeek 兼容 LLM 链路。
- `GET /llm/employee-report-history`：当前员工的报告历史。
- `GET /llm/team-report-history`：当前管理者可见的团队报告历史。
- `POST /llm/employee-report`：已禁用的手动生成兼容入口。
- `POST /llm/team-report`：已禁用的手动生成兼容入口。

LLM 失败会返回明确错误：

```json
{
  "code": "LLM_TIMEOUT",
  "message": "DeepSeek API request timed out"
}
```

```json
{
  "code": "LLM_PROVIDER_ERROR",
  "message": "DeepSeek API request failed"
}
```

## 验证命令

后端测试：

```powershell
cd worklens_backend
$env:WORKLENS_DB_HOST='127.0.0.1'
$env:WORKLENS_DB_PORT='5432'
$env:WORKLENS_DB_NAME='worklens'
$env:WORKLENS_DB_USERNAME='worklens'
$env:WORKLENS_DB_PASSWORD='change-me'
.\mvnw.cmd test
```

前端测试：

```powershell
cd worklens_frontend
npm test
```

前端构建：

```powershell
cd worklens_frontend
npm run build
```

桌面客户端测试：

```powershell
python -m unittest discover worklens_desktop_client/tests
```

## 已知限制

- 团队人数过少时，团队聚合数据在数学上可能接近个体明细。当前版本没有实现最小团队人数门槛、小样本隐藏或更强匿名化。
- 桌面客户端只采集应用名或进程名，不采集窗口标题、浏览器 URL 或域名。
- 月报采用严格层级汇总，只汇总已生成周报。周报边界和自然月边界不完全一致时，月报覆盖范围可能和自然月有少量出入。
- 临时密码仅在创建账号或重置密码的响应中返回一次；管理员需通过安全渠道交给对应员工。
- 当前系统仍是原型，没有包含生产部署加固、组织层级、多级审批或运行监控。
