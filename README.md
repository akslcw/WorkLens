# WorkLens

WorkLens 是一个面向团队的员工应用使用效率分析系统原型。

核心权限边界：
- 管理者默认只能看团队聚合后的、去标识化的趋势数据，不能直接查看员工个体明细
- 员工本人可以查看自己的完整使用明细
- 如果管理者需要查看某个员工的明细，必须先发起申请，并由该员工本人批准
- 批准后的授权仅一次性有效，用后即失效
- 每一次实际查看员工明细的行为都会留下审计记录
- 员工本人可以查看“谁申请过看我的数据、我如何处理、对方是否真的看过”

当前仓库为单体应用，包含：
- `worklens_backend`：Spring Boot + MyBatis-Plus + PostgreSQL
- `worklens_frontend`：Vue 3 + TypeScript + Vite
- `worklens_desktop_client`：Windows Python 桌面采集客户端
- `compose.yml`：本地 PostgreSQL 开发环境

## 当前完成程度

当前已完成到模块 17，后端、前端、桌面客户端基础链路已跑通：
- 后端可启动，`GET /health` 可用
- PostgreSQL 可通过 Docker Compose 一键启动
- 员工档案 CRUD 已完成，且仅管理者可访问
- 用户登录、鉴权、角色识别已完成，所有业务接口都要求登录
- 应用使用记录上报已完成，记录归属强制以服务端解析出的当前登录员工 `employees.id` 为准
- 管理者团队聚合接口已完成
- 员工个人明细接口已完成，不能通过改参数查看其他人的数据
- 前端已完成登录后双视角展示：管理者看聚合视图，员工看个人明细视图
- 审计授权流程已完成：管理者可发起明细查看申请，员工本人可批准或拒绝，批准后管理者可一次性查看明细，并可查询对应访问审计日志
- 管理者可查询自己发起过的全部明细查看申请及其状态
- 员工可查询所有“指向自己”的明细查看申请记录，并区分“已批准但未查看”与“已批准且已查看”
- Windows 桌面采集客户端已完成：
  - 员工账号登录后可真实调用 `/auth/login`
  - 可自动采集当前前台应用的进程名
  - 可将连续使用同一应用的时间段合并成记录
  - 可将长时间无键鼠操作单独记为 `Idle`
  - 默认每 5 分钟批量上报一次
  - 上报失败时会写入本地 SQLite，恢复后自动补传
  - 已提供最小系统托盘图标，显示“运行中/已停止”状态

当前仍是 MVP/原型版本，不是完整产品。

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

仓库根目录提供了示例环境变量文件 [.env.example](./.env.example)。

最简单的启动方式：

```powershell
docker compose --env-file .env.example -f compose.yml up -d
```

启动后可执行：

```powershell
docker ps
```

如果本机已经跑过旧版本 PostgreSQL，且密码与当前 `.env.example` 不一致，先重建数据卷再启动：

```powershell
docker compose -f compose.yml down -v
docker compose --env-file .env.example -f compose.yml up -d
```

### 2. 启动后端

后端数据库连接信息从环境变量读取。PowerShell 示例：

```powershell
cd worklens_backend
$env:WORKLENS_DB_HOST='127.0.0.1'
$env:WORKLENS_DB_PORT='5432'
$env:WORKLENS_DB_NAME='worklens'
$env:WORKLENS_DB_USERNAME='worklens'
$env:WORKLENS_DB_PASSWORD='change-me'
.\mvnw.cmd spring-boot:run
```

启动成功后访问：

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

启动后默认访问：

```text
http://localhost:5173
```

### 4. 启动 Windows 桌面采集客户端

先安装依赖：

```powershell
python -m pip install -r worklens_desktop_client/requirements.txt
```

手动登录并上报一条记录：

```powershell
python -m worklens_desktop_client.manual_report --base-url http://localhost:8080
```

仅本地采集并输出合并后的记录：

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

## 桌面客户端关键参数

