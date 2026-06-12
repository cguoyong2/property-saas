# 08-uni-app微信小程序页面清单

## 1. 页面列表

| 页面 | 路径建议 | 功能 | 主要接口 |
|---|---|---|---|
| 登录授权 | `/pages/login/index` | 微信授权、手机号登录、隐私协议 | `/api/app/auth/wx-login` |
| 选择小区/房屋 | `/pages/house/switch` | 多租户、多项目、多房屋切换 | `/api/app/houses` |
| 首页 | `/pages/home/index` | 当前房屋、待缴费、公告、报修入口 | `/api/app/home` |
| 房屋绑定 | `/pages/house/bind` | 提交房屋认证资料 | `/api/app/house-bindings` |
| 我的房屋 | `/pages/house/list` | 查看/切换/解绑房屋 | `/api/app/houses`、`/api/app/house-bindings/{bindId}/unbind` |
| 物业缴费 | `/pages/bill/list` | 待缴账单、筛选、合并支付 | `/api/app/bills` |
| 账单详情 | `/pages/bill/detail` | 账单明细、收据、支付状态 | `/api/app/bills/{billId}` |
| 支付收银台 | `/pages-sub/payment/cashier` | 创建订单、调起微信支付 | `/api/app/pay/orders` |
| 报事报修 | `/pages/workorder/create` | 填写类型、描述、图片 | `/api/app/workorders` |
| 工单详情 | `/pages/workorder/detail` | 进度、处理记录、评价 | `/api/app/workorders/{workOrderId}` |
| 投诉建议 | `/pages/workorder/complaint` | 投诉、建议提交 | `/api/app/complaints` |
| 通知公告 | `/pages/notice/list` | 公告列表、已读状态 | `/api/app/notices` |
| 访客邀请 | `/pages/visitor/create` | 访客信息、有效期、二维码 | `/api/app/visitors` |
| 我的车辆 | `/pages/vehicle/list` | 车牌、车位、月租状态 | `/api/app/vehicles` |
| 租赁合同 | `/pages/lease/contracts` | 查看本人合同、租金账单 | `/api/app/lease/contracts` |
| 我的 | `/pages/mine/index` | 个人资料、房屋、缴费、工单、设置 | `/api/app/mine` |

## 2. 小程序上下文

小程序登录后必须维护：

```json
{
  "memberId": 10001,
  "currentTenantId": 1,
  "currentProjectId": 1,
  "currentHouseId": 1,
  "currentBindRole": "OWNER"
}
```

## 3. 房屋绑定角色

| 角色 | 说明 | 权限 |
|---|---|---|
| OWNER | 业主/户主 | 缴费、报修、添加家属、访客、车辆 |
| FAMILY | 家属 | 报修、查看公告、访客，缴费是否允许由租户配置 |
| TENANT | 租户 | 租赁期内服务和缴费权限 |
| RESIDENT | 住户 | 基础服务权限 |
| VISITOR | 访客 | 临时通行权限 |

## 4. 小程序验收要点

- 一个微信用户可绑定多个房屋。
- 切换房屋后待缴账单、公告、工单均切换到对应租户/项目。
- 没有房屋绑定时，只展示绑定入口和公开公告。
- 支付成功后账单状态自动刷新。
- 报修提交后物业端出现待受理工单。
