# TASK-036 文件上传与对象存储闭环报告

## 执行范围

- 后端文件对象接口、元数据、权限和本地存储实现。
- 工单、巡检图片证据文件 ID 校验。
- 小程序报修图片上传入口。
- 开发/生产配置与迁移脚本。

## 修改结果

| 类别 | 文件 | 结果 |
| --- | --- | --- |
| 文件服务 | `backend/src/main/java/com/yongquan/propertysaas/file/**` | 新增上传、下载、列表、详情、删除、本地存储和文件元数据服务 |
| 业务校验 | `WorkOrderService`、`PatrolService` | 写入 `imageFileIds` 前校验文件租户、项目和模块 |
| 权限迁移 | `V20__file_object_permissions.sql` | 新增后台和小程序文件权限 |
| 小程序 | `mobile-uniapp/src/api/*`、`pages/workorder/create.vue` | 新增 `uni.uploadFile` 封装和报修图片上传 UI |
| 配置 | `application.yml`、`application-prod.yml` | 新增上传根目录、文件大小限制和 multipart 上限 |

## 关键接口

| 接口 | 权限 | 用途 |
| --- | --- | --- |
| `POST /api/files` | `file:object:upload` | 后台上传 |
| `GET /api/files` | `file:object:list` | 后台文件列表 |
| `GET /api/files/{fileId}/content` | `file:object:download` | 后台下载 |
| `DELETE /api/files/{fileId}` | `file:object:delete` | 后台删除 |
| `POST /api/app/files` | `app:file:upload` | 小程序上传 |
| `GET /api/app/files/{fileId}/content` | `app:file:download` | 小程序下载本人上传文件 |

## 验证结果

| 命令 | 结果 |
| --- | --- |
| `mvn -q test` | 通过 |
| `npx vue-tsc --noEmit` | 通过 |
| `npm run build:h5` | 通过 |
| `npm run build:mp-weixin` | 通过 |
| `ruby -e "require 'yaml'; YAML.load_file('openapi/openapi.yaml')"` | 通过 |
| 完成包复核 | 通过，`TASK-036完成版.zip` 共 588 个文件，未包含 `node_modules`、`dist`、`target`、`unpackage`、`data/uploads` |

## 结论

TASK-036 完成。工程包已具备文件对象元数据、上传下载、租户项目模块隔离和工单图片证据闭环；最新完成包为 `outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-036完成版.zip`。生产 MinIO/S3 真实适配仍建议在正式部署阶段结合真实对象存储资料完成。
