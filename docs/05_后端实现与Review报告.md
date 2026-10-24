# 万企商服通后端实现与 Review 报告

> 报告日期：2026-09-24  
> 最近进度复核：2026-10-14
> 后端版本：0.1.0  
> 运行范围：本地开发与测试环境  
> 最新验收结论：2026-10-14 超级管理员可直接控制七项功能，部署限制已取消。75单元/4个Testcontainers集成测试（含七项API全开/全关）和后台类型/构建通过；OpenAPI快照校验69操作/61路径，新版后端Health UP，配置只读查询7条均可调。浏览器视觉、微信实际操作、真实OCR/客服、真机、平台审核和生产仍待验证；当前功能值未改变。

## 1. 实施结论

2026-10-14 最新方案：超级管理员直接管理七项开关，移除部署清单与FEATURE_RELEASE_NOT_ALLOWED；releaseAllowed仅为兼容字段固定true，不参与有效值计算。权限、原因、二次确认、CAS版本与审计保留；OCR依赖及敏感资料/服务双门槛保留。当前本地开关值不变，未迁移数据库。测试与运行结果见 [本次记录](./changes/2026-10-14_005_超级管理员直接控制功能开关.md)。以下部署清单描述是早先轮次的历史结论，不是当前运行规则。

2026-10-14 新增超级管理员功能开关后台：系统设置 → 小程序功能开关（`/capabilities`），菜单/路由与后端GET/PUT仅允许 SUPER_ADMIN；原因、二次确认、版本冲突回读及审计配套，部署开放限制保留。角色异常明确返回403。本轮后台 TypeScript/Vite构建、75单元测试、4个Testcontainers集成测试通过，均无跳过；临时库验证拥有product:view/edit的普通管理员读写403且配置/版本/审计不变，超级管理员更新和业务闭环通过。新版JAR已重启本地容器，Health UP，超级管理员配置查询7条，匿名值仅品类参考true；未调整本地功能值、未迁移数据库。浏览器后台停在登录页，遵守电脑操作技能禁止自动填写认证表单的限制，未执行桌面/窄屏视觉及弹窗点击验收；微信实操、真机、平台审核和生产未验证。本轮未重构建小程序，上一轮构建结果不作为本轮验证。详见 [本次记录](./changes/2026-10-14_004_超级管理员功能开关后台.md)。

本轮功能开关环境收尾：新版后端已重启且Health UP，匿名能力查询实际返回默认值；OpenAPI已同步并校验69操作/61路径，两端生成产物类型检查及后台构建通过。未用本地业务数据执行开关写入，写入和既有报单只读回归在Testcontainers临时库完成。

2026-10-14 完成审核风险筛查与客户功能开关：新增七项匿名布尔查询、管理员权限/原因/版本/审计配置、部署允许清单、服务目录与客户写入口拦截；风险服务默认关闭，旧报单授权查询保留。新增006配置表并已在本地执行。后端74个单元测试、4个Testcontainers集成测试通过（包含无跳过权限/禁用/冲突/审计与既有只读、业务闭环回归），小程序类型检查、H5/微信构建通过；OpenAPI中文契约同步收尾。未执行浏览器视觉、微信实际操作、真实OCR/客服、真机、平台审核或生产验证。管理员配置本轮为接口，不含后台开关GUI；隐私授权及实际业务资质仍待整改。见 [本轮记录](./changes/2026-10-14_003_审核风险筛查与客户功能开关.md)。

2026-10-14 工作台最近报单布局优化：后台 TypeScript 检查和 Vite 生产构建通过，新增分区行布局、中文服务标签、提交/草稿时间说明与窄屏换行；无后端、接口、数据库和运行配置变化。本轮未执行浏览器视觉验收、单元/集成测试、容器业务闭环或生产验证。见 [布局变更记录](./changes/2026-10-14_002_后台最近报单布局优化.md)。

2026-10-14 完成报单时间、草稿和审核整改：69 单元测试、3 Testcontainers 集成测试通过，无跳过；后台/小程序类型检查与 H5/微信构建通过。本地新版后端 Health UP，历史时间有备份后按日志修复，报单列表新增最近提交时间；真实预览签名 HTTP 200，后台及 H5 图片点击放大通过。新增必填资料未审核通过不可办理/完成的服务端门槛，审核补件重提全链路在临时容器库通过。微信实际图片/文档预览、真机和生产仍待验证。见 [整改记录](./changes/2026-10-14_001_报单时间只读预览与审核流程整改.md)。

2026-10-13 附件选择组件修复：新增 FilePicker 并接入开票附件页，隔离 H5/微信文件选择及响应式参数。小程序完整 TypeScript 检查、H5/微信生产构建，以及两端文件选择参数与格式校验回归通过；development watcher 已生成新组件。本轮未修改后端、数据库和接口，也未重跑后端测试、容器闭环、微信文件选择弹窗/上传、真机或生产验收。详见 [附件选择组件记录](./changes/2026-10-13_007_跨端附件选择组件与微信参数修复.md)。

2026-10-13 手工验收环境已重新启动：MySQL、Valkey、SeaweedFS 均为 healthy，既有 `wqst-api-dev` 容器运行且 Health 返回 UP；管理后台 5174、小程序 H5 5175、OpenAPI 均返回 HTTP 200。管理员和客户密码登录、当前身份、后台工作台及 H5 代理服务目录冒烟成功。微信 development watcher 编译完成。Docker 启动时遇到失效的 `sailor-ingest.sock`，隔离本机两个临时运行目录后恢复，未删除数据库卷。本轮未修改业务代码，未重新执行单元测试、Testcontainers 集成测试、全量手工用例、微信模拟器、真机或生产验证。详见 [本次启动记录](./changes/2026-10-13_006_全服务启动与手工验收准备.md)。

本阶段已完成模块化单体后端，正式接口统一使用 `/api/v1`。已删除原 `/api/cases` 和 `/api/files/upload` 模拟接口，没有接入生产微信凭据，也没有使用真实客户资料。

已实现模块：

- 后台和客户账号密码登录、BCrypt、Sa-Token、角色、权限、独立失败计数和限流。
- 正式微信 code 换取 OpenID/UnionID、客户首次绑定、已绑定微信登录，以及后台客户账号发放和重置密码。
- `local` Profile 模拟客户登录；`prod` Profile 强制关闭。
- 企业客户增删改查、企业成员、后台账号和角色分配。
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

- `WechatAuthClient` 已调用微信 `/sns/jscode2session`，只提取 OpenID/UnionID；微信 `session_key` 不保存、不返回。
- AppID/AppSecret 未配置时返回 `WECHAT_AUTH_NOT_CONFIGURED`。
- 无效 code 返回 `WECHAT_CODE_INVALID`，微信服务或网络异常返回 `WECHAT_AUTH_UNAVAILABLE`。
- 未绑定微信返回 `WECHAT_ACCOUNT_UNBOUND`，不会自动创建没有企业关系的空账号；真实 AppID/AppSecret、微信服务器和真机绑定仍待外部验收。

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
5. 管理后台报单、客户增删改查、团队与角色、业务模板、资料审核、Excel 导入和 Excel/ZIP 导出页面已接入真实接口；导出任务历史暂按浏览器会话记录的任务 ID 展示，后端分页历史接口待后续补充。
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

## 12. 项目进度复核（2026-10-08 历史快照）

### 12.1 当前阶段

项目当前处于“后端与管理后台本地联调完成，小程序业务开发待推进”阶段，尚未进入生产交付。

