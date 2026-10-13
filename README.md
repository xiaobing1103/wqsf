# 万企商服通（wqsf）

万企商服通是面向已合作企业客户的报单与资料协作系统，包含微信小程序、管理后台、Spring Boot API、MySQL、Valkey 和 SeaweedFS。

## 当前阶段

> 最近复核：2026-10-10

项目当前处于“后端、管理后台和小程序主要客户流程已完成本地联调，等待真机与正式环境验收”阶段，尚未完成生产部署与生产验证。

| 模块 | 当前状态 | 下一步 |
| --- | --- | --- |
| 后端 API | 认证、企业、产品、报单、文件、审核补件、导入导出、通知和审计已实现；支持客户密码登录、首次绑定微信和已绑定微信一键登录 | 配置正式微信凭据并进行真机、上线前复验 |
| 管理后台 | 登录、工作台、报单、客户增删改查、客户账号发放/重置密码、团队与角色、模板、审核、导入导出已接真实接口 | 补齐通知中心和账号停用，按需拆分单页组件 |
| 微信小程序 | M01–M06、企业资料、通知、商标/资质咨询和贸易增量资料流程已接真实接口；双登录、退出和切换账号已完成 | 补报单时间线，完成正式微信绑定、真机、HTTPS 域名和客服验收 |
| 数据库与基础设施 | 27 张表及本地 Compose 方案已完成本轮实跑验收 | 上线前重新执行空库、升级库和容器验收 |
| 生产环境 | 未部署、未验证 | 准备正式密钥、网关、监控、备份和发布方案 |

2026-10-09 已使用合成的本地演示企业完成一轮真实接口与管理后台全流程：创建贸易增量报单、上传与提交、审核、补件、重新提交、线下凭证交接、处理中、完成、Excel/ZIP 导出和下载均已跑通。MySQL、Valkey、SeaweedFS、后端和管理后台仍在运行；生产环境未验证。

同日已补齐管理后台“团队与权限”真实页面，以及客户资料列表的新增、详情、编辑和软删除能力。删除企业会保留历史数据，并在存在未完成报单时由后端拒绝操作。2026-10-10 已补齐微信 development 包的本地开发认证、生成 API 运行时兼容和原生导航栏；随后新增客户密码登录、首次微信绑定、已绑定微信一键登录、退出/切换账号和后台客户账号发放。正式微信换取 OpenID 的代码已实现，但仍需正式 AppID/AppSecret 和真机完成外部联调验收。

## 工程目录

```text
wqsf/
├─ apps/client-uni/       # uni-app 小程序
├─ apps/admin-web/        # Vue 管理后台
├─ apps/design-preview/   # 高保真设计预览
├─ services/api/          # Spring Boot API
├─ database/              # 数据库脚本
├─ infra/                 # 本地 Compose 基础设施
├─ openapi/               # OpenAPI 契约快照
└─ docs/                  # 设计、维护和变更文档
```

## 本地启动

需要 JDK 21、Node.js 20 或更高版本、pnpm 10 和 Docker Desktop。

```powershell
docker compose -f infra/docker-compose.yml up -d

cd services/api
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run

cd ../../apps/admin-web
pnpm install
pnpm dev
```

如果 Windows 上启动 Tomcat 时出现 `Unable to establish loopback connection` 或 `Invalid argument: connect`，在同一个后端终端先执行：

```powershell
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp"
.\mvnw.cmd spring-boot:run
```

这是本机 Java NIO 的临时目录兼容处理，不需要修改项目源码。

默认地址：

- 管理后台：`http://127.0.0.1:5174`
- API：`http://127.0.0.1:8080/api/v1`
- Swagger：`http://127.0.0.1:8080/swagger-ui.html`
- Health：`http://127.0.0.1:8080/actuator/health`

小程序 H5 调试：

```powershell
cd apps/client-uni
pnpm install
pnpm dev:h5
```

