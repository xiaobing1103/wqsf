# 万企商服通 API 接口设计

> API 版本：`v1`  
> 基础地址：`/api/v1`  
> 数据格式：JSON；文件上传使用 `multipart/form-data`

## 1. 通用约定

### 2026-10-14 客户功能开放接口

- 匿名 `GET /api/v1/client/capabilities`：`ApiResponse.data` 是七个布尔字段（tradeApplicationEnabled、trademarkConsultationEnabled、qualificationConsultationEnabled、categoryMatchEnabled、licenseOcrEnabled、sensitiveMaterialUploadEnabled、customerServiceEnabled），仅表示当前可用，不表示审核通过。
- `GET /api/v1/admin/client-capabilities`：后台登录且角色为 `SUPER_ADMIN`；data 为 key/enabled/version/releaseAllowed 的数组。releaseAllowed仅为兼容旧客户端保留，现固定true，不参与有效值计算，不表示业务资质或审核通过。
- `PUT /api/v1/admin/client-capabilities/{key}`：后台登录且角色为 `SUPER_ADMIN`；请求 enabled（布尔必填）、version（整数>=0必填）、reason（非空<=200字）；返回最新配置数组，更新与审计同事务。
- 普通管理员即使拥有 product:view/product:edit 仍返回 403 `AUTH_PERMISSION_DENIED`；匿名和客户会话不能作为后台登录使用。角色拒绝统一返回403，不再落入通用500处理；其他角色注解接口同样受益。公开布尔接口、路径及响应字段不变。
- 403 FEATURE_DISABLED：功能暂未开放；404 CAPABILITY_NOT_FOUND：未知功能键；409 OPTIMISTIC_LOCK_CONFLICT：版本冲突。2026-10-14取消部署清单限制，管理接口不再返回FEATURE_RELEASE_NOT_ALLOWED；七项有效值按enabled计算，OCR另依赖品类参考。超级管理员可直接开启/关闭所有七项，无需修改环境变量。
- 目录过滤未开放服务；新建、开票/简表/文字保存、提交、文件上传及品类推荐/OCR按开关拦截。开票与敏感项新上传/文字需敏感开关；服务关闭不影响既有授权列表/详情/时间线/文件预览。保留原权限，不开放匿名客户资料。
- 默认仅开启手填品类参考，其他关闭；配置、管理员使用及审查边界见 [功能开放管理](./10_小程序审核风险与功能开放管理.md)。

### 1.1 请求头

```http
Authorization: Bearer <token>
Content-Type: application/json
X-Request-Id: <客户端生成的 UUID>
Idempotency-Key: <创建/提交/导出时使用>
```

文件上传不强制使用 JSON Content-Type，但仍需传递 Authorization、X-Request-Id 和业务幂等键。

### 1.2 通用响应

```json
{
  "success": true,
  "code": "OK",
  "message": "操作成功",
  "data": {},
  "requestId": "01J8EXAMPLE",
  "timestamp": "2026-09-23T16:30:00+08:00"
}
```

错误示例：

```json
{
  "success": false,
  "code": "MATERIAL_REQUIRED",
  "message": "请先补齐必填资料",
  "data": {
    "fields": ["taxNo", "basicAccount"]
  },
  "requestId": "01J8EXAMPLE",
  "timestamp": "2026-09-23T16:30:00+08:00"
}
```

### 1.3 分页参数

```text
page=1&pageSize=20
```

`pageSize` 默认 20，最大 100。列表默认按 `createdAt DESC, id DESC` 排序。

## 2. 认证与当前用户

### 2.1 微信小程序登录

```http
POST /api/v1/auth/wechat/login
```

请求：

```json
{
  "code": "wx-login-code"
}
```

响应：

```json
{
  "token": "client-token",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 10001,
    "displayName": "测试联系人",
    "phoneMask": "138 **** 0000"
  }
}
```

