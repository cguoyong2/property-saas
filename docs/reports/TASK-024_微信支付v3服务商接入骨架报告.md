# TASK-024 微信支付 v3 服务商接入骨架报告

## 实现结果

- 新增 `WechatPayClient` 抽象，拆分支付业务和微信验签边界。
- 本地开发默认 `DEV_SIMULATED`，继续支持既有 HMAC 烟测。
- 生产默认 `REAL_WECHAT_V3`，要求真实证书和 API v3 key 引用，未完整接入前不会继续走模拟验签。
- 生产环境检查脚本已要求微信证书路径和 API v3 key 引用。

## 配置项

| 配置 | 说明 |
| --- | --- |
| `WECHAT_PAY_MODE` | 生产必须为 `REAL_WECHAT_V3` |
| `WECHAT_PAY_MERCHANT_PRIVATE_KEY_PATH` | 商户私钥路径或密钥系统挂载路径 |
| `WECHAT_PAY_MERCHANT_CERTIFICATE_PATH` | 商户证书路径 |
| `WECHAT_PAY_PLATFORM_CERTIFICATE_PATH` | 微信平台证书路径 |
| `WECHAT_PAY_API_V3_KEY_REF` | API v3 key 的密钥系统引用 |

## 验证

- `WechatPayClientImplTests`：通过。
- `mvn -q test`：通过。

## 剩余风险

真实微信支付 v3 的 HTTP 原始报文验签、AES-GCM resource 解密、平台证书轮换、时间戳/nonce 重放防护仍需真实商户资料到位后完成。