| 范围 | 复核结论 |
| --- | --- |
| 后端 | 主要业务模块、权限、安全文件、导入导出和审计代码仍存在，未发现被回退为模拟接口的情况 |
| 管理后台 | 登录、工作台、报单、客户、模板、审核、导入和导出页面仍通过领域 API 层调用真实后端接口 |
| 小程序 | 生成 API 和上传适配器已存在，但只有一个业务页，页面主要使用固定示例文案和 Toast，没有接入真实业务请求 |
| 微信登录 | 当时 `/api/v1/auth/wechat/login` 只保留契约，`WechatAuthClient` 尚待实现；当前状态以 Review 27 为准 |
| 生产交付 | 未部署、未验证；正式密钥、网关、监控、备份、病毒扫描和发布回退方案仍需落地 |

### 12.2 当前运行状态

2026-10-08 首次执行只读环境检查时：

- `docker compose -f infra/docker-compose.yml ps` 未返回运行中的项目服务。
- `http://127.0.0.1:8080/actuator/health` 连接被拒绝，后端当时未运行。
- 该时点还没有启动容器，也没有重跑单元测试、集成测试或前端生产构建。
- 2026-09-28 的 54 个单元测试、2 个集成测试、容器健康、Actuator `UP` 和前端检查仍作为最近一次全量历史基线。

同日后续完成本地启动与接口健康检查：

- MySQL 8.4、Valkey 8、SeaweedFS 4.06 三个 Compose 容器均为 `healthy`。
- MySQL 查询确认当前数据库为 `wqst`、共 26 张表；本地启动管理员创建后共有 3 个账号。
- Valkey `PING` 返回 `PONG`，SeaweedFS 主节点状态返回 HTTP 200。
- 后端使用 `local` Profile 在 8080 端口启动，Actuator 返回 `UP`，Swagger 返回 HTTP 200，OpenAPI 包含 50 个路径。
- 管理后台开发服务在 5174 端口启动并返回 HTTP 200，Vite `/api` 代理可正常转发后端请求。
- 本地管理员登录、`GET /api/v1/auth/me`、`GET /api/v1/admin/dashboard` 和经 5174 代理访问 `/auth/me` 均返回业务码 `OK`。
- 本机 Java 21 首次启动出现 `Unable to establish loopback connection`；将当次 Java 进程的 `jdk.net.unixdomain.tmpdir` 指向 `C:\Windows\Temp` 后启动成功，未修改项目源码或配置文件。
- 本次未执行单元测试、Testcontainers 集成测试、前端生产构建和生产环境验证，不能用本次冒烟代替全量验收。

### 12.3 下一阶段建议顺序

1. 先实现小程序登录和当前用户，再依次接入服务目录、报单草稿、文件上传、提交补件和进度查询。
2. 实现微信登录适配器后，使用正式 AppID/AppSecret 在正式 DCloud 编译链和微信真机上验收。
3. 按实际优先级补齐后台通知中心、导出任务历史等缺口，不因为单个文件较大就提前做过度拆分。
4. 上线前重新执行 OpenAPI 校验、前端构建、后端 `clean verify`、空库与升级库验收、安全检查和核心业务冒烟。

## 13. 管理后台首页交互 Review（2026-10-08）

- 管理员身份展示区与退出按钮已拆分，点击头像或姓名不会注销。
- 退出必须经过页面内确认弹窗，取消后保持在 `/dashboard`。
- “运营工作台”已改为可访问的快捷按钮，从 `/cases` 点击可返回 `/dashboard`。
- 首页和侧边栏可见图标已替换为代码内置 SVG，没有引入新的前端依赖。
- 通知功能未接入前不显示未读红点，按钮保留明确的待接入提示。
- `apps/admin-web` 执行 `pnpm check` 通过：TypeScript 无错误，Vite 生产构建成功，共转换 50 个模块。
- 当前本地后端与容器仍在运行，但本次前端修改没有执行后端单元测试、Testcontainers 集成测试或生产验证。

## 14. 管理后台业务模板页面 Review（2026-10-08）

- 根因确认不是后端或数据库异常：三个产品和对应资料模板均可通过现有接口读取，原问题是 `/products` 使用的目录卡片、产品行和模板详情类名没有配套 CSS，且只有较小的“查看模板”文字按钮可点击。
- 服务模块已按宽屏三列、中等宽度两列、窄屏单列展示；产品条目改为整行可点击，并提供悬停、键盘焦点、加载和选中状态。
- 点击产品后会展示对应资料模板、自动定位并聚焦详情；关闭后清除详情和产品选中态。接口路径、请求响应、数据库和权限均未改变。
- `apps/admin-web` 执行 `pnpm check` 通过：TypeScript 静态检查无错误，Vite 生产构建成功，共转换 50 个模块。
- 本地浏览器已实测三列、两列和单列布局，贸易增量 11 项资料加载、产品切换、选中态与关闭操作均正常，控制台无警告或错误。
- 验收时 MySQL、Valkey、SeaweedFS 三个现有容器均为 `healthy`，Actuator 返回 `UP`；本次没有重启或重建容器，也没有修改后端和数据库。
- 未执行后端单元测试、Testcontainers 集成测试或生产环境验证。

## 15. 管理员线下凭证交接整改 Review（2026-10-08）

### 15.1 安全边界和业务结论

- 网银登录密码、U 盾密码、交易密码、短信验证码等认证秘密不允许由小程序提交，也不允许在管理后台、数据库、日志、导出文件或上传文件中保存。
- 管理后台只为贸易增量报单登记线下交接状态，支持“待交接、已线下交接、已核验、已归还、无需交接”五种状态。
- 新表 `credential_handover` 只保存报单、三类交接状态、经办管理员、交接/核验时间和乐观锁版本，不包含密码、密文、密码文件或自由备注字段。
- 管理员操作入口为“报单管理 → 贸易增量报单详情 → 线下凭证交接”，并受 `case:credential-handover` 权限控制；小程序没有对应入口。
- 企业资料收集统一提供 `docs/templates/企业入库信息表.xlsx`，只包含开票信息和基础登记信息，不包含任何密码填写项。

### 15.2 接口、数据库和契约

- 新增 `GET /api/v1/admin/cases/{caseId}/credential-handover` 查询交接状态。
- 新增 `PUT /api/v1/admin/cases/{caseId}/credential-handover` 保存交接状态，使用 `version` 做乐观锁校验。
- 新增迁移 `database/003_credential_handover.sql`，并加入 Compose 初始化脚本挂载；本地现有数据库已执行迁移。
- OpenAPI 已更新为 51 个路径、56 个操作，并重新生成管理后台和 uni-app 两端类型与端点代码。

### 15.3 本轮验证结论

| 验证类型 | 结果 |
| --- | --- |
| 静态检查 | OpenAPI 校验与生成同步通过；管理后台 TypeScript 检查和生产构建通过；uni-app 生成 API 类型检查通过 |
| 单元测试 | 58 个通过，0 跳过 |
| 集成测试 | 2 个 Testcontainers 测试通过，0 跳过；实际启动独立 MySQL、Valkey 和 SeaweedFS 容器 |
| 容器实跑 | 本地 MySQL、Valkey、SeaweedFS 均为 healthy；后端 Health 为 `UP`；管理后台 5174 返回 HTTP 200；数据库共 27 张表 |
| 浏览器验收 | 未执行。自动化工具被本地环回地址安全策略阻止，不能写成点击验收通过 |
| 生产验证 | 未执行 |

Windows 本机完整后端验证命令如下；`JAVA_TOOL_OPTIONS` 用于规避本机 Java NIO 连接临时 Valkey 时的环回异常，不是业务逻辑配置：

```powershell
cd services/api
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp"
.\mvnw.cmd "-Dapi.version=1.40" clean verify
```

## 16. 本地演示数据全流程验收（2026-10-09）

### 16.1 演示数据边界

- 本地客户显示名：`星澜贸易演示用户`。
- 本地企业名称：`长沙星澜数字贸易有限公司（本地演示）`。
- 报单编号：`BD20261009111421ED76`，服务类型为 `TRADE_INCREMENT`，月份为 `2026-10`。
- 联系方式、税号、银行账号和邮箱全部使用不可用于生产的合成值，没有使用真实客户资料。
- 9 个文件资料使用项目内安全图片验证上传链路，只用于接口和流程验收，不代表真实证件或业务资料内容通过真实性审核。

