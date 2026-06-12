# TASK-043 生产配置一致性审计与环境检查脚本增强

## 任务目标

在不接入真实生产资料、不改业务代码的前提下，对齐生产上线资料、`.env.prod.example`、`application-prod.yml` 和 `scripts/prod-env-check.sh`，增强正式部署前的生产配置门禁。

## 执行范围

1. 补齐 `.env.prod.example` 中后端生产 profile、Redis、文件上传、任务调度和微信支付相关变量。
2. 增强 `scripts/prod-env-check.sh`，校验 profile、端口、MySQL SSL、JWT 长度和 TTL、文件存储路径、上传大小、任务开关、微信支付模式和证书路径引用。
3. 更新 `生产上线资料收集表.md`，使资料表和检查脚本覆盖项一致。
4. 更新 README、发行说明、最终交付索引、最终验收记录、MANIFEST 和 TASK-043 报告。
5. 生成 TASK-043 完成包并复核包体。

## 验收标准

- `bash -n scripts/prod-env-check.sh` 通过。
- 合法生产样例环境变量执行 `prod-env-check.sh` 通过。
- 不安全生产样例执行 `prod-env-check.sh` 失败。
- 完成包不包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads`。

## 未完成事项

真实环境变量值、真实证书路径、真实 KMS 引用、真实微信支付资料和对象存储资料仍由项目方在正式部署阶段填写。
