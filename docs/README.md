# 万企商服通开发文档

这组文档是开发、联调、测试和交付的统一依据。建议按以下顺序阅读：

1. [开发文档总览](./00_开发文档总览.md)
2. [数据库设计规范](./01_数据库设计规范.md)
3. [后端设计与开发规范](./02_后端设计与开发规范.md)
4. [前端设计与开发规范](./03_前端设计与开发规范.md)
5. [API 接口设计](./04_API接口设计.md)
6. [后端实现与 Review 报告](./05_后端实现与Review报告.md)
7. [后端维护与 Bug 定位手册](./06_后端维护与Bug定位手册.md)
8. [工程目录与修改导航](./07_工程目录与修改导航.md)
9. [逐次变更记录](./changes/README.md)
10. [完整数据库增量脚本](../database/002_full_schema.sql)

接口契约和前端生成入口：

- [OpenAPI JSON 快照](../openapi/万企商服通_OpenAPI_V1.json)
- 根目录命令：`pnpm api:export`、`pnpm api:check`、`pnpm api:generate`、`pnpm api:sync`

如果你主要负责前端，建议先阅读第 7、8、9 项，再根据接口问题查阅第 5 项。

## 文档和代码的关系

- `apps/client-uni`：正式 uni-app 客户端骨架。
- `apps/admin-web`：管理后台骨架。
- `services/api`：Spring Boot 后端实现。
- `apps/design-preview`：高保真设计预览，不作为生产业务代码。
- `database/001_init.sql`：早期 6 张核心表骨架。
- `database/002_full_schema.sql`：本设计对应的完整业务增量结构。
- `docs/changes`：每一次代码、配置、数据库和目录结构变更的说明。

## 实现状态说明

后端认证、企业、产品、报单、文件、审核补件、导入导出、通知和审计模块已经实现，正式接口统一使用 `/api/v1`。管理后台的登录、工作台、报单、客户、业务模板、资料审核、Excel 导入和 Excel/ZIP 导出已使用真实接口；小程序业务页仍按后续迭代接入。容器、空库和 Testcontainers 实跑结果以 [后端实现与 Review 报告](./05_后端实现与Review报告.md) 为准。

## 变更维护规则

从 2026-09-24 起，每次修改都必须新增 `docs/changes/YYYY-MM-DD_NNN_主题.md`，并更新 [变更记录索引](./changes/README.md)。目录或关键入口变化时，还必须更新 [工程目录与修改导航](./07_工程目录与修改导航.md)。具体约束见项目根目录的 [`AGENTS.md`](../AGENTS.md)。