### 16.2 实际执行链路

```text
本地客户开发登录
  -> 创建贸易增量草稿
  -> 填写开票信息
  -> 上传 9 份文件并填写水母报告链接
  -> 首次提交（PENDING_REVIEW）
  -> 管理后台审核通过 10 项
  -> 营业执照发起补件（NEED_SUPPLEMENT）
  -> 客户删除旧演示文件、重新上传并再次提交
  -> 管理后台完成第 11 项审核
  -> 保存线下凭证交接状态
  -> PROCESSING
  -> COMPLETED
  -> 生成并下载审核表 Excel 和资料 ZIP
```

线下交接最终状态：网银凭证 `HANDED_OVER`、U 盾 `VERIFIED`、交易授权凭证 `NOT_REQUIRED`。系统仍然没有接收或保存任何密码、验证码或密钥文件。

### 16.3 验证证据

| 验证类型 | 2026-10-09 实际结果 |
| --- | --- |
| 后端真实接口 | Health 为 `UP`；客户登录、报单创建、资料上传、提交、补件、重新提交和时间线接口通过 |
| 管理后台浏览器 | 页面实际显示并完成 11 项审核、补件、交接状态、处理中、完成、导出和下载操作 |
| 数据库一致性 | 报单为 `COMPLETED`；11 项资料均为 `SUBMITTED/PASSED`；补件单为 `RESOLVED`；状态日志完整记录 6 个节点 |
| 文件存储 | 9 个当前文件为 `READY`，被补件替换的旧演示文件为 `DELETED` |
| 导出 | `REVIEW_EXCEL` 和 `MATERIAL_ZIP` 均为 `SUCCEEDED`，下载次数均为 1，下载日志均为 `SUCCESS` |
| 审计 | 创建、开票、两次提交、补件、三次状态变更和线下交接均有操作日志 |
| 容器实跑 | MySQL、Valkey、SeaweedFS 均为 `healthy`；管理后台 5174 返回 HTTP 200 |
| 单元测试 | 本轮未重新执行；最近一次结果仍为 58 个通过、0 跳过 |
| Testcontainers 集成测试 | 本轮未重新执行；最近一次结果仍为 2 个通过、0 跳过 |
| 生产验证 | 未执行 |

首次使用 PowerShell `-Form` 上传 PNG 时，请求被后端以 `FILE_MIME_MISMATCH` 正确拒绝，因为客户端把文件标记为 `application/octet-stream`。改为显式发送 `Content-Type: image/png` 后，9 个文件全部通过扩展名、MIME 和文件头校验。这证明文件安全校验实际生效。

### 16.4 当前本地状态和限制

- 演示数据保留在本地 `wqsf_mysql_data` 和 `wqsf_seaweedfs_data` 中，重启服务后仍可在后台查看。
- 管理后台工作台当前显示 1 个已完成报单，待审核、待补资料和处理中均为 0。
- 本轮验证的是本地后端和管理后台，不代表小程序真实页面、微信登录、真机或生产环境已经通过。
- 如需回退，应先确认仅删除本报单 `BD20261009111421ED76` 及其关联演示文件、日志和导出任务，再恢复本地种子企业/客户显示名；不要删除 Docker Volume 或影响其他本地数据。

## 17. 团队权限与客户资料维护 Review（2026-10-09）

### 17.1 实现结论

- 管理后台新增 `/team` 页面和侧边栏入口，仅 `SUPER_ADMIN` 可见且可访问。
- 团队页面使用真实账号接口，支持查看后台成员、新建管理员、调整岗位角色和预览当前权限；当前登录账号不能在页面修改自身角色。
- 客户资料列表的三点图标已改为真实操作菜单，提供查看详情、编辑资料和删除客户；页面顶部增加“新增客户”。
- 编辑企业使用 `version` 做乐观锁；信用代码和联系电话不回显明文，留空时保持原值。
- 删除企业是软删除，只允许超级管理员执行；存在未完成报单时返回 `COMPANY_ACTIVE_CASE_EXISTS`，历史报单、文件和日志不删除。
- 企业新增、编辑和软删除均写入操作日志；本轮没有新增表、字段、索引或种子数据。

### 17.2 接口与契约

新增接口：

```text
GET    /api/v1/admin/companies/{companyId}
PATCH  /api/v1/admin/companies/{companyId}
DELETE /api/v1/admin/companies/{companyId}?version={version}
```

OpenAPI 当前为 52 个路径、59 个操作，已重新生成管理后台和 uni-app 两端代码。账号接口路径没有变化，管理后台新增 `api/accounts.ts` 作为团队页面的领域适配层。

### 17.3 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 静态检查 | `pnpm api:check` 通过；管理后台 `pnpm check` 通过，TypeScript 无错误且 Vite 生产构建成功；uni-app 生成 API `pnpm typecheck` 通过；`git diff --check` 和 Markdown 本地相对链接检查通过 |
| 单元测试 | Maven Surefire 61 个通过，0 失败，0 跳过；新增企业版本冲突、未完成报单阻止删除和软删除测试 |
| 集成测试 | 2 个 Testcontainers 测试通过，0 失败，0 跳过；真实验证团队账号创建/角色更新、企业新增/详情/编辑/软删除 |
| 容器实跑 | 本地 MySQL、Valkey、SeaweedFS 均为 `healthy`，Actuator 为 `UP`，管理后台 5174 为 HTTP 200，Swagger 返回重定向到 UI |
| 浏览器验收 | 后端重启后原浏览器标签登录态失效，只确认登录页可访问；未输入、读取或绕过管理员密码，团队页和客户操作菜单需管理员重新登录后手工点击复验 |
| 生产验证 | 未执行 |

### 17.4 已知限制

- 当前账号接口没有账号停用、删除和密码重置能力，因此团队页不提供这些伪操作。
- 岗位角色来自固定种子 `OPERATIONS`、`REVIEWER`、`SUPER_ADMIN`；暂不提供自由组合权限的角色编辑器。
- 管理后台业务仍集中在 `AdminLayout.vue`，功能可用但文件较大；后续按业务优先级拆分，不在本轮过度重构。

## 18. 客户操作菜单样式修复 Review（2026-10-09）

- 客户列表的三点按钮和菜单已从表格单元格定位改为独立相对定位容器，避免浏览器表格布局导致菜单项横向散落。
- 菜单底部与三点按钮对齐并向左上方展开，使用纵向网格排列“查看详情、编辑资料、删除客户”，避免遮挡分页；操作列保留固定宽度，接口、权限、数据库和点击逻辑没有变化。
- `apps/admin-web` 执行 `pnpm check` 通过：TypeScript 无错误，Vite 生产构建成功，共转换 51 个模块。
- 后台当前登录态已失效，浏览器只确认到登录页；未读取、输入或绕过管理员密码，登录后的视觉点击复验和生产验证未执行。

## 19. 小程序基础架构与首批页面 Review（2026-10-09）

### 19.1 实现结论

- 补齐与 DCloud 编译器同版本的 `uni-h5`、`uni-mp-weixin` 和类型依赖，原 `isInSSRComponentSetup` 导出错误已消失。
- 根 `App.vue` 只承担生命周期和全局样式，`main.ts` 注册 Pinia；旧单页静态原型已拆分为 M01–M06 约定路由。
- H5 调试通过本地客户开发登录建立会话，微信端保留 `uni.login -> /auth/wechat/login` 适配；Token 统一写入 `wqst_client_token`。
- M01 从真实接口读取当前用户、企业、已发布服务和最近报单，并实现加载、空数据、局部失败及重试。
- M02 从真实服务目录读取产品和资料数量，可在 3 个已发布服务之间切换；贸易增量进入 M03，知识产权和资质服务进入 M05。
- M03–M06 本轮仅注册路由和明确的阶段占位，不声称文件上传、简表提交、报单分页或通知功能已完成。
- H5 使用 `/api/v1` 同源代理到本地 `8080`，解决浏览器从前端端口直连后端导致的跨域请求失败。

