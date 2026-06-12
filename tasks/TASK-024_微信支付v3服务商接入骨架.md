# TASK-024 微信支付 v3 服务商接入骨架

## 目标

把开发模拟支付与生产真实微信支付 v3 服务商接入边界拆开，避免生产环境继续误用本地 HMAC 模拟验签。

## 已完成内容

1. 新增 `WechatPayClient`、`WechatPayClientImpl` 和 `WechatPayProperties`。
2. 开发模式 `DEV_SIMULATED` 继续支持现有 HMAC 回调烟测。
3. 生产模式 `REAL_WECHAT_V3` 要求商户私钥、商户证书、微信平台证书和 API v3 key 引用。
4. 生产模式下不再执行开发 HMAC 验签，未接入 HTTP 原始报文验签和 resource 解密前会明确失败。
5. `.env.prod.example` 和 `scripts/prod-env-check.sh` 已补充真实微信支付配置项。

## 验收

- 新增 `WechatPayClientImplTests`，覆盖开发 HMAC 通过和生产缺配置失败。
- 后端 `mvn -q test` 通过。

## 未完成事项

真实微信支付 v3 仍需在获得商户号、证书、API v3 key、微信平台证书和公网回调地址后，接入原始报文验签、AES-GCM 解密、时间戳/nonce 重放防护和证书轮换。
