# WorkLens

WorkLens 是一个面向团队的员工应用使用效率分析系统原型。

核心权限边界：
- 管理者默认只看团队聚合后的去标识化趋势，不看单个员工明细
- 员工本人只看自己的完整使用明细

当前仓库是单体项目，包含：
- `worklens_backend`：Spring Boot + MyBatis-Plus + PostgreSQL
- `worklens_frontend`：Vue 3 + TypeScript + Vite
- `compose.yml`：本地 PostgreSQL 开发环境

## 当前完成程度

当前已完成到模块 8，可跑通一条基础闭环：
- 后端可启动，`GET /health` 可用
- PostgreSQL 可通过 Docker Compose 启动
- 员工档案 CRUD 已完成，且仅管理者可访问
- 登录、鉴权、角色识别已完成，所有接口都要求登录态
- 应用使用记录上报已完成，记录归属强制以当前登录身份对应的 `employees.id` 为准
- 管理者团队聚合接口已完成
- 员工个人明细接口已完成，不能通过改参数查看他人数据
- 前端已完成登录后双视角展示：管理者看聚合视图，员工看个人明细视图

当前还不是完整产品，仍属于开发中的原型版本。

## 目录结构

```text
worklens/
├─ compose.yml
├─ .env.example
├─ worklens_backend/
└─ worklens_frontend/
```

## 启动方式

### 1. 启动 PostgreSQL

仓库根目录提供了示例环境变量文件 [.env.example](/E:/code/worklens/.env.example)。

最简单的本地启动方式：

```powershell
cd E:\code\worklens
docker compose --env-file .env.example -f compose.yml up -d
```

启动后可用下面的命令检查容器状态：

```powershell
docker ps
```

如果你本机已经跑过旧版本 PostgreSQL，且密码与当前 `.env.example` 不一致，先重建数据卷再启动：

```powershell
docker compose -f compose.yml down -v
docker compose --env-file .env.example -f compose.yml up -d
```

### 2. 启动后端

后端数据库连接信息从环境变量读取。PowerShell 下可以先按 `.env.example` 设置：

```powershell
cd E:\code\worklens\worklens_backend
$env:WORKLENS_DB_HOST='127.0.0.1'
$env:WORKLENS_DB_PORT='5432'
$env:WORKLENS_DB_NAME='worklens'
$env:WORKLENS_DB_USERNAME='worklens'
$env:WORKLENS_DB_PASSWORD='change-me'
.\mvnw.cmd spring-boot:run
```

启动成功后，可访问：

```text
http://localhost:8080/health
```

预期返回：

```text
ok
```

### 3. 启动前端

```powershell
cd E:\code\worklens\worklens_frontend
npm install
npm run dev
```

启动后默认访问：

```text
http://localhost:5173
```

## 当前主要接口

- `POST /auth/login`
- `GET /auth/me`
- `GET /health`
- `POST /employees`
- `GET /employees`
- `GET /employees/{id}`
- `PUT /employees/{id}`
- `DELETE /employees/{id}`
- `POST /usage-records`
- `GET /usage-records`
- `GET /team-usage-summary`

## 当前使用说明

这个仓库目前没有单独的初始化种子脚本，也没有注册页。

这意味着：
- 数据库表会由后端启动时自动创建
- 但登录账号、员工档案、使用记录，需要你自行准备测试数据
- 现有测试用例里有完整的测试数据构造逻辑，可作为参考

## 已知限制

团队聚合接口虽然不返回员工明细，但当团队人数很少时，聚合结果可能在事实上接近单人明细。

例如：
- 团队只有 1 人时，聚合值就等于该员工自己的数据
- 团队只有 2 到 3 人时，也可能通过上下文推断出个体情况

当前版本还没有加入这类保护机制：
- 最小样本量门槛
- 小样本隐藏
- 更严格的匿名化/去标识化策略

所以目前的“聚合视图不暴露个体”只在接口结构上成立，在小团队场景下仍存在推断风险。这是当前版本明确保留的已知限制。

## 常用验证命令

后端测试：

```powershell
cd E:\code\worklens\worklens_backend
.\mvnw.cmd test
```

前端测试：

```powershell
cd E:\code\worklens\worklens_frontend
npm test
```

前端构建：

```powershell
cd E:\code\worklens\worklens_frontend
npm run build
```
