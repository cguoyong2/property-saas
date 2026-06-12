# TASK-037 导入导出批次体系

## 目标

补齐项目技术文档中的导入批次体系：导入文件先进入 `file_object`，业务导入通过 `source_file_id` 关联批次，支持批次查询、错误明细查询和错误报告下载。

## 已完成内容

1. 新增导入中心接口：
   - `GET /api/import/batches`：导入批次列表。
   - `GET /api/import/batches/{batchId}`：导入批次详情。
   - `GET /api/import/batches/{batchId}/errors`：错误明细分页。
   - `GET /api/import/batches/{batchId}/errors.csv`：错误报告 CSV 下载。
   - `POST /api/import/house-csv`：从已上传 CSV 导入房屋。
   - `POST /api/import/history-bill-csv`：从已上传 CSV 导入历史账单。
2. 新增 CSV 解析器和导入中心服务，复用既有房屋导入、历史账单导入业务校验。
3. `HouseImportRequest`、`BillImportRequest` 支持 `sourceFileId`，导入批次写入 `source_file_id`。
4. 新增 `V21__import_batch_permissions.sql`，补齐导入批次查询和错误明细权限。
5. 新增 CSV 模板：
   - `docs/business/templates/house_import_template.csv`
   - `docs/business/templates/history_bill_import_template.csv`
6. 更新 OpenAPI 和数据导入模板说明。

## 验收结果

| 检查项 | 结果 |
| --- | --- |
| 后端测试 | 通过 |
| OpenAPI YAML 解析 | 通过 |
| PC 管理端构建 | 通过 |
| 移动端类型检查、H5、微信构建 | 通过 |
| 导入批次权限迁移 | 已新增 dev/prod v21 |
| CSV 模板 | 已新增 |
| 完成包复核 | 通过，608 个文件，未包含依赖和构建产物目录 |

执行命令：

```bash
mvn -q test
ruby -e "require 'yaml'; YAML.load_file('openapi/openapi.yaml')"
npm run build
npx vue-tsc --noEmit
npm run build:h5
npm run build:mp-weixin
```

结果：通过。

## 未完成事项

1. 当前文件化导入支持 CSV；真正 `.xlsx` 解析可在后续按依赖安全策略引入 Apache POI 或服务端转换组件。
2. 当前已提供错误报告 CSV 下载，未将错误报告再回写为 `error_report_file_id` 文件对象。
