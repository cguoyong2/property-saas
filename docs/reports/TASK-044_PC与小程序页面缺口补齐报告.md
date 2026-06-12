# TASK-044 PC 与小程序页面缺口补齐报告

## 执行范围

- PC 管理端页面入口与自定义操作页。
- 微信小程序账单、工单、投诉建议、房屋解绑页面闭环。
- 后端小程序房屋解绑接口、投诉建议聚合查询接口和权限迁移。
- 任务清单、UI 页面清单、交付索引、验收记录、README、发行说明和 MANIFEST。

## 修改结果

| 类别 | 文件 | 结果 |
| --- | --- | --- |
| PC 页面 | `admin-web/src/config/pages.ts`、`admin-web/src/router/index.ts` | 新增投诉建议、消息记录、消息模板、导入批次、文件对象入口，并接入租户配置和任务中心路由 |
| PC 自定义页 | `admin-web/src/views/platform/TenantConfigView.vue`、`admin-web/src/views/operations/JobCenterView.vue` | 新增租户配置读写页和任务中心手动触发页 |
| 小程序页面 | `mobile-uniapp/src/pages/bill/detail.vue`、`mobile-uniapp/src/pages/workorder/detail.vue`、`mobile-uniapp/src/pages/workorder/complaint.vue` | 新增账单详情、工单详情/评价、投诉建议提交页面 |
| 小程序交互 | `mobile-uniapp/src/pages/bill/list.vue`、`mobile-uniapp/src/pages/workorder/list.vue`、`mobile-uniapp/src/pages/house/list.vue`、`mobile-uniapp/src/pages/home/index.vue` | 新增详情跳转、投诉建议入口和房屋自助解绑 |
| 小程序 API | `mobile-uniapp/src/api/app.ts`、`mobile-uniapp/src/store/member.ts` | 新增账单详情、工单详情、评价、投诉建议、房屋解绑 API 和当前房屋清理 |
| 后端接口 | `MemberController`、`MemberService`、`WorkOrderController`、`WorkOrderService`、`WorkOrderRepository` | 新增会员自助解绑和投诉建议聚合列表 |
| 权限迁移 | `V25__app_house_unbind_permission.sql` | 新增 `app:house:unbind` 和 `service:complaint:list` 权限 |

## 验证结果

| 验证项 | 结果 |
| --- | --- |
| `mvn test` | 通过，4 个测试全部通过 |
| `admin-web npm run build` | 通过 |
| `mobile-uniapp npm run build:h5` | 通过 |
| `mobile-uniapp npm run build:mp-weixin` | 通过 |
| 完成包复核 | 通过，包内 658 个条目，`node_modules`、`dist`、`target`、`unpackage`、`data/uploads` 排除项命中 0 |

## 说明

本任务未接入真实微信支付拉起、真实生产环境资料、真实 MinIO/S3 配置和真实第三方设备联调；这些事项仍按项目方确认后置。小程序依赖安装使用 `npm install --legacy-peer-deps` 适配 DCloud 当前 peer 依赖组合，安装审计仍显示 uni 工具链依赖树中的漏洞提示，未做破坏性升级。

## 结论

TASK-044 完成。PC 管理端和微信小程序在当前可开发范围内的页面缺口已补齐，最新完成包为 `outputs/智慧物业SaaS_Codex工程包_v1.1.1_TASK-044完成版.zip`。
