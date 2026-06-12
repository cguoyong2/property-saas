# TASK-046 PC 管理端业务动作专用交互补齐报告

## 完成内容

- 在 PC 通用列表页新增页面级和行级业务动作框架。
- 补齐账单、退款、工单、消息、租赁合同、门禁权限、导入批次的专用动作入口。
- 新增动作参数弹窗、确认框、下载处理、操作成功刷新。
- 补齐消息派发/重试、导入批次错误明细相关权限种子。

## 修改文件

- `admin-web/src/views/common/GenericListView.vue`
- `admin-web/src/api/admin.ts`
- `sql/seed_demo_data.sql`
- `sql/seed_data.sql`

## 后置说明

本任务只接入系统内已有后端接口；真实微信支付、真实生产资料、第三方设备实机联调仍按项目方确认后置。