### 19.2 实跑数据

本地浏览器使用现有合成演示账号和企业，不包含真实客户敏感信息：

| 数据 | 实际读取结果 |
| --- | --- |
| 当前客户 | 星澜贸易演示用户 |
| 当前企业 | 1 家 |
| 服务模块 / 产品 | 3 个模块 / 3 个已发布产品 |
| 最近报单 | 1 笔，资料完成度 11/11，状态已完成 |

### 19.3 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 静态检查 | `apps/client-uni` 执行 `pnpm typecheck` 通过；同时覆盖完整 Vue 应用和生成 API 两个 TypeScript 配置 |
| 前端构建 | `pnpm build:h5` 通过；`pnpm build:mp-weixin` 通过，产物位于 `dist/build/mp-weixin` |
| 接口联调 | Actuator 为 `UP`；本地客户登录、`auth/me`、`companies/my`、`services/catalog`、`cases/my` 均真实请求通过 |
| 浏览器验收 | 390×844 H5 视口通过：M01 显示真实用户、企业、3 个服务和最近报单；M02 服务切换、贸易增量到 M03、其他服务到 M05 路由通过；修复 H5 跨域后未出现新的请求错误 |
| 容器实跑 | `docker compose -f infra/docker-compose.yml ps` 显示 MySQL、Valkey、SeaweedFS 均为 `healthy` |
| 单元测试 | 本轮未执行；后端代码和数据库未改，最近一次结果仍为 61 个通过 |
| Testcontainers 集成测试 | 本轮未执行；最近一次结果仍为 2 个通过 |
| 微信开发者工具 / 真机 | 未执行；缺少正式 AppSecret、HTTPS 合法域名配置和本轮微信工具导入验收 |
| 生产验证 | 未执行 |

### 19.4 下一阶段和限制

- M03 需要接入产品资料模板、报单草稿、资料项状态和提交前刷新。
- M04 需要接入结构化开票表单、`uni.uploadFile`、删除和失败重试。
- M05 只允许法人姓名、公司名称和联系电话三项，不得扩展网银、U 盾、交易密码或验证码输入。
- M06 需要接入分页、补件说明、时间线和通知；首页通知按钮当前明确提示“后续批次接入”。
- 微信正式环境必须通过环境变量配置可访问的 HTTPS API 地址，并完成微信开发者工具及真机验证后才能描述为正式联调通过。

## 20. 小程序贸易增量报单闭环 Review（2026-10-09）

### 20.1 实现结论

- M02 对贸易增量服务创建或恢复当月草稿；恢复时精确匹配企业、产品、月份和可编辑状态，避免多企业账号误打开其他草稿。
- M03 从报单详情和产品模板动态渲染 11 项资料，不在前端硬编码资料数量；提交前重新拉取详情并由后端再次校验必填项。
- M04 保存 8 项开票字段，敏感值保存后只读取掩码；9 类附件通过 `uni.uploadFile` 发送到私有存储，水母报告使用 URL 资料项保存。
- M06 使用真实分页接口展示报单、状态统计、资料完成度和补件提示；已提交报单只能查看，不能继续修改。
- 创建和提交使用不同的 `Idempotency-Key`；本地只缓存草稿编号与幂等键，不保存敏感表单或文件内容。

### 20.2 本地实跑数据

本轮只使用合成演示企业和项目内安全图片，没有录入真实客户证件、征信、银行账号或生产密钥：

| 数据 | 实际结果 |
| --- | --- |
| 报单编号 | `BD20261009171053A6FB` |
| 资料完成度 | 从 0/11 更新为 11/11 |
| 附件 | 9 类资料各使用 `apps/design-preview/public/screens/M01.png` 验证上传 |
| URL | `https://example.com/demo-report` |
| 提交后状态 | `PENDING_REVIEW`，小程序显示“待审核” |
| M06 统计 | 全部 2、处理中 1、待补资料 0、已完成 1 |

该合成附件只证明扩展名、MIME、文件头、multipart、对象存储和业务关联链路正常，不代表真实业务资料的内容真实性审核通过。

### 20.3 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 静态检查 | `pnpm typecheck` 通过；`git diff --check` 和 Markdown 本地相对链接检查通过 |
| 前端构建 | `pnpm build:h5` 与 `pnpm build:mp-weixin` 均通过 |
| 接口联调 | 真实本地接口完成草稿创建、详情/模板、开票、9 类附件、URL、提交和分页列表闭环 |
| 浏览器验收 | 390×844 H5 实跑通过；提交后显示新报单“待审核”和 11/11，浏览器控制台无 warning/error |
| 容器实跑 | MySQL、Valkey、SeaweedFS 均为 `healthy`，Actuator Health 为 `UP` |
| 后端单元测试 | 本轮未执行；没有修改后端代码 |
| Testcontainers 集成测试 | 本轮未执行；没有修改后端代码 |
| 微信开发者工具 / 真机 | 未执行 |
| 生产验证 | 未执行 |

### 20.4 后续范围

- M05 知识产权/资质简表仍需接入真实创建与提交。
- M06 的报单时间线和消息通知中心尚未接入。
- 微信正式联调仍需 AppID/AppSecret、HTTPS 合法域名、微信开发者工具和真机验收。

## 21. 小程序开票页面布局与流程复审（2026-10-09）

### 21.1 Review 结论

- M04 的 `uni-input` 原来按 `content-box` 计算，`width: 100%` 之外再叠加左右 padding，实际外框越过网格列；现已改为 `border-box`，并为网格子项增加 `min-width: 0`。
- 已保存信息点击“重新填写”后，底部“保存并继续”原来会直接切换附件页；现已在 `editingInvoice` 状态先调用保存逻辑。
- 用户截图左下角的像素角色只存在于浏览器“批注”悬浮层，页面 DOM 和项目截图中均不存在，因此没有用业务 CSS 隐藏外部工具节点。

### 21.2 实跑结果

- 宽屏测量中，两个相邻 `uni-input` 的外框宽度分别等于各自网格列宽度，左右边界之间保留正常间距。
- 390×844 视口下 `innerWidth` 与文档 `scrollWidth` 均为 390，没有横向溢出。
- `caseId=3` 使用合成开票信息和演示 URL 完成 11/11 后提交，报单编号为 `BD20261009172143EF32`，状态为 `PENDING_REVIEW`。
- MySQL 只读核对显示 11 个资料项均已提交、关联 9 个附件、审核通过数为 0；本轮没有打开或判断附件内容。
- 浏览器控制台无 warning/error；`pnpm typecheck`、`pnpm build:h5` 和 `pnpm build:mp-weixin` 均通过。
- MySQL、Valkey、SeaweedFS 均为 `healthy`，Actuator Health 为 `UP`。

### 21.3 未完成边界

- 管理后台原登录会话已过期，当前停留在登录页；未读取、猜测或绕过管理员密码。
- 逐项审核、线下凭证交接状态登记以及报单从待审核流转到处理中、已完成，必须在管理员重新登录后继续。
- 后端单元测试、Testcontainers 集成测试、微信开发者工具、真机和生产验证本轮均未执行。

## 22. 小程序企业资料、通知和简表咨询 Review（2026-10-09）

### 22.1 实现结论