当前客户端的默认策略如下：
- 采样频率：每 `5` 秒检测一次当前前台应用
- 采集内容：只采集应用名/进程名，不采集窗口标题，不做域名级追踪
- 空闲判定：连续 `5` 分钟无键鼠操作视为 `Idle`
- 空闲处理：空闲时间单独记录为一条 `Idle` 记录，不跳过
- 上报周期：每 `5` 分钟批量上报一次
- 失败重试：上报失败时写入本地 SQLite，后续恢复连接后自动补传

## 当前主要能力

### 认证与基础权限

- `POST /auth/login`
- `GET /auth/me`
- `GET /health`
- 所有业务接口都要求有效 token，不允许匿名访问

### 员工管理

- `POST /employees`
- `GET /employees`
- `GET /employees/{id}`
- `PUT /employees/{id}`
- `DELETE /employees/{id}`

说明：
- `/employees` 全部增删查改仅允许管理者访问

### 使用记录与双视角查询

- `POST /usage-records`
- `GET /usage-records`
- `GET /team-usage-summary`

说明：
- `POST /usage-records` 仅允许员工上报，且不接受客户端传 `employeeId`
- `GET /usage-records` 仅返回当前登录员工自己的明细
- `GET /team-usage-summary` 仅返回团队聚合统计，不返回任何个体明细

### 审计授权流程

- `POST /detail-access-requests`
- `GET /detail-access-requests`
- `PATCH /detail-access-requests/{id}/decision`
- `GET /detail-access-requests/{id}/usage-records`
- `GET /detail-access-requests/{id}/access-logs`

说明：
- 管理者可发起明细查看申请，并查询自己发起过的申请记录
- 审批人只能是被申请查看的那个员工本人
- 批准后的授权为一次性有效，管理者成功查看一次后即失效
- 若授权已使用，接口返回 `403 Forbidden`，错误文案为 `Detail access authorization has already been used`
- 若授权已过期或不存在，接口返回 `403 Forbidden`，错误文案为 `Detail access authorization is expired or does not exist`
- 每次实际查看员工明细都会写入访问审计日志

### 员工侧查看自身相关访问记录

- `GET /detail-access-requests/targeting-me`

说明：
- 仅允许员工访问
- 只返回“目标员工是当前登录员工本人”的申请记录
- 返回内容包含申请人、申请理由、申请状态、是否已实际查看、查看时间
- 可区分以下几类状态：
  - `PENDING`：已申请，待员工处理
  - `REJECTED`：员工已拒绝
  - `APPROVED` 且 `hasBeenViewed=false`：员工已批准，但管理者还没实际查看
  - `USED` 且 `hasBeenViewed=true`：员工已批准，且管理者已经实际查看过

## 当前使用说明

这个仓库目前没有单独的初始化种子脚本，也没有注册页面。

这意味着：
- 数据库表会由后端启动时自动创建
- 登录账号、员工档案、使用记录、审计申请记录，需要自行准备测试数据
- 现有集成测试和客户端单元测试里包含完整的测试数据构造逻辑，可作为参考

## 已知限制

团队聚合接口虽然不返回员工明细，但当团队人数很少时，聚合结果在数学上可能接近个体明细。

例如：
- 团队只有 1 人时，聚合值就等于该员工自己的数据
- 团队只有 2 到 3 人时，也可能通过上下文推断出个体情况

当前版本还没有加入这类保护机制：
- 最小分组人数门槛
- 小样本隐藏
- 更严格的匿名化/去标识化策略

因此，当前“管理者默认只能看聚合视图”只在接口结构上成立，在极小团队场景下仍存在推断风险。这是当前版本明确保留的已知限制。

## 常用验证命令

后端测试：

```powershell
cd worklens_backend
.\mvnw.cmd test
```

前端测试：

```powershell
cd worklens_frontend
npm test
```

桌面客户端测试：

```powershell
python -m unittest worklens_desktop_client.tests.test_api_client
python -m unittest worklens_desktop_client.tests.test_activity_tracker
python -m unittest worklens_desktop_client.tests.test_sync_service
python -m unittest worklens_desktop_client.tests.test_background_runner
```

前端构建：

```powershell
cd worklens_frontend
npm run build
```
