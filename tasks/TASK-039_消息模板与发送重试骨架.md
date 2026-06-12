# TASK-039 消息模板与发送重试骨架

## 任务目标

在不接入真实短信、微信或厂商通道的前提下，补齐工程包内消息模板管理、待发送消息派发和失败消息重试骨架。

## 执行内容

1. 新增 `message_template` 表，用于租户级消息模板维护。
2. 新增消息模板列表、创建和修改接口。
3. 新增待发送消息派发接口。
4. 新增失败消息批量重试和单条重试接口。
5. `SITE` 站内信可标记为已发送；`SMS`、`WECHAT` 在真实通道未配置时写入失败原因，不外呼第三方。
6. 新增 `V23__message_template_retry_permissions.sql`，补齐权限初始化。
7. 更新 OpenAPI、权限矩阵、数据库设计说明、迁移说明和交付文档。

## 交付物

| 文件 | 说明 |
|---|---|
| `MessageTemplateView`、`MessageDispatchResult`、`MessageTemplateRequest` | 新增消息模板和派发结果对象 |
| `NoticeController`、`NoticeService`、`NoticeRepository` | 新增模板管理、派发和重试接口 |
| `V23__message_template_retry_permissions.sql` | 新增表和权限迁移 |
| `openapi/openapi.yaml` | 新增消息模板与重试接口契约 |
| `docs/reports/TASK-039_消息模板与发送重试骨架报告.md` | 本任务执行报告 |

## 验收标准

1. `mvn -q test` 通过。
2. `openapi/openapi.yaml` 可被 YAML 解析。
3. 前端和移动端构建仍可通过。
4. 完成包不包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads` 等构建产物或上传文件。

## 边界说明

真实短信、微信模板消息和第三方厂商通道仍按项目方确认后置。本任务只完成内部模板、发送状态补偿和失败原因留痕。