- 企业资料页复用 `/api/v1/companies/my`，只展示信用代码和联系电话掩码，不新增明文读取接口。
- 通知中心复用 `/api/v1/notifications/my` 和 `POST /notifications/{id}/read`，支持分页、未读样式、标记已读和跳转相关报单。
- M05 按企业、产品和 `IP`/`QUALIFICATION` 服务类型恢复可编辑草稿或创建新草稿，保存三字段简表后提交审核；贸易增量和简表报单详情按服务类型分流。
- 联系客服在微信目标包使用原生 `open-type="contact"`，H5 明确提示前往微信小程序，不硬编码未经确认的客服电话。
- 后端接口、数据库表和种子数据均未修改；继续复用既有 `IP_SIMPLE`、`QUALIFICATION_SIMPLE` 和 `notification` 表。

### 22.2 本地实跑数据

本轮只使用已标记为本地演示的合成企业，联系电话使用合成测试号码，不包含真实客户身份、银行或生产密钥：

| 服务 | 报单编号 | 资料完成度 | 提交后状态 |
| --- | --- | --- | --- |
| 商标注册咨询 | `BD20261009174937E91D` | 3/3 | `PENDING_REVIEW` |
| 资质申报服务 | `BD202610091751129118` | 3/3 | `PENDING_REVIEW` |

通知中心读取到 4 条既有合成业务通知；点击首条后已读数量从 0 变为 1，并正确跳转到关联的贸易增量报单。

### 22.3 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 静态检查 | `apps/client-uni` 的 `pnpm typecheck` 和根目录 `pnpm api:check` 通过；浏览器控制台无 warning/error |
| 前端构建 | `pnpm build:h5` 与 `pnpm build:mp-weixin` 通过；微信产物确认包含 `open-type="contact"` |
| 接口联调 | 企业资料、通知分页/已读、商标咨询和资质咨询真实本地接口通过；两笔咨询均完成草稿、简表保存、提交和列表回显 |
| 浏览器验收 | H5 企业资料、通知、报单分流和客服提示通过；390×844 下企业资料、通知、简表和我的页面均无横向溢出 |
| 容器实跑 | MySQL、Valkey、SeaweedFS 均为 `healthy`，Actuator Health 为 `UP`，5175 返回 HTTP 200 |
| 后端单元测试 | 本轮未执行；没有修改后端代码 |
| Testcontainers 集成测试 | 本轮未执行；没有修改后端代码或数据库 |
| 微信开发者工具 / 真机 | 未执行；原生客服还需公众平台配置后验收 |
| 生产验证 | 未执行 |

### 22.4 已知限制

- 微信客服能否接通取决于正式小程序后台的客服人员配置，CLI 构建不能代替开发者工具和真机验证。
- 当前通知接口没有独立未读计数或一键全部已读；本轮按列表逐条标记，不额外扩展后端。
- 报单时间线尚未接入小程序。

## 23. 小程序统一图标库替换 Review（2026-10-09）

### 23.1 实现结论

- 新增 `AppIcon.vue`，统一封装 `@dcloudio/uni-ui` 的 `uni-icons`，小程序页头、底栏、首页、服务选择、贸易资料、开票附件、简表、通知、企业资料和“我的”均已接入。
- 贸易增量、商标咨询、资质申报分别使用 `shop-filled`、`medal-filled` 和 `auth-filled`，原字符箭头、字符勾选和 CSS 线条图标已移除。
- 数字步骤、完成度圆环、进度轨道和未读点仍保留为业务状态表达，不属于图标替换范围。
- 增加 `sass` 开发依赖以编译 `uni-icons` 自带 SCSS，并通过 `uni-ui.d.ts` 补充当前包导出缺失的 Vue 类型声明。
- 本轮没有修改接口、后端、数据库或管理后台；用户截图中的像素人物属于浏览器批注悬浮层，不在项目 DOM 中。

### 23.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 静态检查 | `apps/client-uni` 执行 `pnpm typecheck` 通过；根目录 `pnpm api:check` 和 `git diff --check` 通过 |
| 前端构建 | `pnpm build:h5` 与 `pnpm build:mp-weixin` 通过；Dart Sass 仅输出 legacy JS API 弃用提示 |
| 浏览器验收 | 390×844 实际检查 8 个关键路由；图标库均成功渲染，没有缺字占位或横向溢出，控制台无 warning/error |
| 后端单元测试 | 未执行；本轮未修改后端代码 |
| Testcontainers 集成测试 | 未执行；本轮未修改接口、数据库或后端代码 |
| 容器实跑 | 本轮未重新执行；H5 浏览器复用了已运行的本地接口和合成演示数据 |
| 微信开发者工具 / 真机 | 未执行；CLI 目标包构建不能替代工具导入和真机显示验收 |
| 生产验证 | 未执行 |

### 23.3 已知限制和回退

- `uni-icons` 仍依赖上游图标字体和 SCSS，微信开发者工具及真机需要后续复验。
- Dart Sass legacy JS API 提示来自当前 DCloud/Vite 构建链，不影响产物；本轮没有修改第三方依赖源码。
- 回退只需恢复本轮 Vue 文件、移除 `AppIcon.vue`、`uni-ui.d.ts` 和 `sass` 依赖，不涉及接口或数据库回滚。

## 24. 小程序表单与底部导航样式修复 Review（2026-10-10）

### 24.1 修复结论

- M05 简表的 `uni-input` 原按 `content-box` 计算，`width: 100%` 之外继续叠加左右 padding；现已改为 `border-box`，并限制最小/最大宽度，输入框不再越过卡片右边界。
- M01 的“全部服务”和“查看全部”原来无实体边框且带负右边距；现已取消负边距并增加完整浅色边框、白色背景、圆角、轻阴影和按压反馈。
- 通用 `BottomNav` 保留原首页、报单、我的导航逻辑，增加安全区留白、顶部阴影、半透明背景、按钮圆角按压态和当前项图标胶囊。
- 本轮没有修改接口、路由、后端、数据库、依赖或管理后台。

### 24.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 静态检查 | `apps/client-uni` 执行 `pnpm typecheck` 通过；根目录 `pnpm api:check` 和 `git diff --check` 通过 |
| 前端构建 | `pnpm build:h5` 与 `pnpm build:mp-weixin` 通过；仅有既有 Dart Sass legacy JS API 弃用提示 |
| 浏览器验收 | 390×844 下 M01 两个文字按钮边框完整且右边界未超出视口；M05 可编辑简表输入框和授权行位于卡片内；TabBar 阴影、选中胶囊、三项导航布局正常 |
| 浏览器控制台 | 服务稳定后使用独立新标签复验，无 warning/error；旧标签保留了服务重启期间的连接中断历史日志 |
| 后端单元测试 | 未执行；本轮未修改后端代码 |
| Testcontainers 集成测试 | 未执行；本轮未修改接口、数据库或后端代码 |
| 容器实跑 | MySQL、Valkey、SeaweedFS 均为 `healthy`；后端重启后 Actuator 为 `UP`，H5 5175 返回 HTTP 200 |
| 微信开发者工具 / 真机 | 未执行 |
| 生产验证 | 未执行 |

### 24.3 已知限制和回退

- CLI 微信目标包构建不能代替微信开发者工具和真机的安全区验收。
- 自定义 TabBar 仍由页面组件固定定位；后续若迁移到原生 tabBar，需要统一调整页面底部留白和导航行为。
- 回退只需恢复 `simple.vue`、`home/index.vue` 和 `BottomNav.vue`，不涉及接口或数据库回滚。

## 25. 微信本地接口联调与导航栏统一 Review（2026-10-10）

### 25.1 实现结论

