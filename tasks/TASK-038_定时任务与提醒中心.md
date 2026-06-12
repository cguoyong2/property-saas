# TASK-038 定时任务与提醒中心

## 任务目标

补齐工程包内可落地的定时任务与提醒中心能力，将已有工单 SLA、巡检漏检、租赁合同到期提醒和站内信记录串成统一作业入口。

## 执行内容

1. 后端新增 `job` 模块，提供手动触发接口和统一执行结果。
2. 启用 Spring Scheduling，并通过 `property-saas.job.enabled` 控制定时任务是否自动运行。
3. 实现跨租户定时执行时的租户范围扫描，避免依赖登录用户上下文导致定时任务空跑。
4. 新增站内信派发任务，将 `message_record` 中 `channel=SITE`、`send_status=PENDING` 的记录标记为 `SENT`。
5. 新增 `V22__job_permissions.sql`，补齐任务中心权限。
6. 更新 OpenAPI、权限矩阵、迁移说明、交付报告和完成包。

## 交付物

| 文件 | 说明 |
|---|---|
| `backend/src/main/java/com/yongquan/propertysaas/job/**` | 任务中心 Controller、Service、Repository、Scheduler 和配置 |
| `backend/src/main/resources/db/migration/V22__job_permissions.sql` | 开发迁移权限初始化 |
| `backend/src/main/resources/db/migration-prod/V22__job_permissions.sql` | 生产迁移权限初始化 |
| `openapi/openapi.yaml` | 新增 Job tag 和任务中心接口 |
| `docs/reports/TASK-038_定时任务与提醒中心报告.md` | 本任务执行报告 |

## 验收标准

1. `mvn -q test` 通过。
2. `openapi/openapi.yaml` 可被 YAML 解析。
3. 默认配置下定时任务不自动执行，只有 `PROPERTY_SAAS_JOB_ENABLED=true` 时启用。
4. 手动触发接口均带权限标识。
5. 完成包不包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads` 等构建产物或上传文件。

## 边界说明

短信、微信模板消息和真实外部消息通道仍按项目方确认后置到正式部署或厂商资料齐备后接入。本任务仅闭环工程包内的站内信派发和业务提醒记录。
