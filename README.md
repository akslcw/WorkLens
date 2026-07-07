# WorkLens

WorkLens 是一套面向团队的员工应用使用效率分析系统原型。

核心权限边界：
- 管理者默认只能看到团队聚合后的、去标识化的趋势数据，不能直接查看员工个人明细。
- 员工本人只能看到自己的完整明细。
- 如果管理者需要查看某个员工的个人明细，必须先走“申请 -> 员工本人审批 -> 一次性查看 -> 留痕审计”流程。

当前仓库是单体应用，包含：
- `worklens_backend`：Spring Boot + MyBatis-Plus + PostgreSQL
- `worklens_frontend`：Vue 3 + TypeScript + Vite
- `worklens_desktop_client`：Windows Python 桌面采集客户端
- `compose.yml`：本地 PostgreSQL 开发环境

## 当前完成度

当前已完成到模块 22。

已经实现的主链路：
- 登录鉴权，所有业务接口都要求登录，不允许匿名访问
- 员工档案 CRUD，仅管理者可访问；新增员工时会同步创建员工登录账号
- 员工账号使用工号登录，统一初始密码为 `worklens123`，首次登录或被重置后必须先修改密码
- 员工使用记录上报，记录归属强制以服务端解析出的 `employees.id` 为准
- 双视角权限边界
  - 员工只能查看自己的完整明细
  - 管理者只能查看团队聚合数据
- 审计授权流程
  - 管理者可发起个人明细查看申请
  - 只有目标员工本人可以批准或拒绝
  - 批准后授权一次性有效，用后即失效
  - 每次实际查看都会写入审计日志
  - 员工本人可查看“谁申请过看我的数据、我怎么处理、对方是否已实际查看”
- Windows 桌面采集客户端
  - 员工账号登录后自动上报使用记录
  - 每 5 秒采样一次当前前台应用进程名
  - 5 分钟无键鼠操作视为 `Idle`
  - `Idle` 单独记录，不跳过
  - 每 5 分钟批量上报
  - 上报失败写入本地 SQLite，恢复后自动补传
  - 支持最小系统托盘运行方式
- LLM 报告能力
  - DeepSeek API 薄封装接入，不引入 Agent 叙事
  - 员工可主动生成“最近一周”的个人鼓励性总结
  - 管理者可生成仅基于团队聚合数据的团队简报
  - 报告生成历史已落库，可查询
  - LLM 调用超时或上游失败时会返回清晰错误，不是空白 500

## 目录结构