后端通过微信 `jscode2session` 校验一次性 `code` 并取得 OpenID/UnionID；`session_key` 不保存、不返回。只允许已绑定且启用的 CLIENT 账号登录，未绑定时返回 `409 WECHAT_ACCOUNT_UNBOUND`，不会自动创建无企业关系账号。

### 2.2 客户账号密码登录与首次绑定

```http
POST /api/v1/auth/client/login
```

普通账号登录：

```json
{
  "username": "client_demo",
  "password": "由管理员发放的客户密码",
  "bindWechat": false
}
```

在正式微信小程序中首次绑定：

```json
{
  "username": "client_demo",
  "password": "由管理员发放的客户密码",
  "bindWechat": true,
  "wechatCode": "uni.login-新取得的一次性-code"
}
```

- 只允许 `account_type=CLIENT` 且状态为 `ENABLED` 的账号；管理员账号不能从该接口登录。
- 密码错误统一返回 `AUTH_INVALID_CREDENTIALS`，不暴露用户名是否存在。
- 管理员和客户密码登录分别计数；同一用户名 15 分钟内失败 5 次后返回 `429 AUTH_RATE_LIMITED`。
- `bindWechat=true` 时必须提供新取得的 code。微信或客户账号已绑定其他对象时拒绝覆盖，分别返回 `WECHAT_ALREADY_BOUND` 或 `ACCOUNT_WECHAT_ALREADY_BOUND`。
- 绑定与登录成功后更新 `last_login_at`，密码只保存 BCrypt 哈希。

### 2.3 后台登录

```http
POST /api/v1/auth/admin/login
```

请求：

```json
{
  "username": "operator",
  "password": "由前端 HTTPS 传输的密码"
}
```

响应包含后台用户、角色编码和权限编码。密码错误不得说明是用户名不存在还是密码错误。

### 2.4 当前用户与退出

```http
GET /api/v1/auth/me
POST /api/v1/auth/logout
```

`/auth/me` 同时识别后台和客户端登录场景；客户端退出后前端还必须清除企业、目录、草稿和幂等键缓存。

### 2.5 微信登录错误码

| 错误码 | HTTP | 含义 |
| --- | --- | --- |
| `WECHAT_AUTH_NOT_CONFIGURED` | 503 | 后端未配置正式 AppID/AppSecret |
| `WECHAT_CODE_INVALID` | 401 | code 无效、过期或微信返回业务错误 |
| `WECHAT_AUTH_UNAVAILABLE` | 503 | 微信服务或网络暂时不可用 |
| `WECHAT_ACCOUNT_UNBOUND` | 409 | 当前微信尚未绑定客户账号 |
| `WECHAT_CODE_REQUIRED` | 400 | 选择绑定但未提交微信 code |
| `WECHAT_ALREADY_BOUND` | 409 | 当前微信已绑定其他客户账号 |
| `ACCOUNT_WECHAT_ALREADY_BOUND` | 409 | 当前客户账号已绑定其他微信 |

## 3. 企业客户接口

### 3.1 当前用户企业

```http
GET /api/v1/companies/my
```

响应：

```json
[
  {
    "id": 20001,
    "companyName": "示例科技有限公司",
    "creditCodeMask": "9143**********8X",
    "contactPhoneMask": "138 **** 0000",
    "memberRole": "CONTACT"
  }
]
```

### 3.2 企业详情

```http
GET /api/v1/companies/{companyId}
```

返回内容按当前用户权限脱敏。小程序只返回该用户所属企业。

### 3.3 后台企业列表

```http
GET /api/v1/admin/companies?page=1&pageSize=20&keyword=示例&status=ENABLED
```

需要 `company:view` 权限。

### 3.4 后台企业维护

```http
GET    /api/v1/admin/companies/{companyId}
POST   /api/v1/admin/companies
PATCH  /api/v1/admin/companies/{companyId}
DELETE /api/v1/admin/companies/{companyId}?version={version}
```

