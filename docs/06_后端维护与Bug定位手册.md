# 万企商服通后端维护与 Bug 定位手册

> 适用对象：以前端开发为主、需要维护或联调后端的开发者  
> 后端目录：`services/api`  
> 技术栈：Spring Boot 3.4、Java 21、MyBatis-Plus、Sa-Token、MySQL、Valkey、SeaweedFS S3

## 1. 先理解一条请求经过哪里

后端请求通常按下面的顺序执行：

```text
前端请求
  -> Controller：接收参数、校验登录和权限
  -> Service：业务规则、状态判断、事务和数据范围
  -> Mapper：执行 MySQL 查询或更新
  -> MySQL / Valkey / SeaweedFS
  -> ApiResponse：统一返回给前端
```

排查接口 Bug 时，不要先翻全部源码。先根据 URL 找 Controller，再沿构造函数中的 Service、Mapper 往下找。

## 2. 业务模块和代码位置

| 前端功能或接口前缀 | Controller 入口 | 主要业务逻辑 | 数据库/外部服务 |
| --- | --- | --- | --- |
| `/api/v1/auth` | `auth/AuthController.java` | `auth/AuthService.java` | `user_account`、Valkey、Sa-Token |
| 后台账号与角色 | `account/AdminAccountController.java` | `account/AccountService.java` | `user_account`、`role`、`permission` |
| `/api/v1/companies` | `company/CompanyController.java` | `company/CompanyService.java` | `company`、`company_user` |
| 服务和产品 | `catalog/CatalogController.java`、`AdminProductController.java` | `catalog/CatalogService.java` | 服务模块、产品、模板三组表 |
| `/api/v1/cases` | `caseorder/CaseController.java` | `caseorder/CaseService.java` | 报单、开票、资料、状态日志 |
| 后台审核和补件 | `caseorder/AdminCaseController.java` | `caseorder/ReviewService.java` | 审核日志、补件表、通知表 |
| `/api/v1/files` | `file/FileController.java` | `file/FileService.java` | `file_object`、SeaweedFS |
| `/api/v1/admin/imports` | `imports/ImportController.java` | `ImportService.java`、`ImportWorker.java` | EasyExcel、异步任务、SeaweedFS |
| `/api/v1/admin/exports` | `exports/ExportController.java` | `ExportService.java`、`ExportWorker.java` | EasyExcel、ZIP、SeaweedFS |
| `/api/v1/notifications` | `notification/NotificationController.java` | `NotificationService.java` | `notification` |
| `/api/v1/audit` | `audit/AuditController.java` | `AuditService.java` | 三类审计日志表 |
| `/api/v1/admin/dashboard` | `dashboard/DashboardController.java` | `DashboardService.java` | 报单和企业统计 |

## 3. 前端最常修改的后端位置

### 3.1 修改接口请求或响应字段

按这个顺序检查：

1. 对应 Controller 末尾的 `record XxxRequest`。
2. 对应 Service 末尾的 `record XxxView` 或 `record XxxCommand`。
3. 如果需要保存，检查对应 `entity` 和数据库字段。
4. 更新 `docs/04_API接口设计.md`。
5. 更新或新增测试。

不要只修改前端字段名。Java `record`、Service 返回结构和 API 文档必须同步。

### 3.2 修改报单状态

主要位置：

- 状态规则：`common/CaseStateMachine.java`
- 状态执行：`caseorder/CaseService.java`
- 后台入口：`caseorder/AdminCaseController.java`
- 状态文档：`docs/00_开发文档总览.md`

禁止通过普通更新接口直接写 `service_case.status`。

### 3.3 修改贸易增量 11 项资料

主要位置：

- 初始模板数据：`database/002_full_schema.sql`
- 模板读取：`catalog/CatalogService.java`
- 创建报单快照：`caseorder/CaseService.java`
- 文件限制：模板项的扩展名、大小和数量字段
- 前端说明：`docs/04_API接口设计.md`

已存在的报单使用创建时的资料快照。修改模板不会自动改变历史报单。

### 3.4 修改权限

主要位置：