```text
worklens/
|-- compose.yml
|-- .env.example
|-- worklens_backend/
|-- worklens_frontend/
`-- worklens_desktop_client/
```

## 启动方式

### 1. 启动 PostgreSQL

仓库根目录提供了示例环境变量文件 [`.env.example`](./.env.example)。

最简单的启动方式：

```powershell
docker compose --env-file .env.example -f compose.yml up -d
```

确认容器状态：

```powershell
docker ps
```

如果你本机之前跑过旧的数据库卷，且口令与当前 `.env.example` 不一致，可重建卷：

```powershell
docker compose -f compose.yml down -v
docker compose --env-file .env.example -f compose.yml up -d
```

### 2. 启动后端

后端数据库连接信息通过环境变量读取。

```powershell
cd worklens_backend
$env:WORKLENS_DB_HOST='127.0.0.1'
$env:WORKLENS_DB_PORT='5432'
$env:WORKLENS_DB_NAME='worklens'
$env:WORKLENS_DB_USERNAME='worklens'
$env:WORKLENS_DB_PASSWORD='change-me'
.\mvnw.cmd spring-boot:run
```

健康检查：

```text
http://localhost:8080/health
```

预期返回：

```text
ok
```

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

### 4. 启动桌面采集客户端

先安装依赖：

```powershell
python -m pip install -r worklens_desktop_client/requirements.txt
```

手动登录并上报一条记录：

```powershell
python -m worklens_desktop_client.manual_report --base-url http://localhost:8080
```

只做本地采集并输出合并结果：

```powershell
python -m worklens_desktop_client.collect_activity
```

持续采集并按周期自动上报：

```powershell
python -m worklens_desktop_client.run_sync_client --base-url http://localhost:8080
```

以系统托盘方式运行：

```powershell
pythonw -m worklens_desktop_client.tray_app
```

## 桌面客户端使用顺序（重要）

员工拿到初始账号后，必须先在网页端登录并完成首次修改密码，然后再启动桌面采集客户端。

原因：新员工账号和被管理员重置密码后的账号都会处于 `mustChangePassword=true` 状态。这个状态下 `/auth/login` 可以成功，但后端会拒绝 `/usage-records` 等业务接口。桌面客户端会保留本地 SQLite 缓存，不会丢数据，但日志会提示：

```text
Upload blocked: current account must change password in the web app before desktop uploads can continue.
```

正确顺序：

```text
1. 管理员创建员工账号或重置密码
2. 员工打开网页端，用工号和初始密码 worklens123 登录
3. 员工按页面提示修改密码
4. 员工再启动桌面采集客户端
```

如果顺序反过来，桌面客户端会持续采集并缓存数据，但在改密完成前无法成功上报。

## LLM 配置

LLM 报告能力使用 DeepSeek API，API Key 不写入仓库，必须通过环境变量提供。

启动后端前可按需设置：

```powershell
$env:WORKLENS_DEEPSEEK_API_KEY='your-deepseek-api-key'
```

可选配置项：

```powershell
$env:WORKLENS_DEEPSEEK_BASE_URL='https://api.deepseek.com'
$env:WORKLENS_DEEPSEEK_MODEL='deepseek-v4-flash'
$env:WORKLENS_DEEPSEEK_CONNECT_TIMEOUT='5s'
$env:WORKLENS_DEEPSEEK_READ_TIMEOUT='15s'
```

## 当前关键参数

桌面客户端默认策略：
- 采样频率：每 `5` 秒检测一次当前前台应用
- 采集内容：只采集应用名/进程名，不采集窗口标题，不做域名级追踪
- 空闲判定：连续 `5` 分钟无键鼠操作视为 `Idle`
- 空闲处理：空闲时间单独记为 `Idle`
- 上报周期：每 `5` 分钟批量上报一次
- 失败重试：失败写入本地 SQLite，恢复后自动补传
- 上报阻塞提示：如果后端返回 `Password change required`，本地缓存保留，日志明确提示员工先到网页端完成改密

使用记录展示与合并：
- 后端写入 `/usage-records` 时，会检查该员工最近一条记录；如果应用名相同，且上一条结束时间与新记录开始时间间隔在 `15` 秒内，则更新上一条记录的结束时间，不再插入新行
- 员工端个人明细页默认展示 `20` 条记录，点击 `Load more` 继续加载，避免列表无限增长

后端 LLM 默认策略：
- 员工报告时间范围：最近一周
- 员工报告生成方式：员工主动点击生成
- 团队报告输入：只允许团队聚合数据，绝不把个人明细传给 LLM
- 报告历史策略：同一时间段重复生成时，保留多份历史记录，不覆盖旧报告

员工账号默认策略：
- 登录账号：使用员工工号，不使用姓名
- 初始密码：统一为 `worklens123`
- 强制改密：新员工首次登录、管理员重置密码后再次登录，都必须先修改密码
- 管理者重置：管理者可在员工档案页将员工密码重置为统一初始密码

## 主要接口能力

### 认证与基础权限

- `POST /auth/login`
- `GET /auth/me`
- `GET /health`

说明：
- 所有业务接口都要求有效 token

### 员工管理

- `POST /employees`
- `GET /employees`
- `GET /employees/{id}`
- `PUT /employees/{id}`
- `DELETE /employees/{id}`
- `POST /employees/{id}/reset-password`
- `POST /auth/change-password`

说明：
- `/employees` 全部增删查改仅允许管理者访问
- 新增员工会同步创建 `auth_users` 记录，`username` 等于员工工号，角色固定为 `EMPLOYEE`
- `POST /employees/{id}/reset-password` 仅管理者可调用，会把员工密码重置为 `worklens123` 并要求下次登录先改密
- `POST /auth/change-password` 用于完成强制改密，改密前的账号不能访问其他业务页面

### 使用记录与双视角查询

- `POST /usage-records`
- `GET /usage-records`
- `GET /team-usage-summary`

说明：
- `POST /usage-records` 仅允许员工上报，且不接受客户端传 `employeeId`
- `GET /usage-records` 仅返回当前登录员工自己的明细
- `GET /team-usage-summary` 仅返回聚合统计，不返回任何个人明细

### 审计授权流程

- `POST /detail-access-requests`
- `GET /detail-access-requests`
- `PATCH /detail-access-requests/{id}/decision`
- `GET /detail-access-requests/{id}/usage-records`
- `GET /detail-access-requests/{id}/access-logs`
- `GET /detail-access-requests/targeting-me`

说明：
- 管理者可发起明细查看申请
- 只有目标员工本人可以审批
- 授权一次性有效，用后即失效
- 员工可查看指向自己的申请及后续实际查看情况

### LLM 报告

- `GET /llm/test-response`
- `POST /llm/employee-report`
- `GET /llm/employee-report-history`
- `POST /llm/team-report`
- `GET /llm/team-report-history`

说明：
- `GET /llm/test-response` 仅用于验证 DeepSeek 调用链路是否可用
- `POST /llm/employee-report` 仅 `EMPLOYEE` 可调用，基于本人最近一周明细生成报告
- `GET /llm/employee-report-history` 仅 `EMPLOYEE` 可调用，只能看自己的报告历史
- `POST /llm/team-report` 仅 `MANAGER` 可调用，输入只允许团队聚合数据
- `GET /llm/team-report-history` 仅 `MANAGER` 可调用，只能看自己生成过的团队报告历史

### LLM 失败处理

当 DeepSeek 调用失败时：
- 超时返回 `504 Gateway Timeout`
- 上游报错或调用失败返回 `502 Bad Gateway`

返回示例：

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

## 当前使用说明

当前仓库仍是 MVP/原型状态，还没有做：
- 注册页面
- 初始化种子数据脚本
- 生产级部署和监控
- 真实组织架构、多级审批、报表前端页面美化

这意味着：
- 数据库表会在后端启动时自动初始化
- 测试账号、员工档案和使用记录需要自行准备
- 集成测试代码本身就是当前最完整的接口行为样例

## 已知限制

团队聚合接口虽然不返回个人明细，但在团队人数很少时，聚合结果在数学上可能接近个体明细。

例如：
- 团队只有 1 人时，聚合值本质上就等于该员工个人数据
- 团队只有 2 到 3 人时，也可能通过上下文推断出个体情况

当前版本没有做这类保护：
- 最小分组人数门槛
- 小样本隐藏
- 更严格的匿名化/去标识化策略

这是当前版本明确保留的已知限制，本轮不处理。

员工账号初始密码也有一个已知取舍：所有新员工和被重置密码的员工都会短暂共享统一初始密码 `worklens123`。系统会强制这些账号登录后立即修改密码，但在员工完成修改前，理论上存在被他人冒用初始密码登录的窗口期风险。这是当前版本为降低演示和管理复杂度接受的限制。

## 常用验证命令

后端测试：

```powershell
cd worklens_backend
.\mvnw.cmd test
```

前端构建：

```powershell
cd worklens_frontend
npm run build
```

桌面客户端测试：

```powershell
python -m unittest worklens_desktop_client.tests.test_api_client
python -m unittest worklens_desktop_client.tests.test_activity_tracker
python -m unittest worklens_desktop_client.tests.test_sync_service
python -m unittest worklens_desktop_client.tests.test_background_runner
```
