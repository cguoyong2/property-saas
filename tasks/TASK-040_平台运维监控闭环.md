# TASK-040 平台运维监控闭环

## 任务目标

补齐平台运营端运维监控能力，将已有 `/api/platform/monitor` 契约从占位接口补成可用的后端聚合接口和 PC 页面。

## 执行内容

1. 新增平台监控聚合对象，覆盖接口失败、待重试接口、消息失败、待发消息、支付异常、退款异常、登录失败和高风险操作。
2. 实现 `GET /api/platform/monitor` 后端接口。
3. 新增平台监控权限迁移 `V24__platform_monitor_permission.sql`。
4. 新增 PC 管理端 `/platform/monitor` 页面，展示指标网格和告警列表。
5. 更新权限矩阵、迁移说明、任务清单、README、发行说明、最终交付索引、验收记录和 MANIFEST。

## 交付物

| 文件 | 说明 |
|---|---|
| `PlatformMonitorView`、`MonitorAlertView` | 平台监控聚合视图 |
| `PlatformTenantController`、`PlatformTenantService`、`PlatformTenantRepository` | 平台监控接口实现 |
| `admin-web/src/views/platform/PlatformMonitorView.vue` | PC 运维监控页面 |
| `V24__platform_monitor_permission.sql` | 平台监控权限迁移 |
| `docs/reports/TASK-040_平台运维监控闭环报告.md` | 本任务执行报告 |

## 验收标准

1. `mvn -q test` 通过。
2. `openapi/openapi.yaml` 可被 YAML 解析。
3. PC 管理端生产构建通过。
4. 移动端类型检查、H5 构建和微信小程序构建通过。
5. 完成包不包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads` 等构建产物或上传文件。

## 边界说明

本任务只做工程包内可查询的监控摘要和告警项，不接入真实 APM、短信告警、云监控或生产告警渠道。真实监控告警仍按正式部署阶段的运维方案接入。
