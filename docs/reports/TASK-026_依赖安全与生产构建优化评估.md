# TASK-026 依赖安全与生产构建优化评估

## 检查结果

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| PC 管理端类型检查 | `npx vue-tsc --noEmit` | 通过 |
| 小程序生产依赖审计 | `npm audit --omit=dev --json` | 14 个漏洞：13 high、1 moderate |

## 漏洞来源

| 依赖 | 严重度 | 说明 |
| --- | --- | --- |
| `@dcloudio/uni-app` / `@dcloudio/uni-cli-shared` 链路 | high | 牵引 uni-cloud、uni-components、uni-push、uni-stat、uni-h5 等包 |
| `@intlify/core-base` / `@intlify/message-resolver` / `@intlify/runtime` | high | vue-i18n 原型污染和 XSS 相关公告 |
| `esbuild` | moderate | dev server 可被网页发起请求读取响应，主要影响开发服务暴露场景 |

## 处置建议

1. 不建议直接执行 `npm audit fix --force`，该操作会把 DCloud 依赖链拉到不兼容的大版本，可能破坏 uni-app 编译和微信小程序产物。
2. 上线前优先确认 DCloud 官方当前稳定安全版本，按 DCloud 推荐组合升级 `@dcloudio/*`。
3. 如短期不能升级，应确保生产只发布小程序构建产物，不暴露 H5/Vite dev server。
4. 未使用 uni-cloud、uni-push、uni-stat 时，应在产品和构建配置中保持禁用，降低可利用面。
5. 对 H5 预览环境增加内网限制、访问认证和临时域名有效期控制。

## 当前结论

PC 管理端新增审计页面类型检查通过。小程序依赖风险仍需在上线前由项目负责人按 DCloud 兼容版本决策，不在本轮强制大版本升级。