- 查询、新增和编辑需要 `company:view`；删除仅允许 `SUPER_ADMIN`。
- 新增和编辑会写入操作日志，响应中的统一社会信用代码和联系电话始终为脱敏值。
- 编辑请求必须携带当前 `version`。统一社会信用代码或联系电话传空值时保持原值，避免前端回填脱敏值覆盖数据库原文。
- 删除是软删除：企业设为停用、写入 `deleted_at` 并停用企业成员关系，不删除历史报单、文件或日志。
- 企业存在 `DRAFT`、`PENDING_REVIEW`、`NEED_SUPPLEMENT` 或 `PROCESSING` 报单时返回 `409 COMPANY_ACTIVE_CASE_EXISTS`。
- 版本过期时返回 `409 OPTIMISTIC_LOCK_CONFLICT`，前端应重新读取详情后再操作。

### 3.5 后台企业客户账号

```http
GET  /api/v1/admin/companies/{companyId}/client-accounts
POST /api/v1/admin/companies/{companyId}/client-accounts
PUT  /api/v1/admin/companies/{companyId}/client-accounts/{userId}/password
```

创建请求：

```json
{
  "username": "client_demo",
  "password": "至少 8 位的初始密码",
  "displayName": "演示客户联系人",
  "phone": "",
  "memberRole": "CONTACT"
}
```

- 需要 `company:view`，创建账号时固定为 `CLIENT` 角色并立即关联指定企业。
- `memberRole` 只允许 `OWNER`、`CONTACT` 或 `VIEWER`。
- 重置密码前必须校验该用户确为当前企业成员，创建和重置均写操作审计；响应不返回密码或哈希。
- 通用管理员密码重置为 `PUT /api/v1/admin/accounts/{userId}/password`，仅允许 `SUPER_ADMIN`，ADMIN 密码至少 12 位，CLIENT 密码至少 8 位。

## 4. 产品和资料模板接口

### 4.1 小程序服务目录

```http
GET /api/v1/services/catalog
```

响应：

```json
[
  {
    "moduleCode": "TRADE_INCREMENT",
    "moduleName": "贸易增量",
    "products": [
      {
        "id": 30001,
        "productName": "贸易增量服务",
        "materialCount": 11,
        "description": "月度企业材料提交"
      }
    ]
  },
  {
    "moduleCode": "IP",
    "moduleName": "知识产权",
    "products": []
  },
  {
    "moduleCode": "QUALIFICATION",
    "moduleName": "资质申报",
    "products": []
  }
]
```

### 4.2 产品资料模板

```http
GET /api/v1/products/{productId}/material-template
```

返回已发布模板项：

```json
{
  "templateCode": "TRADE_INCREMENT_V1",
  "versionNo": 1,
  "items": [
    {
      "itemCode": "BUSINESS_LICENSE",
      "itemName": "营业执照图片",
      "inputType": "FILE",
      "required": true,
      "sensitive": false,
      "allowedExtensions": ["jpg", "jpeg", "png", "pdf"],
      "maxFileSizeMb": 10,
      "helpText": "请上传清晰完整的营业执照"
    }
  ]
}
```

### 4.3 后台产品管理

```http
GET  /api/v1/admin/products
POST /api/v1/admin/products
PATCH /api/v1/admin/products/{productId}
POST /api/v1/admin/products/{productId}/publish
POST /api/v1/admin/products/{productId}/unpublish
```

需要 `product:view` 或 `product:edit` 权限。模板发布必须生成版本记录。

## 5. 报单接口

### 5.1 创建草稿

```http
POST /api/v1/cases
Idempotency-Key: create-case-unique-key
```

请求：

```json
{
  "companyId": 20001,
  "productId": 30001,
  "serviceType": "TRADE_INCREMENT",
  "caseMonth": "2026-09"
}
```

响应：

```json
{
  "id": 40001,
  "caseNo": "BD20260923001",
  "status": "DRAFT",
  "companyName": "示例科技有限公司",
  "serviceName": "贸易增量服务",
  "materials": {
    "total": 11,
    "completed": 0,
    "requiredRemaining": 11
  }
}
```

### 5.2 我的报单列表

```http
GET /api/v1/cases/my?page=1&pageSize=20&status=NEED_SUPPLEMENT
```

