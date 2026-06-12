# TASK-031 生产迁移 SQL 兼容性清理报告

## 清理范围

- `backend/src/main/resources/db/migration`
- `backend/src/main/resources/db/migration-prod`
- `sql/seed_prod_minimal.sql`

## 修改说明

MySQL 8.0 对 `ON DUPLICATE KEY UPDATE` 中的 `VALUES(col)` 写法提示弃用。本任务将相关语句统一改为插入别名引用：

```sql
INSERT INTO table_name (...) VALUES (...)
AS new
ON DUPLICATE KEY UPDATE column_name = new.column_name;
```

该写法保持重复键更新语义不变，同时消除 MySQL 8.0 的弃用警告。

## 验证结果

| 类别 | 命令 | 结果 |
| --- | --- | --- |
| 旧写法扫描 | `rg -n "VALUES\\(" backend/src/main/resources/db/migration backend/src/main/resources/db/migration-prod sql/seed_prod_minimal.sql` | 无匹配 |
| SQL 语法抽样 | MySQL 容器内执行 `INSERT ... VALUES (...) AS new ON DUPLICATE KEY UPDATE ...` | 通过 |
| 后端打包 | `mvn -q clean -DskipTests package` | 通过 |
| 后端测试 | `mvn -q test` | 通过 |
| 生产迁移 | `java -jar ... --spring.main.web-application-type=none` | 通过，空生产临时库迁移到 v19 |
| 迁移日志扫描 | `rg -n "VALUES function|deprecated|Error|Exception" /tmp/task031-prod-migration.log` | 无匹配 |
| PC 构建 | `npm run build` | 通过 |
| 移动端类型检查 | `npx vue-tsc --noEmit` | 通过 |
| H5 构建 | `npm run build:h5` | 通过 |
| 微信小程序构建 | `npm run build:mp-weixin` | 通过 |

## 生产迁移抽样

| 检查项 | 结果 |
| --- | ---: |
| 表数量 | `58` |
| `sys_menu` 菜单数量 | `141` |
| 成功迁移数 | `18` |
| 最新迁移 | `19 operation log permissions` |

## 结论

TASK-031 完成。生产迁移 SQL 已清理 MySQL 8.0 `VALUES()` 弃用写法，生产临时空库迁移可到 v19，迁移日志未再出现弃用警告、错误或异常。

## 后续建议

1. 真实上线前在预生产 MySQL 实例上复跑 `prod` profile 迁移，并归档完整日志。
2. 继续按上线资料收集表确认真实数据库、Redis、对象存储、微信支付和首个管理员资料。