- `apps/client-uni/.env.development` 通过 `VITE_CLIENT_AUTH_MODE=dev` 让 H5 和微信 development 包调用既有 `/auth/client/dev-login`；正式微信构建继续使用 `uni.login -> /auth/wechat/login`。
- 微信开发构建默认连接 `http://127.0.0.1:8080/api/v1`。该地址只适用于电脑上的开发者工具模拟器，真机需使用电脑局域网 IP，正式环境必须使用 HTTPS 合法域名。
- `scripts/patch-uni-generated.mjs` 已加入根目录 `api:generate`，自动消除生成代码对微信逻辑层缺失的 `Headers` 和 `URLSearchParams` 全局对象依赖。
- 首页、服务选择、资料清单、开票、简表、企业资料、消息通知和我的报单已移除 `AppHeader` 与 `navigationStyle: custom`，统一由微信原生导航栏提供标题和返回行为；简表页按实际服务动态设置标题。
- 接口契约、数据库结构和后端业务逻辑没有变化。模拟器验收时新增一笔本地合成商标咨询草稿，未提交审核。

### 25.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| OpenAPI / 静态检查 | `pnpm api:generate`、`pnpm api:check` 和 `pnpm --dir apps/client-uni typecheck` 通过；快照为 59 个操作、52 个路径 |
| 前端构建 | `pnpm --dir apps/client-uni build:h5` 与 `build:mp-weixin` 通过；只有既有 Dart Sass legacy JS API 弃用提示 |
| 容器实跑 | MySQL、Valkey、SeaweedFS 均为 `healthy` |
| 后端接口 | Actuator Health 为 `UP`；`/api/v1/auth/client/dev-login` 返回 `code=OK` 和 Token |
| 微信开发者工具模拟器 | 导入 `dist/dev/mp-weixin` 后，本地开发登录、首页、服务选择、贸易资料清单、商标咨询、企业资料、消息通知和我的报单均读取真实本地接口；页面使用原生标题且无重复头部；调试器 `Errors: 0` |
| 微信开发者工具 warnings | 存在工具自身或资源预加载类提示，数量会随页面切换累积；未将其描述为零警告 |
| 后端单元测试 | 本轮未执行；最近 61 个通过只作为历史基线 |
| Testcontainers 集成测试 | 本轮未执行；最近 2 个通过只作为历史基线 |
| 真机验证 | 未执行 |
| 生产验证 | 未执行 |

### 25.3 已知限制和回退

- 正式微信登录仍依赖有效 AppID/AppSecret；服务端换取会话已在后续 Review 27 实现，但本地开发登录不能作为上线验收结论。
- 真机不能访问开发电脑的 `127.0.0.1`，需在同一局域网下使用电脑 IP，并检查防火墙和后端监听地址。
- 正式发布前还需配置 HTTPS request/upload/download 合法域名，并完成真机、客服和生产环境验收。
- 回退前端配置、导航和生成补丁即可；本轮没有数据库迁移，不需要数据库回滚。

## 26. 小程序二级页双导航与旧窗口状态修复 Review（2026-10-10）

### 26.1 修复结论

- 页面源码和微信生成 WXML 已无 `AppHeader`、自定义胶囊或重复标题节点；重复头部不是当前 Vue 模板再次渲染造成的。
- 八个页面和 `globalStyle` 已显式设置 `navigationStyle: default`；二次复核确认 development/build 两套生成配置均为默认导航。
- 电脑上同时存在两个微信开发者工具项目窗口：旧 `dist/build/mp-weixin` 窗口稳定复现双胶囊，当前 `dist/dev/mp-weixin` 窗口显示单胶囊。用户截图来自前者，实际根因是继续使用了保留异常模拟器状态的旧构建窗口，而不是当前页面又渲染了一套头部。
- 已关闭旧 build 窗口，并用窗口清单确认只保留正确的 development 窗口。
- 固定标题继续由 `navigationBarTitleText` 提供，商标/资质共用简表页继续在加载服务类型后动态设置标题。
- 接口、数据库、后端业务、页面路由和客户数据均未变化。

### 26.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 配置静态检查 | 源码未发现 `AppHeader` 或 `navigationStyle: custom`；development/build 产物的全局及页面 JSON 均为 `navigationStyle: default` |
| OpenAPI / TypeScript | `pnpm api:check` 通过，59 个操作、52 个路径；`apps/client-uni` 的 `pnpm typecheck` 通过 |
| 前端构建 | `pnpm build:h5` 与 `pnpm build:mp-weixin` 通过；只有既有 Dart Sass legacy JS API 弃用提示 |
| Git 差异检查 | `git diff --check` 通过；仅有工作区既有 LF/CRLF 提示 |
| 微信开发者工具模拟器 | 同时截取两个项目窗口确认差异：旧 build 窗口为双胶囊，development 窗口为单胶囊；旧窗口已关闭，当前只保留 development 窗口并激活到首页 |
| 后端单元测试 | 本轮未执行；没有修改后端代码 |
| Testcontainers 集成测试 | 本轮未执行；没有修改接口、数据库或后端代码 |
| 容器实跑 | 本轮未重新执行；本次修改不涉及后端或基础设施 |
| 真机验证 | 未执行 |
| 生产验证 | 未执行 |

### 26.3 已知限制和回退

- 本地接口联调只使用 `dist/dev/mp-weixin`；`dist/build/mp-weixin` 仅作为生产构建产物，不要同时导入为第二个调试项目。
- 模拟器出现“所有位置都不能跳转”时，本轮确认是 WXML“选择页面元素”检查模式拦截点击；已关闭该模式并普通编译。源码及 development 产物的 `bindtap`、`navigateTo`、`reLaunch` 均存在，不属于路由代码缺失。
- CLI 构建与当前 development 模拟器截图不能替代全部页面实际返回栈和真机的导航、安全区验收。
- 回退只需恢复 `apps/client-uni/src/pages.json` 和相关文档；不涉及接口或数据库回滚。

## 27. 客户双登录、微信绑定与账号切换 Review（2026-10-10）

### 27.1 实现结论

- 新增 `POST /api/v1/auth/client/login`。它只允许启用的 CLIENT 账号，支持普通密码登录，也支持 `bindWechat=true + wechatCode` 在密码校验成功后首次绑定当前微信。
- `POST /api/v1/auth/wechat/login` 通过真实 `jscode2session` 适配器换取 OpenID/UnionID，只登录已绑定账号；不再自动创建无企业关系账号。
- 管理员和客户密码登录使用不同 Valkey 键统计失败次数，15 分钟内达到 5 次后返回 `AUTH_RATE_LIMITED`。客户登录成功会更新 `last_login_at`。
- 微信已绑定其他账号或客户账号已绑定其他微信时拒绝覆盖；密码只保存 BCrypt 哈希，微信 `session_key` 不保存、不返回。
- 管理后台客户三点菜单新增“登录账号”：可查询企业账号、创建并关联 CLIENT 账号、选择成员身份和重置密码。另新增超级管理员通用密码重置接口。
- 小程序新增 `/pages/auth/login` 双登录页。启动时只恢复已有 Token，无会话时由用户选择微信或账号密码；“我的”页面支持二次确认后退出并登录其他账号。
- 退出、401 和新账号登录会清理用户、企业、目录、报单草稿以及创建/提交幂等键，防止账号切换后读取上一账号缓存。
- 本地 `local` Profile 使用合成账号 `local_client / LocalClient-2026!`；development 的微信按钮走 `/auth/client/dev-login` 模拟已绑定登录，不冒充正式微信绑定。
- 数据库已有字段足以承载本轮能力，没有新增表、字段、索引、种子数据或迁移 SQL。

