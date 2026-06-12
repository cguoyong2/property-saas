# TASK-023 高风险操作审计日志

## 目标

把已有 `operation_log` 表接入真实业务动作，覆盖上线前必须可追责的高风险操作。

## 已完成内容

1. 新增 `OperationLogService`、`OperationLogRepository` 和 `/api/system/operation-logs` 查询接口。
2. 新增 `system:operationLog:list` 权限和 PC 端 `/platform/audit` 操作审计页面。
3. 覆盖账单作废、退款申请、退款审批、用户状态变更、用户项目授权、角色菜单授权、合同生效、合同终止、工单派单、设备配置变更和门禁权限同步。
4. 操作日志记录操作者、租户、项目、模块、动作、对象、前后状态、原因、IP、User-Agent 和 traceId。

## 验收

- 后端 `mvn -q test` 通过。
- PC 管理端 `npx vue-tsc --noEmit` 通过。

## 未完成事项

后续可继续把更多普通 CRUD 纳入审计，但本任务优先覆盖上线前风险清单中的高风险动作。