- 权限种子：`database/002_full_schema.sql`
- 权限读取：`auth/SaTokenPermissionProvider.java`
- 固定权限：Controller 上的 `@SaCheckPermission` 或 `@SaCheckRole`
- 动态权限：Controller/Service 内的 `SecuritySupport`
- 企业数据范围：`company/CompanyService.requireClientAccess`

权限校验和企业数据范围是两层检查，不能只保留其中一层。

### 3.5 修改文件上传规则

主要位置：

- 扩展名、MIME、文件头：`file/FileInspector.java`
- 上传、删除、预览权限：`file/FileService.java`
- S3 操作：`file/ObjectStorageService.java`
- 大小和数量：资料模板项配置

不要向前端返回 `objectKey` 或永久对象地址。

### 3.6 修改 Excel 或 ZIP

- 客户导入：`imports/ImportService.java`、`ImportWorker.java`
- 审核 Excel：`exports/ExportWorker.java` 中的 `ReviewRow`
- ZIP 目录：`ExportWorker.materialZip`
- 文件名清理：`common/SafeFiles.java`

ZIP 仍必须保持 01–11 目录和 `审核表.xlsx`，除非需求文档明确改变。

## 4. 常见错误怎么排查

| HTTP/错误码 | 常见原因 | 优先检查 |
| --- | --- | --- |
| 401 `AUTH_REQUIRED` | Token 缺失、过期或登录场景不一致 | 请求头、`AuthService`、前端 Token 存储 |
| 403 `AUTH_PERMISSION_DENIED` | 角色缺权限 | Controller 权限注解、角色权限种子 |
| 403 `COMPANY_SCOPE_DENIED` | 当前客户不属于该企业 | `company_user`、`requireClientAccess` |
| 400 `MATERIAL_REQUIRED` | 必填资料未提交 | 报单详情的 `missingItems`、资料状态 |
| 409 `CASE_NOT_EDITABLE` | 报单已不在草稿/待补件 | 状态机和当前报单状态 |
| 409 `OPTIMISTIC_LOCK_CONFLICT` | 页面数据已过期 | 重新获取详情和 `version` |
| 400 `FILE_*` | 文件扩展名、MIME、文件头或大小不符 | `FileInspector`、模板限制 |
| 503 `VALKEY_UNAVAILABLE` | Valkey 未启动 | `docker compose ps`、Valkey 日志 |
| 503 `OBJECT_STORAGE_UNAVAILABLE` | SeaweedFS 未启动或密钥不一致 | Compose、S3 配置、健康检查 |
| 503 `DATABASE_UNAVAILABLE` | MySQL 未启动或连接参数错误 | MySQL 容器、`application-*.yml` |
| 导入/导出长时间 `PENDING` | 异步线程或恢复任务异常 | `ImportRecovery`、`ExportRecovery`、服务日志 |
| Testcontainers 显示 Docker 400 或集成测试被跳过 | Docker 29 最低 API 为 1.40，测试客户端默认 API 过旧 | `DOCKER_HOST`、`-Dapi.version=1.40`、`target/failsafe-reports` |

## 5. 本地验证命令

后端单元和集成测试入口：

```powershell
cd services/api
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"
.\mvnw.cmd "-Dapi.version=1.40" clean verify
```

成功时必须同时看到 `54` 个单元测试和 `2` 个集成测试均为 `Skipped: 0`。只看到 `BUILD SUCCESS` 不代表集成测试实际运行，应同时检查 `target/failsafe-reports/`。

基础设施：

```powershell
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs --tail 100
```

接口地址：

```text
API:     http://127.0.0.1:8080/api/v1
Swagger: http://127.0.0.1:8080/swagger-ui.html
Health:  http://127.0.0.1:8080/actuator/health
```

## 6. 每次修改后的最小检查

- 接口能否按文档调用。
- 前端使用的字段名是否改变。
- 权限和企业数据范围是否仍有效。
- 数据库脚本和实体字段是否一致。
- 是否新增了敏感信息输出。
- `mvnw clean verify` 是否通过。
- 是否新增本次变更记录并更新变更索引。

如果 Docker 未启动，要明确写“集成测试跳过”，不能写成容器实测通过。