`status` 可不传、传一个状态，或使用英文逗号传多个状态；多个状态按“任一匹配”筛选，并继续使用同一套服务端分页。例如小程序“处理中”同时包含待审核和实际办理中：

```http
GET /api/v1/cases/my?page=1&pageSize=20&status=PENDING_REVIEW,PROCESSING
```

当前小程序列表口径：

| 页面分类 | `status` 参数 |
| --- | --- |
| 全部报单 | 不传 |
| 处理中 | `PENDING_REVIEW,PROCESSING` |
| 待补资料 | `NEED_SUPPLEMENT` |
| 已完成 | `COMPLETED` |

### 5.3 报单详情

```http
GET /api/v1/cases/{caseId}
```

必须按当前用户企业关系校验。返回客户可见资料项和客户可见审核说明，不返回内部备注。

### 5.4 保存贸易增量开票信息

```http
PUT /api/v1/cases/{caseId}/invoice-info
```

请求：

```json
{
  "companyName": "示例科技有限公司",
  "taxNo": "9143**********8X",
  "contactAddress": "湖南省衡阳市蒸湘区示例路 88 号",
  "contactPhone": "138****0000",
  "legalPerson": "赵先生",
  "companyEmail": "service@example.com",
  "bankBranch": "示例银行衡阳分行",
  "basicAccount": "********6628"
}
```

后端收到的是用户输入的原值时，立即加密；响应只返回掩码值。文档示例使用脱敏值，不代表实际请求必须传掩码。

### 5.5 保存知识产权/资质申报简表单

```http
PUT /api/v1/cases/{caseId}/simple-info
```

请求：

```json
{
  "legalPersonName": "赵先生",
  "companyName": "示例科技有限公司",
  "contactPhone": "138****0000",
  "consent": true
}
```

### 5.6 提交报单

```http
POST /api/v1/cases/{caseId}/submit
Idempotency-Key: submit-case-40001-v1
```

失败时返回缺失项：

```json
{
  "code": "MATERIAL_REQUIRED",
  "data": {
    "missingItems": [
      {"materialCode": "TAX_QUOTA_SCREENSHOT", "materialName": "税务局开票额度截图"},
      {"materialCode": "ONLINE_BANK_LIMIT", "materialName": "网银限额截图"}
    ]
  }
}
```

## 6. 文件接口

### 6.1 上传文件

```http
POST /api/v1/files
Content-Type: multipart/form-data
```

表单字段：

```text
caseId       报单 ID
materialId   资料项 ID
file         文件二进制
```

响应：

```json
{
  "fileId": 70001,
  "materialId": 50005,
  "originalName": "开票额度截图.png",
  "fileSize": 2457600,
  "status": "READY",
  "sha256": "脱敏示例摘要"
}
```

不返回 `objectKey`、S3 endpoint 或永久 URL。

### 6.2 删除文件

```http
DELETE /api/v1/files/{fileId}
```

只允许删除当前用户自己报单中仍处于可编辑状态的文件。

### 6.3 受控预览

```http
POST /api/v1/files/{fileId}/preview
```

响应二选一：

```json
{
  "previewUrl": "短时签名 URL",
  "expiresAt": "2026-09-23T16:35:00+08:00"
}
```

或返回后端流式预览地址。每次预览都写入 `file_access_log`。

## 7. 后台审核接口

### 7.1 报单列表

```http
GET /api/v1/admin/cases?page=1&pageSize=20&keyword=示例&status=NEED_SUPPLEMENT&serviceType=TRADE_INCREMENT&assigneeId=10001
```

需要 `case:view`。

### 7.2 分配负责人

```http
POST /api/v1/admin/cases/{caseId}/assign
```

```json
{
  "assigneeId": 10001
}
```

需要 `case:assign`。

### 7.3 资料逐项审核

```http
POST /api/v1/admin/materials/{materialId}/review
```

