# TASK-022 联调验收与安全加固报告

## 1. 验收范围

本轮针对 `TASK-001` 至 `TASK-021` 已实现能力做收口验收，重点覆盖：

- 后端 Spring Boot 编译与测试；
- PC 管理端 TypeScript 校验与生产构建；
- uni-app H5 与微信小程序构建；
- 后台登录、平台接口权限、小程序会员登录；
- 小程序房屋、账单、我的等核心接口；
- 跨租户/跨会员访问拦截；
- 支付回调签名拒绝；
- 统一安全错误响应。

## 2. 安全加固项

1. `/api/app/house-bindings` 从匿名白名单移除，必须登录后访问。
2. `/api/app/house-bindings` 补充 `@RequiresPermission("app:house:bind")`。
3. 房屋绑定申请校验当前 JWT 必须为 `MEMBER`，且请求体 `tenantId/memberId` 必须与当前登录会员上下文一致。
4. Spring Security 未登录和无权限响应统一返回 `ApiResponse` JSON，包含业务错误码与 `traceId`。
5. JDBC 单条查询无结果统一映射为 `404001`，避免资源不存在时暴露 500。

## 3. 联调脚本

新增脚本：

```bash
scripts/integration-smoke.sh
```

默认连接：

```bash
http://localhost:8080
```

可覆盖后端地址：

```bash
API_BASE_URL=http://localhost:19022 ./scripts/integration-smoke.sh
```

脚本覆盖：

- `/actuator/health` 健康检查；
- 未登录访问小程序业务接口被拒绝；
- 租户后台账号登录；
- 租户账号访问平台接口被拒绝；
- A/B 租户会员登录；
- A 租户会员查询本人房屋和账单；
- B 租户会员访问 A 租户房屋账单被拒绝；
- 未登录提交房屋绑定被拒绝；
- 会员代他人提交房屋绑定被拒绝；
- 微信支付错误签名回调被拒绝。

## 4. 建议执行命令

后端：

```bash
cd backend
mvn -q -DskipTests compile
mvn -q test
```

PC 管理端：

```bash
cd admin-web
npm install --legacy-peer-deps --ignore-scripts
npx vue-tsc --noEmit
npm run build
```

微信小程序端：

```bash
cd mobile-uniapp
npm install --legacy-peer-deps --ignore-scripts
npx vue-tsc --noEmit
npm run build:h5
npm run build:mp-weixin
```

联调烟测：

```bash
cd backend
mvn -q spring-boot:run -Dspring-boot.run.arguments='--server.port=19022 --spring.flyway.enabled=false'
```

另开终端：

```bash
API_BASE_URL=http://localhost:19022 ./scripts/integration-smoke.sh
```

## 5. 数据库变更

本任务无新增数据库迁移。

## 6. 接口变更

接口路径不变；仅加固访问控制与错误响应：

- `/api/app/house-bindings` 由匿名可访问调整为会员登录且具备 `app:house:bind` 权限后访问。
- Spring Security 层 401/403 响应统一返回 `{ code, message, data, traceId }`。
- 查询资源不存在时统一返回 `404001`。

## 7. 未完成事项与上线风险

1. `mobile-uniapp` 仍存在 DCloud 旧依赖链带来的 npm audit 警告；不建议使用 `npm audit fix --force` 破坏 uni-app 版本兼容。
2. 支付回调当前为本地 HMAC 模拟验签，真实微信支付服务商模式上线前需接入平台证书、商户证书、回调报文解密和重放防护。
3. 第三方门禁/停车/梯控仍为适配层预留，真实厂商接入需补充厂商级联调用例。
4. 当前联调脚本覆盖关键烟测链路，不替代完整验收用例矩阵；正式交付前仍需按 `tests/验收测试用例.md` 做人工/自动化全量验收。
