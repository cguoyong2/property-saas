# TASK-039 消息模板与发送重试骨架报告

## 执行范围

- 消息模板表和模板管理接口。
- 待发送消息派发接口。
- 失败消息批量重试和单条重试接口。
- 权限迁移、OpenAPI 和交付文档。

## 修改结果

| 类别 | 文件 | 结果 |
| --- | --- | --- |
| 数据表 | `message_template`、`V23__message_template_retry_permissions.sql` | 新增租户级消息模板表和演示模板 |
| 后端接口 | `NoticeController`、`NoticeService`、`NoticeRepository` | 新增模板列表/创建/修改、待发送派发、失败重试 |
| 领域对象 | `MessageTemplateView`、`MessageDispatchResult`、`MessageTemplateRequest` | 新增模板视图、模板请求和派发结果 |
| 接口契约 | `openapi/openapi.yaml` | 新增 `/api/service/message-templates` 和 `/api/service/messages/*retry*` 等接口 |
| 权限 | `docs/business/05-权限矩阵.md`、V23 迁移 | 新增 `service:messageTemplate:*`、`service:message:dispatch`、`service:message:retry` |
| 文档 | 任务清单、README、发行说明、迁移说明、最终交付索引、MANIFEST | 刷新 TASK-039 完成口径 |

## 关键流程

1. 租户后台维护消息模板，模板按 `tenant_id + template_code + channel` 唯一。
2. `POST /api/service/messages/dispatch-pending` 处理待发送消息。
3. `POST /api/service/messages/retry-failed` 批量处理失败消息。
4. `POST /api/service/messages/{messageId}/retry` 重试单条消息。
5. `SITE` 渠道标记为 `SENT`；`SMS`、`WECHAT` 在真实通道未配置时标记为 `FAILED` 并写入原因。

## 验证结果

| 命令 | 结果 |
| --- | --- |
| `mvn -q test` | 通过 |
| `ruby -e "require 'yaml'; YAML.load_file('openapi/openapi.yaml')"` | 通过 |
| `npm run build` | 通过 |
| `npx vue-tsc --noEmit` | 通过 |
| `npm run build:h5` | 通过 |
| `npm run build:mp-weixin` | 通过 |
| 完成包复核 | 通过，`TASK-039完成版.zip` 共 633 个文件，未包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads` |

## 结论

TASK-039 完成。工程包已具备消息模板管理、待发送消息派发和失败消息重试骨架；最新完成包为 `outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-039完成版.zip`。真实短信、微信模板消息和第三方厂商通知通道仍按项目方确认后置。
