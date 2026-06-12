# TASK-037 导入导出批次体系报告

## 执行范围

- 导入批次列表、详情、错误明细和错误 CSV 下载。
- 房屋 CSV 文件导入。
- 历史账单 CSV 文件导入。
- 源文件 `source_file_id` 与 `file_object` 关联。
- OpenAPI、权限迁移和导入模板文档。

## 修改结果

| 类别 | 文件 | 结果 |
| --- | --- | --- |
| 导入中心 | `backend/src/main/java/com/yongquan/propertysaas/importing/**` | 新增批次查询、错误查询、CSV 导入和错误报告下载 |
| 文件关联 | `HouseImportRequest`、`BillImportRequest`、导入 Repository | 批次写入 `source_file_id` |
| 文件读取 | `FileObjectService` | 新增业务源文件读取校验 |
| 权限迁移 | `V21__import_batch_permissions.sql` | 新增 `import:batch:list/view/errors` |
| 模板文档 | `docs/business/templates/*.csv`、`13-数据导入模板说明.md` | 新增可直接导入 CSV 模板和接口流程 |
| 接口契约 | `openapi/openapi.yaml` | 新增 Import tag、批次接口和 CSV 导入接口 |

## 关键流程

1. 调用 `POST /api/files` 上传 CSV，`moduleCode=import`。
2. 使用返回的 `fileId` 调用 `POST /api/import/house-csv` 或 `POST /api/import/history-bill-csv`。
3. 导入服务读取 `file_object` 中的源文件，校验租户、项目、模块一致。
4. 复用现有业务导入逻辑写入业务表、`import_batch` 和 `import_error_detail`。
5. 通过批次接口查询结果，通过 `errors.csv` 下载错误报告。

## 验证结果

| 命令 | 结果 |
| --- | --- |
| `mvn -q test` | 通过 |
| `ruby -e "require 'yaml'; YAML.load_file('openapi/openapi.yaml')"` | 通过 |
| `npm run build` | 通过 |
| `npx vue-tsc --noEmit` | 通过 |
| `npm run build:h5` | 通过 |
| `npm run build:mp-weixin` | 通过 |
| 完成包复核 | 通过，`TASK-037完成版.zip` 共 608 个文件，未包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads` |

## 结论

TASK-037 完成。工程包已具备基于源文件的导入批次闭环、错误明细追踪和错误报告下载能力；最新完成包为 `outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-037完成版.zip`。后续如需支持原生 Excel `.xlsx`，建议在依赖安全评估后单独引入解析组件。
