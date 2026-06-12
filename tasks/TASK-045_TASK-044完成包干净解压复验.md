# TASK-045 TASK-044 完成包干净解压复验

## 任务目标

从 `TASK-044` 完成版 zip 解压出全新干净目录，验证最新交付包在不依赖当前工作目录构建产物的情况下可复现后端、PC 管理端、小程序端和 OpenAPI 基础校验。

## 执行范围

1. 解压 `outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-044完成版.zip` 到 `outputs/task-045-verify`。
2. 复核 zip 条目数和排除项，确认未包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads`。
3. 在干净副本执行后端 `mvn test`。
4. 在干净副本解析 `openapi/openapi.yaml`。
5. 在干净副本安装 PC 依赖并执行 `npm run build`。
6. 在干净副本安装小程序依赖并执行 H5 与微信小程序构建。
7. 复核开发迁移与生产迁移最新版本均到 `V25__app_house_unbind_permission.sql`。

## 验收标准

- TASK-044 zip 可解压且包体排除项正确。
- 后端测试通过。
- OpenAPI YAML 可解析。
- PC 管理端生产构建通过。
- 小程序 H5 与微信小程序构建通过。
- 形成 TASK-045 复验报告并生成 TASK-045 完成包。

## 未完成事项

真实微信支付、真实生产资料、真实对象存储资料和真实第三方设备厂商联调继续按项目方确认后置到正式部署或设备安装阶段。
