# 万企商服通后端实现与 Review 报告

> 报告日期：2026-09-24  
> 后端版本：0.1.0  
> 运行范围：本地开发与测试环境  
> 结论：环境安装、容器、空库、单元测试、Testcontainers 集成测试和本机接口烟测均已通过。

## 1. 实施结论

本阶段已完成模块化单体后端，正式接口统一使用 `/api/v1`。已删除原 `/api/cases` 和 `/api/files/upload` 模拟接口，没有接入生产微信凭据，也没有使用真实客户资料。

已实现模块：

- 后台账号密码登录、BCrypt、Sa-Token、角色和权限。
- `local` Profile 模拟客户登录；`prod` Profile 强制关闭。
- 企业客户、企业成员、后台账号和角色分配。
- 三类服务、产品、版本化资料模板和发布/下架。
- 报单草稿、11 项/3 项模板快照、开票信息、简表单和文字链接。
- 严格报单状态机、状态日志、逐项审核、补件和通知。
- 私有文件上传、删除、短时预览、访问日志和事务补偿。
- Excel 客户导入、行级错误、去重、脱敏错误报告和 Dry Run。
- 审核 Excel、贸易增量 ZIP、异步任务、条件抢占和宕机恢复。
- 操作日志、文件访问日志、下载日志和后台审计查询。
- 统一响应、分页、参数校验、Request ID、异常处理、Actuator 和 OpenAPI。

## 2. 环境安装结果

| 组件 | 结果 | 版本/说明 |
| --- | --- | --- |
| JDK | 已可用 | OpenJDK 21.0.2 |
| Maven | 已可用 | Scoop Maven 3.9.16；项目已生成 Maven Wrapper |
| MySQL Shell | 已安装 | 8.4.5，安装路径下可直接调用 |
| Docker Desktop | 已安装 | 4.91.0；Docker CLI 29.8.0 |
| Docker Compose | 已安装 | v5.5.1 |
| WSL 可选功能 | 已启用 | `Microsoft-Windows-Subsystem-Linux`、`VirtualMachinePlatform` 均为 Enabled |
| Windows 重启 | 已完成 | 重启后 CBS、Windows Update 待重启标志已清除 |
| Docker Engine | 已可用 | Docker Desktop Linux Engine 29.8.0，`desktop-linux` Context |

当前终端可直接调用：

```text
C:\Program Files\Docker\Docker\resources\bin\docker.exe
C:\Program Files\MySQL\MySQL Shell 8.4\bin\mysqlsh.exe
```

启动前已检查旧 Volume。原有 `mysql`、`valkey`、`seaweedfs` 卷均保留且未挂载到本项目；只删除并重建了本次首次初始化失败后产生、无业务数据的 `infra_mysql_data` 开发卷。

## 3. Review 1：环境与数据库

### 已确认

- Compose 固定 MySQL 8.4、Valkey 8、SeaweedFS 4.06。
- 三个服务均配置健康检查、持久化卷和 `restart: unless-stopped`。
- MySQL 首次初始化按 `001_init.sql`、`002_full_schema.sql` 顺序挂载。
- SeaweedFS S3 配置仅包含本地开发身份，无匿名公开读权限。
- MySQL Shell 实测为 26 张表、3 个服务模块、3 个产品、3 套模板和 17 个模板项。
- `file_object.case_id` 已改为可空，支持无报单归属的导入源文件和错误报告。
- `export_job.request_payload` 保存异步任务请求快照，任务可恢复执行。
- `import_job_error` 使用非保留字 `row_no`，Java 的 `rowNumber` 通过 MyBatis 显式映射。
- SeaweedFS 4.06 使用 HTTP 主节点状态健康检查，不再调用已删除的 `weed shell -server/-command` 参数。

### 实测结果