小程序微信开发者工具调试：

```powershell
cd apps/client-uni
pnpm install
pnpm dev:mp-weixin
```

保持该终端运行，在微信开发者工具中导入：

```text
C:\Users\Administrator\Desktop\shopify\wqsf\apps\client-uni\dist\dev\mp-weixin
```

`apps/client-uni/.env.development` 只让 development 构建中的“微信登录”按钮使用 `/auth/client/dev-login` 模拟已绑定微信；账号密码入口始终调用 `/auth/client/login`。本地合成客户账号为 `local_client`，密码为 `LocalClient-2026!`。正式微信构建会执行 `uni.login -> /auth/wechat/login`，首次使用可在账号登录时勾选“同时绑定当前微信”。项目生成配置已关闭本地开发包的合法域名校验（`urlCheck=false`），电脑模拟器可以访问 `127.0.0.1:8080`。

如果 development 包仍显示“微信登录尚未配置”，先确认打开的是上面的 `dist/dev/mp-weixin`，关闭旧的 `dist/build/mp-weixin` 项目窗口，再在开发项目中执行“清除编译缓存并编译”。不要把生产构建目录当成本地联调包。

真机中的 `127.0.0.1` 指向手机自身，不能访问开发电脑。真机本地调试需要把 `VITE_API_BASE_URL` 设置为电脑局域网 IP、确保手机和电脑在同一网络并放行 Windows 防火墙；正式发布必须改为公众平台配置过的 HTTPS 合法域名。

## 运行状态自检

基础检查：

```powershell
docker compose -f infra/docker-compose.yml ps
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/swagger-ui.html).StatusCode
(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:5174/).StatusCode
```

预期结果为 3 个容器均显示 `healthy`、Health 返回 `UP`、Swagger 和后台均返回 HTTP 200。

管理员接口冒烟：

```powershell
$body = @{
  username = "你的本地管理员用户名"
  password = "你的本地管理员密码"
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Uri http://127.0.0.1:8080/api/v1/auth/admin/login `
  -Method Post -ContentType "application/json" -Body $body

$headers = @{ Authorization = "Bearer $($login.data.token)" }
Invoke-RestMethod http://127.0.0.1:8080/api/v1/auth/me -Headers $headers
Invoke-RestMethod http://127.0.0.1:8080/api/v1/admin/dashboard -Headers $headers
```

登录、当前用户和工作台响应的 `code` 都应为 `OK`。测试后可在前后端终端按 `Ctrl+C`，再执行 `docker compose -f infra/docker-compose.yml down` 停止容器；不要加 `-v`，否则会删除本地数据卷。

## 常用检查

```powershell
# OpenAPI 快照
pnpm api:check

# 管理后台类型检查和生产构建
cd apps/admin-web
pnpm check

# 小程序生成 API 类型检查
cd ../client-uni
pnpm typecheck

# 后端单元测试和 Testcontainers 集成测试
cd ../../services/api
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"
.\mvnw.cmd "-Dapi.version=1.40" clean verify
```

执行后端验证时必须检查 `target/failsafe-reports/`，确认集成测试不是跳过状态。

## 文档入口

- [开发文档索引](./docs/README.md)
- [开发文档总览](./docs/00_开发文档总览.md)
- [前端设计与开发规范](./docs/03_前端设计与开发规范.md)
- [API 接口设计](./docs/04_API接口设计.md)
- [后端实现与 Review 报告](./docs/05_后端实现与Review报告.md)
- [后端维护与 Bug 定位手册](./docs/06_后端维护与Bug定位手册.md)
- [工程目录与修改导航](./docs/07_工程目录与修改导航.md)
- [逐次变更记录](./docs/changes/README.md)

## 安全约束

禁止提交真实客户身份证、征信、银行账号、手机号、微信密钥、数据库生产密码或生产签名密钥。本地开发只使用脱敏测试数据和本地环境变量。
