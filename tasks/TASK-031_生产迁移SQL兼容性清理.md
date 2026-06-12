# TASK-031 生产迁移 SQL 兼容性清理

## 目标

清理 MySQL 8.0 对 `INSERT ... ON DUPLICATE KEY UPDATE` 中 `VALUES()` 写法的弃用警告，确保生产迁移脚本在 MySQL 8.0 环境下可无弃用警告执行。

## 已完成内容

1. 将本地迁移、生产迁移和生产最小初始化脚本中的 `VALUES(col)` 改为 MySQL 8.0 推荐的别名引用写法。
2. 对带 `ON DUPLICATE KEY UPDATE` 的批量插入语句增加 `AS new` 插入别名。
3. 覆盖范围：
   - `backend/src/main/resources/db/migration`
   - `backend/src/main/resources/db/migration-prod`
   - `sql/seed_prod_minimal.sql`
4. 更新 TASK-028、交付前代码审查报告、发行说明和工程包清单，将 MySQL 8.0 `VALUES()` 弃用警告标记为已整改。

## 验收结果

生产迁移抽样结果：

| 检查项 | 结果 |
| --- | ---: |
| 生产临时库 | `property_saas_prod_task031` |
| `information_schema.tables` 表数量 | `58` |
| `sys_menu` 菜单数量 | `141` |
| Flyway 成功迁移数 | `18` |
| 最新迁移 | `19 operation log permissions` |
| 迁移日志弃用警告 | `0` |

执行命令：

```bash
rg -n "VALUES\(" backend/src/main/resources/db/migration backend/src/main/resources/db/migration-prod sql/seed_prod_minimal.sql
mvn -q clean -DskipTests package
mvn -q test
java -jar target/property-saas-backend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none
rg -n "VALUES function|deprecated|Error|Exception" /tmp/task031-prod-migration.log
npm run build
npx vue-tsc --noEmit
npm run build:h5
npm run build:mp-weixin
```

结果：通过。

## 残余风险

1. 真实生产部署仍需在目标 MySQL 小版本上执行一次预生产迁移演练，并保留完整迁移日志。
2. 本地默认迁移目录仍包含历史开发便利性的 `CREATE DATABASE property_saas; USE property_saas;`，不适合作为任意临时库名的应用内迁移验证入口；生产验证应继续使用 `prod` profile 和 `migration-prod`。

## 未完成事项

无阻塞事项。建议下一步进入上线前最终清单复核或按项目负责人优先级处理真实微信支付资料接入。
