# TASK-036 文件上传与对象存储闭环

## 目标

补齐项目技术文档中的文件上传、下载、对象 key、`file_object` 元数据和业务图片证据闭环，让工单和巡检的 `image_file_ids` 能关联真实文件对象，并保持租户、项目、模块隔离。

## 已完成内容

1. 新增文件对象后端接口：
   - `POST /api/files` 后台上传。
   - `GET /api/files` 后台文件列表。
   - `GET /api/files/{fileId}` 后台文件元数据。
   - `GET /api/files/{fileId}/content` 后台下载。
   - `DELETE /api/files/{fileId}` 后台逻辑删除并删除本地文件。
   - `POST /api/app/files` 小程序上传。
   - `GET /api/app/files/{fileId}/content` 小程序下载本人上传文件。
2. 新增本地文件存储实现，默认保存到 `data/uploads`，对象 key 继续遵循 `/tenant/{tenantId}/project/{projectId}/module/{module}/yyyy/mm/{fileId}.{ext}`。
3. 新增 `file:object:*` 和 `app:file:*` 权限迁移，并将小程序登录 token 权限补齐。
4. 工单创建、工单处理事件、巡检提交、巡检整改均校验 `imageFileIds` 必须属于当前租户、项目和模块。
5. 小程序报修页面新增最多 3 张图片选择、上传、预览和删除，提交时写入 `imageFileIds`。
6. dev/prod 配置新增本地存储根目录和文件大小限制，生产可通过环境变量覆盖。

## 验收结果

| 检查项 | 结果 |
| --- | --- |
| 后端单元测试 | 通过 |
| 小程序类型检查 | 通过 |
| 小程序 H5 构建 | 通过 |
| 小程序微信构建 | 通过 |
| 文件权限迁移 | 已新增 `V20__file_object_permissions.sql` |
| 完成包复核 | 通过，588 个文件，未包含依赖和构建产物目录 |

执行命令：

```bash
mvn -q test
npx vue-tsc --noEmit
npm run build:h5
npm run build:mp-weixin
zip -qr outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-036完成版.zip ...
unzip -l outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-036完成版.zip
```

结果：通过。

## 未完成事项

1. 当前存储实现为本地磁盘，生产正式对象存储 MinIO/S3 的 SDK 适配可在部署阶段按真实桶、账号和网络策略替换。
2. 业主端小程序已支持工单图片上传；巡检移动端页面尚未建设，巡检接口已具备图片文件 ID 校验能力。
