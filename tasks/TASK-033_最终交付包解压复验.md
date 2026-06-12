# TASK-033 最终交付包解压复验

## 目标

从 `TASK-032` 完成版 zip 解压干净副本，复验交付包本身是否可读、可构建、可迁移，并将最终验收记录更新到 `TASK-033` 口径。

## 已完成内容

1. 解压 `outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-032完成版.zip` 到 `outputs/task033-acceptance`。
2. 检查干净副本中不包含 `node_modules`、`dist`、`target`、`unpackage` 等构建产物目录。
3. 在干净副本中复验迁移 SQL 兼容性、后端测试、后端打包、PC 构建、移动端类型检查、H5 构建和微信小程序构建。
4. 使用干净副本打出的后端 jar 执行 `prod` profile 非 Web 生产迁移，验证空生产临时库可迁移到 v19。
5. 更新最终交付验收记录、任务清单、发行说明和 Manifest。

## 验收结果

| 检查项 | 结果 |
| --- | --- |
| TASK-032 完成包解压 | 通过 |
| 干净副本构建产物检查 | 通过，无 `node_modules`、`dist`、`target`、`unpackage` |
| 干净副本源码文件数 | `401` |
| 迁移 SQL 旧写法扫描 | 通过，无 `VALUES(` 残留 |
| 后端测试 | 通过 |
| 后端打包 | 通过 |
| PC 依赖安装和构建 | 通过，`0` 个漏洞 |
| 移动端生产依赖 audit | `0 high`、`0 critical` |
| 移动端 H5 构建 | 通过 |
| 微信小程序构建 | 通过 |
| 生产迁移 | 通过，空库迁移到 v19 |
| 迁移日志扫描 | 通过，无 `VALUES function`、`deprecated`、`Error`、`Exception` |

生产迁移抽样：

| 检查项 | 结果 |
| --- | ---: |
| 表数量 | `58` |
| `sys_menu` 菜单数量 | `141` |
| 成功迁移数 | `18` |
| 最新迁移 | `19 operation log permissions` |

执行命令：

```bash
unzip -q outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-032完成版.zip -d outputs/task033-acceptance
find . -type d \( -name node_modules -o -name dist -o -name target -o -name unpackage \) -print
rg -n "VALUES\(" backend/src/main/resources/db/migration backend/src/main/resources/db/migration-prod sql/seed_prod_minimal.sql
mvn -q test
mvn -q clean -DskipTests package
npm install --legacy-peer-deps --ignore-scripts && npm run build
npm install --legacy-peer-deps --ignore-scripts && npx vue-tsc --noEmit && npm run build:h5 && npm run build:mp-weixin
npm audit --omit=dev --json
java -jar target/property-saas-backend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none
rg -n "VALUES function|deprecated|ERROR|Error|Exception|Failed" /tmp/task033-prod-migration.log
```

结果：通过。

## 残余风险

1. 移动端全量 `npm audit` 仍包含 DCloud 开发服务链路的 `2 high`，生产依赖口径为 `0 high`、`0 critical`；生产不得暴露 Vite/uni 开发服务。
2. 真实微信支付、生产域名证书、第三方设备厂商资料、首个平台管理员资料和数据库备份恢复演练仍需项目方上线前补齐。

## 未完成事项

无工程包内部阻塞项。建议下一步由项目负责人组织真实环境资料补齐、预生产演练和 Go/No-Go 签核。
