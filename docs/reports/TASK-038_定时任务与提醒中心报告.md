# TASK-038 定时任务与提醒中心报告

## 执行范围

- 统一任务中心后端模块。
- 工单 SLA 超时、巡检漏检、租赁合同到期提醒和站内信派发作业。
- 手动触发接口与定时调度入口。
- 任务权限、OpenAPI、配置和交付文档。

## 修改结果

| 类别 | 文件 | 结果 |
| --- | --- | --- |
| 调度开关 | `PropertySaasApplication`、`application*.yml` | 启用 Scheduling，新增 `property-saas.job.*` 配置，默认关闭自动调度 |
| 任务中心 | `backend/src/main/java/com/yongquan/propertysaas/job/**` | 新增作业编排、跨租户执行、手动触发和调度入口 |
| 业务提醒 | `JobRepository` | 直接按租户处理 SLA 超时、巡检漏检、合同到期提醒和站内信派发 |
| 权限迁移 | `V22__job_permissions.sql` | 新增 `job:run`、`job:workorder:sla`、`job:patrol:missed`、`job:lease:remind`、`job:message:dispatch` |
| 接口契约 | `openapi/openapi.yaml` | 新增 Job tag 和 `/api/jobs/*` 接口 |
| 文档 | 任务清单、README、发行说明、迁移说明、最终交付索引、MANIFEST | 刷新 TASK-038 完成口径 |

## 关键流程

1. 手动接口按当前租户上下文执行；平台上下文调用 `run-all` 时可扫描全部可运行租户。
2. 定时任务通过 `PROPERTY_SAAS_JOB_ENABLED` 开关控制，默认不自动执行。
3. 定时执行时不依赖登录用户和项目数据范围，按 `sys_tenant` 中可运行租户逐个处理，避免后台调度空跑。
4. 站内信派发仅处理 `SITE` 渠道；短信、微信模板消息真实通道继续等待正式部署资料或厂商资料。

## 验证结果

| 命令 | 结果 |
| --- | --- |
| `mvn -q test` | 通过 |
| `ruby -e "require 'yaml'; YAML.load_file('openapi/openapi.yaml')"` | 通过 |
| 完成包复核 | 通过，`TASK-038完成版.zip` 共 626 个文件，未包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads` |

## 结论

TASK-038 完成。工程包已具备任务中心手动触发、定时调度开关、跨租户作业执行和站内信派发闭环；最新完成包为 `outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-038完成版.zip`。真实短信、微信模板消息和第三方厂商通知通道仍按项目方确认后置。