- MySQL 8.4、Valkey 8、SeaweedFS 4.06 三个 Compose 服务均为 `healthy`。
- `001_init.sql`、`002_full_schema.sql` 已在全新 MySQL 8.4 开发卷和 Testcontainers 临时库中完整执行。
- MySQL Shell 查询结果为 `26 / 3 / 3 / 3 / 17`。
- SeaweedFS 的 Master、Volume、Filer 和 S3 均启动，集成测试完成真实上传、读取和导出。
- 容器最近日志无持续性 `ERROR`、`FATAL` 或 `panic`；SeaweedFS 主节点选举前的短时连接重试属于正常启动过程。

复查命令：

```powershell
docker volume ls
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs --tail 100
```

数据库验收查询：

```sql
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wqst';
SELECT COUNT(*) FROM service_module;
SELECT COUNT(*) FROM service_product;
SELECT COUNT(*) FROM material_template;
SELECT COUNT(*) FROM material_template_item;
```

本次实测依次为 `26、3、3、3、17`。

## 4. Review 2：框架、认证与权限

### 通过项

- 后台固定权限接口使用 Sa-Token `@SaCheckPermission`/`@SaCheckRole`，Controller 保留显式场景检查；状态机、加密、文件和导出规则位于 Service。
- 后台密码使用 BCrypt cost 12；初始化密码少于 12 位会拒绝启动初始化。
- 不提供生产默认管理员；只有同时设置两个 `BOOTSTRAP_ADMIN_*` 环境变量才创建。
- 超级管理员权限在每次启动时同步，新增权限不会因账号已存在而漏配。
- 后台和客户端使用不同 Sa-Token 登录场景。
- 登录失败通过 Valkey 限流；Valkey 不可用时失败关闭并返回 `VALKEY_UNAVAILABLE`。
- 管理接口执行权限校验；客户端资源同时检查登录用户与企业成员关系。
- `prod` Profile 的数据库密码、字段加密密钥和 S3 密钥只接受环境变量，不继承本地默认值。
- 全局异常不返回 SQL、堆栈、服务器路径或对象存储密钥。
- 日志未配置请求体或 Token 输出；文件响应不返回对象键和永久 URL。
- MyBatis 统一扫描 `com.wqst.api` 下带 `@Mapper` 的 22 个接口，应用上下文实测可完整启动。

### 微信边界

- `WechatAuthClient` 接口和 `/api/v1/auth/wechat/login` 已保留。
- AppID/AppSecret 未配置时返回 `WECHAT_AUTH_NOT_CONFIGURED`。
- 配置凭据但适配器尚未联调时返回 `WECHAT_AUTH_NOT_IMPLEMENTED`，不会用开发登录冒充真实微信能力。

## 5. Review 3：业务、文件和导出安全

### 状态与数据范围

- 状态机只允许文档规定的流转；`COMPLETED` 和 `CANCELED` 为终态。
- 通用更新接口不能直接修改状态。
- 创建报单验证企业成员关系、产品发布状态和产品/服务类型匹配。
- 提交前检查全部必填资料；补件重新提交后原补件单标记为已解决。
- 客户详情不返回 `internalNote`；客户时间线只暴露补件说明。
- 乐观锁冲突统一返回 409 `OPTIMISTIC_LOCK_CONFLICT`。

### 敏感字段

- 手机号、税号和基本户账号使用 AES-GCM 加密、HMAC-SHA256 查询摘要和掩码值。
- 新代码不写入旧版开票明文字段。
- Excel 错误报告中的电话和信用代码经过脱敏，行快照不保存完整电话。

### 文件

- 上传同时校验扩展名、MIME、大小、文件头；Excel 额外校验 OOXML 必需条目。
- 对象键使用服务端 UUID，不使用客户文件名作为路径。
- 原文件名经过 Unicode 归一化和路径字符清理。
- 上传后的数据库事务回滚会清理对象；删除在数据库提交后清理对象，避免数据库记录指向已提前删除的文件。
- 敏感预览要求独立权限并写入成功/拒绝访问日志。
- 客户端不能预览导入源文件等无报单归属的系统文件。
- 预览和下载只返回短时签名 URL，且下载签名不会超过任务自身过期时间。