```json
{
  "reviewStatus": "PASSED",
  "clientVisibleNote": "文件清晰，已通过审核。",
  "internalNote": "核对企业名称和日期无误。",
  "version": 3
}
```

需要 `case:review`。`internalNote` 只在后台响应中返回。

### 7.4 创建补件要求

```http
POST /api/v1/admin/cases/{caseId}/supplement-request
```

```json
{
  "materialIds": [50005, 50006, 50007],
  "clientMessage": "请补充最新开票额度截图、网银限额及余额截图。"
}
```

### 7.5 更新整体状态

```http
POST /api/v1/admin/cases/{caseId}/status
```

```json
{
  "targetStatus": "PROCESSING",
  "note": "资料已齐全，进入办理阶段。"
}
```

服务端必须依据状态机判断是否允许流转，不能由前端自由指定任意状态。

### 7.6 线下凭证交接状态

```http
GET /api/v1/admin/cases/{caseId}/credential-handover
PUT /api/v1/admin/cases/{caseId}/credential-handover
```

需要 `case:credential-handover` 权限，仅适用于贸易增量报单。首次查询尚未登记的报单时返回三个 `PENDING` 状态和 `version=0`。

更新请求：

```json
{
  "onlineBankingStatus": "HANDED_OVER",
  "uShieldStatus": "VERIFIED",
  "transactionAuthorizationStatus": "NOT_REQUIRED",
  "version": 0
}
```

允许状态为 `PENDING`、`HANDED_OVER`、`VERIFIED`、`RETURNED`、`NOT_REQUIRED`。响应额外包含最近经办管理员 `handledBy`、登记时间 `handledAt` 和乐观锁版本号。该接口只登记线下交接结果，不接收网银密码、U 盾密码、交易密码、验证码、文件或自由备注；小程序没有对应接口。

## 8. Excel 导入接口

### 8.1 获取导入模板

```http
GET /api/v1/admin/imports/template
```

返回文件下载任务或短时下载地址。

### 8.2 创建导入任务

```http
POST /api/v1/admin/imports
Content-Type: multipart/form-data
```

表单字段：

```text
file         Excel 文件
dryRun       true/false，是否只校验不落库
```

响应：

```json
{
  "jobId": 80001,
  "status": "PENDING",
  "totalRows": 0,
  "successRows": 0,
  "failedRows": 0
}
```

### 8.3 查询导入任务

```http
GET /api/v1/admin/imports/{jobId}
```

### 8.4 下载错误报告

```http
POST /api/v1/admin/imports/{jobId}/error-report
```

错误报告需要对手机号、税号、账号脱敏。

## 9. 导出接口

### 9.1 创建审核表 Excel

```http
POST /api/v1/admin/exports/review-excel
Idempotency-Key: export-review-40001-v1
```

```json
{
  "caseIds": [40001, 40002]
}
```

需要 `export:excel`。

响应数据模型为 `ExportJobView`，字段包括 `id`、`caseId`、`exportType`、`status`、`errorCode`、`errorMessage`、`expiresAt`、`downloadCount`、`createdAt` 和 `completedAt`。该模型与 Excel 导入任务的 `JobView` 独立，前端生成 API 中对应 `ExportJobView`。

### 9.2 创建资料 ZIP

```http
POST /api/v1/admin/exports/material-zip
Idempotency-Key: export-zip-40001-v1
```

```json
{
  "caseId": 40001,
  "includeSensitive": true
}
```

需要 `export:zip`，且服务类型必须是 `TRADE_INCREMENT`。

### 9.3 查询导出任务

```http
GET /api/v1/admin/exports/{jobId}
```

### 9.4 下载导出文件

```http
POST /api/v1/admin/exports/{jobId}/download
```

服务端再次检查权限和过期时间后，返回短时下载地址或文件流，并写入 `download_log`。

当前没有导出任务分页历史接口。管理后台导出中心只查询当前浏览器记录的任务 ID；新增跨设备历史时应补充 `GET /api/v1/admin/exports?page=1&pageSize=20`，并同步 OpenAPI 与前端生成代码。

