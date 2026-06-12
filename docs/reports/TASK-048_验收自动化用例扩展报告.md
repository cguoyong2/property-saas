# TASK-048 验收自动化用例扩展报告

## 完成内容

- 新增 `scripts/integration-business-actions.sh`。
- 覆盖后台登录、工单业务动作、SLA 扫描、消息派发/重试、租赁到期提醒、门禁权限同步和导入错误 CSV 下载。
- 脚本支持 `API_BASE_URL`，可对本机或指定环境执行。
- 无导入批次时错误 CSV 下载用例会跳过，其它动作继续验收。

## 修改文件

- `scripts/integration-business-actions.sh`

## 已执行检查

- `bash -n scripts/integration-business-actions.sh`

## 后置说明

该脚本用于本机和预生产接口回归；不覆盖真实微信支付、真实生产配置和第三方设备实机联调。
