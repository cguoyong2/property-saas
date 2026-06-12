# TASK-041 最新完成包干净解压复验

## 任务目标

基于 `outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-040完成版.zip` 执行干净解压复验，确认最新工程包可在无构建产物、无依赖目录的状态下完成关键构建与迁移文件一致性检查，并刷新最终验收口径。

## 执行范围

1. 将 TASK-040 完成包解压到独立复验目录。
2. 复核包体条目数、排除项和 TASK-040 关键新增文件。
3. 在干净副本中执行后端测试、OpenAPI 解析、PC 管理端构建、小程序类型检查和双端构建。
4. 复核开发迁移与生产迁移均已推进到 `V24__platform_monitor_permission.sql`。
5. 更新最终交付索引、最终验收记录、README、发行说明、MANIFEST 和本任务报告。

## 验收标准

- 输入包可解压，且不包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads`。
- TASK-040 运维监控页面、权限迁移和报告文件均在包内。
- `mvn -q test` 通过。
- `openapi/openapi.yaml` 可解析。
- `npm install --legacy-peer-deps --ignore-scripts` 后 PC 管理端 `npm run build` 通过。
- 小程序端 `npx vue-tsc --noEmit`、`npm run build:h5`、`npm run build:mp-weixin` 通过。
- 最终完成包重新打包并完成包体复核。

## 未完成事项

真实微信支付、生产资料、第三方厂商对接、真实对象存储、短信/微信模板消息和云监控告警仍按项目方确认后置到正式部署或设备安装阶段。
