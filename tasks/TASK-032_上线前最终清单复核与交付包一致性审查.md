# TASK-032 上线前最终清单复核与交付包一致性审查

## 目标

在 TASK-031 完成后，对上线前资料、风险清单、最终验收记录、任务清单、发行说明和交付包内容做一次收口复核，确保当前工程包文档不再引用过期风险或旧完成包口径。

## 已完成内容

1. 新增 TASK-032 执行报告，记录当前上线前最终复核范围、检查结果和剩余外部依赖。
2. 更新最终交付验收记录，将当前最新验收对象补充为 TASK-032 完成包，并同步 TASK-027 至 TASK-031 后已关闭的问题。
3. 更新上线前风险清单，明确已关闭的 PC chunk 风险、生产迁移 SQL 兼容性警告和移动端生产依赖高危风险。
4. 更新任务总清单、发行说明和 Manifest，纳入 TASK-032 任务与报告。
5. 执行包体一致性、SQL 兼容性、构建产物排除和文档陈旧引用扫描。

## 验收结果

| 检查项 | 结果 |
| --- | --- |
| TASK-031 完成包可读 | 通过，`565` 个 zip 条目 |
| 当前源码清单规模 | 通过，排除构建产物后 `401` 个文件 |
| 迁移 SQL 旧写法扫描 | 通过，无 `VALUES(` 残留 |
| 文档陈旧引用扫描 | 通过，除历史基线说明和本任务命令文本外，未发现旧 TASK-022 包、PC chunk 警告和旧移动端 audit 口径 |
| TASK-032 完成包检查 | 通过，包含 TASK-032 文档且不包含构建产物目录 |

执行命令：

```bash
unzip -l outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-031完成版.zip
find . -type f -not -path '*/node_modules/*' -not -path '*/dist/*' -not -path '*/target/*' -not -path '*/unpackage/*' | wc -l
rg -n "VALUES\(" backend/src/main/resources/db/migration backend/src/main/resources/db/migration-prod sql/seed_prod_minimal.sql
rg -n "TASK-022完成版|有 chunk size 警告|17 个 npm audit 风险|后续可清理 MySQL" . | rg -v "历史基线包|TASK-032_上线前最终清单复核与交付包一致性审查.md"
unzip -l outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-032完成版.zip
```

结果：通过。

## 残余风险

1. 真实微信支付 v3 服务商资料、证书、API v3 key、平台证书和公网回调地址仍需项目方提供后才能完成真实支付联调。
2. 第三方门禁、停车、梯控和短信厂商资料仍需项目方与厂商补齐。
3. 生产域名、HTTPS 证书、首个平台管理员、数据库备份恢复演练仍需上线前由项目团队签核。

## 未完成事项

无工程包内部阻塞项。真实上线仍必须按 `docs/deploy/生产上线资料收集表.md` 和 `docs/deploy/生产部署预案与回滚方案.md` 完成外部资料、预生产演练、支付联调和 Go/No-Go 签核。