### 导入导出

- 创建、提交、补件、导入和导出使用 `Idempotency-Key`；业务事务提交后才缓存成功结果。
- 异步导入/导出使用数据库条件更新抢占，防止多实例重复执行。
- 运行超过 60 分钟且未完成的任务会被恢复为待执行状态。
- 审核 Excel 只包含报单和进度字段，不包含身份证、征信、网银或银行账号原件。
- ZIP 仅允许贸易增量，固定生成 01–11 资料目录并附 `审核表.xlsx`。
- ZIP 条目经过路径清理，防止 Zip Slip；敏感文件需要 `export:zip` 和 `file:sensitive:view`。
- 每次导出下载均写入 `download_log`。

## 6. Review 4：构建与测试

最终命令：

```powershell
cd services/api
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"
.\mvnw.cmd "-Dapi.version=1.40" clean verify
```

结果：`BUILD SUCCESS`。

| 指标 | 结果 |
| --- | ---: |
| 自动化测试 | 54 个单元测试 + 2 个集成测试，全部通过，0 跳过 |
| 全项目指令覆盖率 | 69.37% |
| 全项目分支覆盖率 | 56.67% |
| 核心类指令覆盖率 | 89.95% |
| 核心类分支覆盖率 | 74.09% |

核心类统计范围：`CaseService`、`ReviewService`、`CaseStateMachine`、`CryptoService`、`SafeFiles`、`FileInspector`、`ImportWorker`、`ExportWorker`。全项目口径包含 Controller、配置、实体和简单数据对象，因此不以全项目覆盖率代替核心分支门槛。

已覆盖的高风险场景：

- 状态机合法流转、跨级流转和终态拒绝。
- 报单创建 11 项快照、必填校验、开票/简表单、文字链接和文件资料状态。
- 企业数据范围拒绝。
- 审核乐观锁、终态拒绝、补件资料归属和通知。
- AES-GCM 随机 IV、解密、摘要和掩码。
- 扩展名、MIME、文件头、大小和真实 Excel 容器。
- ZIP 路径清理、11 个资料目录、水母链接和审核表。
- Excel 导入成功、重复、错误行、表头错误和安全错误说明。
- 导出任务条件抢占、成功和失败状态。

### 集成测试实测

`BackendIntegrationIT` 的 2 个测试已实际启动独立 MySQL、Valkey、SeaweedFS 容器并通过，未跳过。覆盖：

- 26/3/3/3/17 种子数量、唯一约束和事务回滚。
- 本地客户与后台管理员登录。
- 贸易增量 11 项资料、开票信息、文件上传和报单提交。
- 审核补件、重新提交、处理中和完成状态流转。
- 跨企业访问返回 403。
- Excel 混合导入、审核 Excel、资料 ZIP 和短时下载地址。

## 7. 前端联调信息

### 地址

```text
API:     http://127.0.0.1:8080/api/v1
Swagger: http://127.0.0.1:8080/swagger-ui.html
Health:  http://127.0.0.1:8080/actuator/health
```

### 后台账号

不提供默认密码。首次启动前设置：

```powershell
$env:BOOTSTRAP_ADMIN_USERNAME = "自定义管理员用户名"
$env:BOOTSTRAP_ADMIN_PASSWORD = "自定义强密码（至少12位）"
```

随后调用 `POST /api/v1/auth/admin/login`。

### 本地客户账号

`local` Profile 会创建模拟账号 `local_client` 和模拟企业。调用：

```http
POST /api/v1/auth/client/dev-login
Content-Type: application/json

{}
```

该能力在 `prod` Profile 中关闭。

## 8. 已知限制与后续项