### 27.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 静态检查 | `apps/client-uni` 的 `pnpm typecheck`、`apps/admin-web` 的 `pnpm check`、根目录 `pnpm api:check` 和 `git diff --check` 通过；OpenAPI 为 64 个操作、56 个路径 |
| 前端构建 | 小程序 `pnpm build:h5` 和 `pnpm build:mp-weixin` 通过；只有既有 Dart Sass legacy JS API 弃用提示；后台生产构建随 `pnpm check` 通过 |
| 后端单元测试 | Maven Surefire 64 个通过，0 失败、0 错误、0 跳过；含 3 个 `WechatAuthClientTest` |
| Testcontainers 集成测试 | Maven Failsafe 2 个通过，0 失败、0 错误、0 跳过；真实覆盖企业客户账号创建、企业关联、密码登录、数据范围、密码重置和新密码重新登录 |
| 容器实跑 | MySQL、Valkey、SeaweedFS 运行且为 `healthy` |
| 本地接口冒烟 | Actuator 为 `UP`；`local_client` 登录、`/auth/me`、`/companies/my` 和退出成功 |
| H5 浏览器验收 | 双登录页、账号密码登录、进入首页、退出/切换账号确认、返回登录页和本地模拟微信登录通过；修复了 uni-app H5 表单 submit 事件不提供浏览器 `preventDefault` 的兼容问题 |
| 正式微信/真机 | 未执行：未配置正式 AppID/AppSecret，未验证微信服务器换取 OpenID、真实绑定、合法域名和真机行为 |
| 生产验证 | 未执行 |

### 27.3 Bug 定位、限制和回退

- 客户密码登录先检查 `AuthController.clientLogin`、`AuthService.clientLogin`、`user_account.username/password_hash/status` 和 Valkey 的 `wqst:auth:fail:client:*`。
- 微信问题先按 `WECHAT_AUTH_NOT_CONFIGURED`、`WECHAT_CODE_INVALID`、`WECHAT_AUTH_UNAVAILABLE`、`WECHAT_ACCOUNT_UNBOUND`、`WECHAT_ALREADY_BOUND` 和 `ACCOUNT_WECHAT_ALREADY_BOUND` 分类，再检查 `WechatAuthClient` 与账号绑定字段。
- 小程序登录页和缓存清理分别检查 `pages/auth/login.vue`、`stores/auth.ts`、`api/generated-client.ts` 及三个业务 Store；后台账号发放检查 `api/companies.ts` 和 `AdminLayout.vue`。
- 正式微信仍依赖微信公众平台凭据、HTTPS 合法域名和真机环境，当前本地模拟与单元测试不能替代该验收。
- 回退时恢复本轮认证、账号、后台和小程序文件，并重新执行 `pnpm api:sync`；本轮没有数据库迁移，不需要回滚数据库结构。

## 28. 微信 development 登录占位页修复 Review（2026-10-10）

### 28.1 修复结论

- 登录页源码和生产构建产物完整，空白问题不是 Vue 模板、路由或接口逻辑缺失。
- 旧 `pnpm dev:mp-weixin` watcher 没有完整生成新增登录页，导致 `dist/dev/mp-weixin/pages/auth/login.wxml` 仍是 64 字节的占位文件，只渲染页面路径文字。
- 精确停止旧 watcher 并重新执行 `pnpm dev:mp-weixin` 后，development WXML 恢复为 3674 字节，包含品牌区、微信登录、账号登录、输入框和本地账号提示。
- 微信开发者工具已热刷新到完整双登录页，“账号登录”页签可正常切换；Console 没有业务 error。
- 本次没有修改业务源码、接口、数据库或环境变量，只修复本地 development 编译状态并补充维护说明。

### 28.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 编译产物检查 | development 的 `login.wxml` 为 3674 字节，并包含“万企商服通”“微信登录”“账号登录”和 `local_client` |
| TypeScript 静态检查 | `apps/client-uni` 执行 `pnpm typecheck` 通过 |
| 微信目标构建 | `pnpm build:mp-weixin` 通过；仅有既有 Dart Sass legacy JS API 弃用提示 |
| OpenAPI / Git 检查 | `pnpm api:check` 通过，64 个操作、56 个路径；`git diff --check` 通过，仅有既有 LF/CRLF 提示 |
| 微信开发者工具模拟器 | 完整双登录页正常显示，“账号登录”页签已实际点击并显示完整表单；Console 仅 2 条资源 preload warning，无业务 error |
| 容器实跑 | MySQL、Valkey、SeaweedFS 均为 `healthy`；Actuator Health 为 `UP` |
| 本地接口冒烟 | `/auth/client/dev-login` 返回有效 Token，随后 `/auth/me` 返回“星澜贸易演示用户”和 `CLIENT` 权限 |
| 后端单元测试 | 未执行；本次没有修改后端代码 |
| Testcontainers 集成测试 | 未执行；本次没有修改接口或数据库 |
| 真机 / 生产验证 | 未执行 |

### 28.3 Bug 定位、限制和回退

- 模拟器只显示 WXML 路径时，先比较 `dist/dev` 和 `dist/build` 的目标页面，不要重复修改已经正确的 Vue 源码。
- 如果 development 文件仍是占位内容，停止旧 watcher 并重新执行 `pnpm dev:mp-weixin`；看到 `DONE Build complete. Watching for changes...` 后再普通编译。
- 本地调试保持 watcher 运行，并确认开发者工具导入 `dist/dev/mp-weixin`，不要同时使用旧 `dist/build` 窗口。
- 正式微信授权仍依赖 AppID/AppSecret、HTTPS 合法域名和真机环境；本地模拟登录不能替代真实微信和生产验收。
- 本次没有业务源码与数据库变更，无需业务回滚；文档需要回退时恢复本 Review、前端排错规则和变更索引即可。

## 29. 我的页面与报单分类列表拆分 Review（2026-10-10）

### 29.1 实现结论

- 小程序“我的”页面不再直接展示“我的报单”卡片列表，原生标题由“我的报单”改为“我的”；企业卡片改为以当前企业名称和成员身份为主信息。
- 全部报单、处理中、待补资料、已完成四个统计项改为可点击按钮，分别进入独立 `/pages/case/list` 页面并自动选择对应分类。
- 新报单列表页承载四类标签切换、服务名称、报单编号、状态、资料完成度、补件提示、分页和空状态；点击卡片继续进入既有贸易资料页或商标/资质简表，查看资料状态。
- 首页“查看全部”和当前报单卡片已改为进入全部报单列表；三类报单提交成功后进入处理中列表。
- `/api/v1/cases/my` 的 `status` 参数兼容单状态和英文逗号分隔的多状态。处理中使用 `PENDING_REVIEW,PROCESSING`，由数据库同一次分页查询，避免客户端拼页造成排序或漏数据。
- 接口路径、响应结构、错误码和数据库结构均未改变；仅扩展既有 `status` 查询语义。

### 29.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| TypeScript 静态检查 | `apps/client-uni` 执行 `pnpm typecheck` 通过 |
| 前端构建 | `pnpm build:h5` 和 `pnpm build:mp-weixin` 通过；仅有既有 Dart Sass legacy JS API 弃用提示 |
| 后端单元测试 | Maven Surefire 65 个通过，0 失败、0 错误、0 跳过；新增逗号分隔多状态解析用例 |
| Testcontainers 集成测试 | Maven Failsafe 2 个通过，0 失败、0 错误、0 跳过；真实 MySQL、Valkey、SeaweedFS 测试容器均启动 |
| 本地容器与后端 | MySQL、Valkey、SeaweedFS 均为 `healthy`；后端重启后 Actuator 为 `UP` |
| 真实本地接口 | 合成客户登录成功；全部、处理中、待补资料、已完成分别返回 9、6、0、1，处理中 6 条均为 `PENDING_REVIEW`，组合筛选生效 |
| H5 浏览器验收 | 390×844 下“我的”无内嵌报单列表；企业卡片、四项统计和底部导航无溢出；四类标签均可切换，待补资料空状态正常 |
| 详情跳转 | 从处理中列表点击资质申报报单，成功进入对应简表并显示待审核、已保存字段和只读资料状态 |
| 微信 development 包 | watcher 已增量编译并输出 `DONE Build complete. Watching for changes...`；微信目标 CLI 构建通过，本轮未重新操作微信开发者工具模拟器 |
| 真机 / 生产验证 | 未执行 |

### 29.3 Bug 定位、限制和回退

