# TASK-048 验收自动化用例扩展报告

## 完成内容

- 新增 `scripts/integration-business-actions.sh`。
- 覆盖后台登录、工单业务动作、SLA 扫描、消息派发/重试、租赁到期提醒、门禁权限同步和导入错误 CSV 下载。
- 脚本支持 `API_BASE_URL`，可对本机或指定环境执行。
- 无导入批次时错误 CSV 下载用例会跳过，其它动作继续验收。
- 继续补充现场业务收尾回归：业主/住户自动检索、检索结果小区/房号核对字段、房屋绑定审核列表、车位区域、车位、车辆、车辆品牌、车辆型号、账单自动计算结果、账单详情、收款订单和退款可选已收款账单。
- 对会改变账务金额的现金部分收款检查增加显式开关：`RUN_MUTATING_ACCOUNTING_CHECKS=true`。默认不执行改账动作，避免误跑到正式环境。

## 修改文件

- `scripts/integration-business-actions.sh`

## 已执行检查

- `bash -n scripts/integration-business-actions.sh`

## 后置说明

该脚本用于本机和预生产接口回归；不覆盖真实微信支付、真实生产配置和第三方设备实机联调。