1. 微信真实登录等待正式 AppID/AppSecret 和微信服务端联调。
2. 当前文件校验包含类型、MIME、魔数和 OOXML 结构，不包含病毒扫描；生产上线前建议接入 ClamAV 或云查毒。
3. 本阶段不部署生产环境，`local` Compose 密码只能用于本机开发。
4. `002_full_schema.sql` 是在 `001_init.sql` 后执行一次的增量脚本，不应在同一旧库上无条件重复执行。
5. 管理后台报单、客户、业务模板、资料审核、Excel 导入和 Excel/ZIP 导出页面已接入真实接口；导出任务历史暂按浏览器会话记录的任务 ID 展示，后端分页历史接口待后续补充。
6. 当前 Docker Engine 的最低 API 为 1.40，Testcontainers 命令必须传 `-Dapi.version=1.40`，否则测试会因 Docker 400 被跳过。

## 9. 重启后的最终验收清单

- [x] `wsl --status` 正常并使用 WSL2。
- [x] Docker Engine 与 Compose 可连接。
- [x] 启动前确认并保留旧 Volume。
- [x] 三个容器均为 healthy。
- [x] 空库完整执行 001、002 SQL。
- [x] MySQL Shell 查询得到 26/3/3/3/17。
- [x] Actuator 返回 `UP`。
- [x] Swagger 可打开，OpenAPI 实测包含 50 个路径。
- [x] 后台登录、客户开发登录可用。
- [x] 完成上传、提交、审核、补件、Excel 和 ZIP 的真实闭环。
- [x] Testcontainers 2 个集成测试实际通过，0 跳过。

## 10. OpenAPI 与前端代码生成 Review（2026-09-24）

本轮将 OpenAPI 从“可打开的默认文档”升级为前后端共享的中文契约：

- `OpenApiConfig` 统一维护中文标题、14 个中文 Tag、54 个操作摘要、稳定 operationId 和 BearerAuth。
- Controller 中原有 `ApiResponse<?>` 和认证 `Map<String,Object>` 改为明确泛型/record，生成器可以得到具体响应模型。
- `/v3/api-docs` 实测为 50 个路径、54 个操作；JSON 快照位于 `openapi/万企商服通_OpenAPI_V1.json`。
- 根目录提供 `pnpm api:export`、`pnpm api:check`、`pnpm api:generate`、`pnpm api:sync`。
- Orval 生成目录为 `apps/admin-web/src/api/generated/` 和 `apps/client-uni/src/api/generated/`，均禁止手工修改。
- 管理后台生成 API 复用现有 Fetch 请求层；uni-app 生成 API 使用 `uni.request`，文件上传单独使用 `uni.uploadFile`。

本轮已实测：后端 `mvnw clean verify`（54 个单元测试、2 个集成测试，0 跳过）、OpenAPI 校验、Orval 生成、管理后台 `pnpm check`、uni-app 生成 API 独立类型检查和 `OpenApiConfigTest` 均通过。HBuilderX 微信包编译仍需在正式 DCloud 环境完成。

## 11. 独立目录迁移 Review（2026-09-28）

- 本地项目根目录迁移到 `C:\Users\Administrator\Desktop\shopify\wqsf`，不再依赖上层 `shopify-code` 仓库。
- Compose 项目名固定为 `wqsf`，数据迁移到 `wqsf_mysql_data`、`wqsf_valkey_data`、`wqsf_seaweedfs_data` 命名卷，数据库表结构和种子数据未改变。
- 迁移前已在项目外创建 MySQL 逻辑备份和三个 Docker 数据卷快照；备份不纳入 Git。
- 迁移后三个基础设施容器均为 `healthy`，迁移前后数据基线一致。
- 新路径的后端 `clean verify` 通过 54 个单元测试和 2 个集成测试，0 跳过；实际启动 JAR 后 Actuator 返回 `UP`。
- OpenAPI 校验、管理后台检查与构建、uni-app API 类型检查、设计预览构建均通过；生产环境未验证。