- 四项数字错误先检查 `pages/mine/index.vue` 的 `loadStats` 与 `/cases/my` 返回的 `total`；处理中口径固定为 `PENDING_REVIEW + PROCESSING`。
- 分类列表错误先检查 `pages/case/list.vue` 的筛选映射、`api/case.ts` 和 `CaseService.mine` 的多状态 `IN` 查询。
- 点击卡片不进入资料页时检查 `constants/service.ts` 的 `caseDetailRoute` 和报单 `serviceType`，不要在列表页重复实现详情路由。
- 当前详情仍复用既有贸易资料页和咨询简表，没有新增统一时间线页面；报单时间线仍是后续能力。
- 回退需恢复 `CaseService.mine`、对应单元测试、我的/首页/提交跳转、`pages.json` 和维护文档，并删除新增 `pages/case/list.vue`；不涉及数据库回滚。

## 30. 营业执照品类匹配 Review（2026-10-10）

### 30.1 实现结论

- 底部导航由“首页 / 报单 / 我的”调整为“首页 / 品类匹配 / 我的”；服务选择和报单资料页保留为二级流程，不再显示报单底栏项。
- 新增 `/pages/category/match`：支持营业执照拍照/相册选择、预览、OCR 回填、人工校正、经营范围匹配、行业分组结果和服务跳转。
- 新增 `POST /category-match/recognize` 和 `POST /category-match/recommend`。OCR 通过服务端腾讯云 SDK 调用 `BizLicenseOCR`，凭据只从服务端环境变量读取；图片不写入对象存储、文件表或报单。
- 新增 `business_category_rule` 和 19 条开发基础规则。推荐由数据库规则实际计算，返回命中关键词和可解释分数；无命中返回空结果，不伪造推荐。
- 未配置 `TENCENTCLOUD_SECRET_ID/TENCENTCLOUD_SECRET_KEY` 时，OCR 明确返回 `OCR_NOT_CONFIGURED`，页面自动降级为手工录入后继续匹配。

### 30.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 静态检查 | 小程序 `pnpm typecheck`、后台 `pnpm check`、`pnpm api:check` 通过；OpenAPI 为 66 个操作、58 个路径 |
| 前端构建 | `pnpm build:h5` 和 `pnpm build:mp-weixin` 通过；微信构建产物已包含 `pages/category/match`，只有既有 Dart Sass legacy JS API 提示 |
| 后端单元测试 | Maven Surefire 67 个通过，0 失败、0 错误、0 跳过；新增品类分组、零命中和 OpenAPI 契约用例 |
| Testcontainers 集成测试 | Maven Failsafe 3 个通过，0 失败、0 错误、0 跳过；真实启动 MySQL、Valkey、SeaweedFS，按 001–004 初始化 28 张表，并以客户 Token 验证中文经营范围命中 `SOFTWARE_SERVICE` |
| 数据库实跑 | 已向本地 MySQL 应用 `004_category_match.sql`，查询到 19 条启用规则 |
| 容器实跑 | MySQL、Valkey、SeaweedFS 为 `healthy`；新版后端 JAR 在 `wqst-api-dev` Linux JRE 容器运行，Actuator 为 `UP` |
| 真实接口 | 本地合成客户登录后，测试经营范围返回 2 个行业、3 个品类；未配置密钥时 OCR 返回 HTTP 503 + `OCR_NOT_CONFIGURED`，降级提示正确 |
| H5 浏览器 | 390×844 下页面无横向溢出；真实填写后显示 2 行业/3 品类；底栏仅首页/品类匹配/我的；首页开始报单进入无底栏的服务二级页 |
| 微信开发者工具 | 未执行；只完成微信目标包 CLI 构建 |
| 腾讯云真实 OCR | 未执行；当前环境没有 `TENCENTCLOUD_SECRET_ID/TENCENTCLOUD_SECRET_KEY`，不能冒充通过 |
| 真机 / 生产 | 未执行 |

### 30.3 Bug 定位、限制和回退

- 页面问题先看 `pages/category/match.vue`、`api/category-match.ts`、`BottomNav.vue` 和 `pages.json`；上传需走 `uni.uploadFile`，不能直接调用生成的 FormData 接口。
- OCR 问题先按 `OCR_NOT_CONFIGURED`、`OCR_NO_BIZ_LICENSE`、`OCR_SERVICE_UNAVAILABLE` 分类，再看 `BusinessLicenseOcrClient` 和服务端环境变量；严禁把密钥写进小程序 `.env`。
- 匹配不准确先查询 `business_category_rule.keywords/status/sort_order`，再看 `CategoryMatchService.matchedKeywords`；当前是可解释关键词规则，不是语义模型。
- 新库规则数量为 19 但中文经营范围零命中时，先确认 `004_category_match.sql` 的 `SET NAMES utf8mb4` 未被删除，并检查迁移是否按 001–004 顺序执行。
- 当前品类规则是首批通用基础数据，未等同于任何第三方平台完整的 17 行业/180+ 品类目录，也没有后台规则维护页。
- 回退需恢复导航、页面、领域 API、后端 `categorymatch`、OpenAPI 生成文件和文档；数据库用备份恢复或删除 `business_category_rule`。生产回退前应确认没有运营方已维护的新规则。

## 31. 本地全流程手工测试准备 Review（2026-10-12）

### 31.1 启动结论

- Docker Desktop 因失效运行时套接字无法启动。停止 Docker/WSL 后，将 `Docker/run` 与 `docker-secrets-engine` 临时目录隔离为带时间戳的 `*.broken.*` 目录，Docker Engine 29.8.0 恢复可用；没有删除项目数据库卷。
- `wqst-mysql`、`wqst-valkey`、`wqst-seaweedfs` 已启动且为 `healthy`。
- 当前后端 JAR 已用 `eclipse-temurin:21-jre` 容器 `wqst-api-dev` 启动，Actuator Health 为 `UP`。
- 管理后台运行在 `5174`，小程序 H5 运行在 `5175`，微信 development watcher 已完成编译并持续监听。
- 原数据库已有未知密码的同名 `local_admin`，引导机制不会覆盖。新增独立合成超级管理员 `manual_admin_20261012`，保留旧账号不变。

### 31.2 验证结果

| 验证类型 | 实际结果 |
| --- | --- |
| 后端编译打包 | `mvn -DskipTests package` 成功；测试明确跳过 |
| 单元测试 | 本轮未执行 |
| Testcontainers 集成测试 | 本轮未执行 |
| 容器实跑 | 三个基础设施容器 `running/healthy`；后端容器运行且 Health `UP` |
| 认证冒烟 | `manual_admin_20261012` 管理员登录成功；`local_client` 客户密码登录成功 |
| 只读接口冒烟 | 工作台、后台企业/报单/产品/账号、客户企业/报单/通知、服务目录均返回成功 |
| HTTP 连通 | Swagger、OpenAPI、管理后台 `5174`、小程序 H5 `5175` 可访问 |
| 微信构建 | development watcher 输出 `DONE Build complete. Watching for changes...` |
| 手工用例模板 | 111 条用例；总览公式重算正确，公式错误扫描 0 条，四个工作表完成渲染检查 |
| 浏览器全量点击 | 未执行；交付给用户逐项手工执行 |
| 微信模拟器 / 真机 / 生产 | 未执行 |

### 31.3 边界与排错

- 当前进程由本次 Codex 会话启动；电脑重启或终端关闭后按 `docs/08_全流程手工测试指南.md` 重新启动。
- Docker 日志出现一次界面端“Reset to factory defaults”记录，但项目命名卷、既有账号和业务数据仍可查询，本轮没有主动执行重置命令。
- 业务写入、审核补件、导入导出和权限异常由用户按 Excel 用例逐条验证；本轮接口冒烟不能替代完整业务验收。
- 腾讯云 OCR、正式微信登录/绑定、微信客服、微信开发者工具模拟器、真机和生产依赖外部条件，未执行不得写成通过。