## 10. 消息和状态接口

### 10.1 我的通知

```http
GET /api/v1/notifications/my?page=1&pageSize=20
POST /api/v1/notifications/{notificationId}/read
```

### 10.2 报单状态时间线

```http
GET /api/v1/cases/{caseId}/timeline
```

客户只看到客户可见状态、时间和补件说明；内部备注不返回。

## 11. 幂等和并发

以下接口必须支持 `Idempotency-Key`：

- 创建报单。
- 提交报单。
- 创建补件要求。
- 创建 Excel/ZIP 导出任务。
- 创建 Excel 导入任务。

相同用户、相同接口和相同幂等键重复请求，应返回第一次成功结果，不重复创建业务数据。

## 12. 版本和兼容

- 当前版本为 `/api/v1`。
- 响应新增字段默认向后兼容。
- 删除字段至少经历一个弃用版本，并在 OpenAPI 标记。
- 状态值不能复用旧含义。
- 资料模板通过版本号兼容历史报单，不直接修改已发布模板。

## 13. 接口验收

- [ ] OpenAPI 与本文档路径一致。
- [ ] 请求 DTO 和响应 VO 与示例字段一致。
- [ ] 所有写接口可安全重试或明确返回冲突。
- [ ] 所有后台接口有权限编码。
- [ ] 企业数据范围由后端校验。
- [ ] 敏感响应字段已脱敏。
- [ ] 文件接口不泄露永久对象地址。
- [ ] Excel/ZIP 导出均有日志。

## 14. 实现补充接口

以下接口已在后端实现，用于补齐账号、工作台、文字资料和模板版本管理：

```text
GET  /api/v1/admin/dashboard
GET  /api/v1/admin/accounts
POST /api/v1/admin/accounts
PUT  /api/v1/admin/accounts/{userId}/roles
PUT  /api/v1/admin/accounts/{userId}/password
GET  /api/v1/admin/companies/{companyId}
POST /api/v1/admin/companies
PATCH /api/v1/admin/companies/{companyId}
DELETE /api/v1/admin/companies/{companyId}?version={version}
POST /api/v1/admin/companies/{companyId}/members
GET  /api/v1/admin/companies/{companyId}/client-accounts
POST /api/v1/admin/companies/{companyId}/client-accounts
PUT  /api/v1/admin/companies/{companyId}/client-accounts/{userId}/password
GET  /api/v1/admin/cases/{caseId}
PUT  /api/v1/cases/{caseId}/materials/{materialId}/text
GET  /api/v1/files?materialId={materialId}
POST /api/v1/admin/templates/{sourceTemplateId}/versions
POST /api/v1/admin/templates/{templateId}/publish
GET  /api/v1/audit/operations
GET  /api/v1/audit/file-access
GET  /api/v1/audit/downloads
```

后台账号管理只允许 `SUPER_ADMIN`；企业客户账号入口需要 `company:view` 并校验企业成员关系；工作台需要 `dashboard:view`；审计查询需要 `audit:view`。本地客户端开发登录允许请求体 `{}`，此时使用 `local` Profile 自动创建的模拟账号；`prod` Profile 中该接口不可用。

## 15. OpenAPI 中文契约与前端代码生成

后端 Springdoc 地址：

```text
Swagger UI： http://127.0.0.1:8080/swagger-ui.html
OpenAPI：   http://127.0.0.1:8080/v3/api-docs
JSON 快照： openapi/万企商服通_OpenAPI_V1.json
```

当前契约版本为 `1.0.0`，包含 58 个路径、66 个操作。接口标题、分组、操作摘要、响应说明和主要字段说明均使用中文；`operationId` 保留稳定的英文命名，便于 TypeScript 生成器生成可维护的函数名，例如 `authClientLogin`、`adminCompanyCreateClientAccount`、`categoryMatchRecommend`、`caseCreate`、`exportZip`。

## 16. 营业执照 OCR 与品类匹配

```http
POST /api/v1/category-match/recognize
Content-Type: multipart/form-data
Authorization: Bearer <CLIENT_TOKEN>
```

- 表单字段名为 `file`，只接受 JPG/JPEG/PNG，最大 10MB，并校验扩展名、MIME 与文件头。
- 图片仅在请求内读取并发送至腾讯云 `BizLicenseOCR`，不写入文件表、对象存储或报单。
- 成功返回 `companyName`、`businessScope`、`establishedDate`、`creditCode`、`registeredCapital` 和上游 `providerRequestId`。
- `OCR_NOT_CONFIGURED`（503）表示服务端未配置腾讯云凭据；`OCR_NO_BIZ_LICENSE`（422）表示未识别出营业执照；`OCR_SERVICE_UNAVAILABLE`（502）表示腾讯云暂时不可用。

```http
POST /api/v1/category-match/recommend
Content-Type: application/json
Authorization: Bearer <CLIENT_TOKEN>

{
  "companyName": "星澜数字科技（测试）有限公司",
  "businessScope": "软件开发；信息技术咨询服务；电子产品销售"
}
```

`businessScope` 必填且不超过 5000 字，`companyName` 可选且不超过 128 字。响应返回 `industryCount`、`categoryCount` 和按 `industries[].categories[]` 分组的 `categoryCode/categoryName/matchedKeywords/score/reason`。该分数是可解释的规则匹配分数，不是机器学习概率；零命中时返回空数组，不返回虚假推荐。

统一响应仍然是：

```ts
type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  requestId: string
  timestamp: string
}
```

OpenAPI 的生成类型会保留这层包装。管理后台页面继续调用 `apiRequest<T>` 自动取出 `data`；直接使用 Orval 函数时需要读取返回值的 `.data`。

### 15.1 导出和校验命令

在项目根目录执行：

```powershell
pnpm api:export   # 从正在运行的后端导出中文 OpenAPI JSON
pnpm api:check    # 校验标题、版本、中文摘要、operationId 和 BearerAuth
pnpm api:generate # 根据已保存 JSON 生成两端 TypeScript API
pnpm api:sync     # 先导出最新 JSON，再生成两端 TypeScript API
```

后端未启动时，`api:export` 会明确提示连接失败，不会覆盖已有 JSON 快照。

### 15.2 生成代码目录

```text
apps/admin-web/src/api/generated/       # 后台 Orval API 和 models
apps/client-uni/src/api/generated/      # uni-app Orval API 和 models
```

`generated` 目录由 Orval 管理，禁止手工修改。后台请求通过 `src/api/generated-client.ts` 接入现有 Token、Request ID 和错误处理；uni-app 请求通过同名适配器接入 `uni.request`。文件上传不能调用普通生成函数，应使用 `apps/client-uni/src/api/upload.ts` 的 `uni.uploadFile` 适配器。

### 15.3 中文化维护规则

2026-10-14 报单契约更新：客户/后台列表 CaseSummary 新增 `submittedAt`（最近提交时间）；无时区偏移的业务时间按北京时间理解。列表按最近提交时间排列，未提交回落创建时间。`updatedAt` 表示报单及资料最近更新时间。`POST /api/v1/admin/cases/{id}/status` 进入 PROCESSING/COMPLETED 时，所有必填资料必须已通过或无需提交，否则 HTTP 409、`CASE_REVIEW_INCOMPLETE`。补件仍传具体 `materialIds` 和客户可见 `clientMessage`，不自动把全部未审核项当补件。OpenAPI 和两端生成类型已同步。

- 新增 Controller 必须在 `services/api/src/main/java/com/wqst/api/config/OpenApiConfig.java` 增加中文 Tag 和每个方法的中文摘要。
- 不允许依赖 Springdoc 默认的 `*-controller` 分组或默认 `list_1`、`create_1` 等自增 operationId。
- 改动请求/响应 record 后，必须重新导出 JSON、重新生成代码，并运行两端 TypeScript 检查。
- 公开接口在 OpenAPI 中显式标记为无需 Bearer 鉴权；其余接口默认声明 `BearerAuth`。
