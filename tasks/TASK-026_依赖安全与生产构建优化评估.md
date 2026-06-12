# TASK-026 依赖安全与生产构建优化评估

## 目标

评估 PC 管理端和小程序端当前构建链路，明确上线前依赖安全处置策略。

## 已完成内容

1. PC 管理端执行 `npx vue-tsc --noEmit` 通过。
2. 小程序端执行 `npm audit --omit=dev --json`，生产依赖存在 14 个漏洞：13 个 high、1 个 moderate。
3. 漏洞主要集中在 DCloud uni-app 依赖链、`@intlify/*` 和 `esbuild`。
4. 形成 `docs/reports/TASK-026_依赖安全与生产构建优化评估.md`，明确不建议直接 `npm audit fix --force` 破坏 DCloud/uni-app 兼容。

## 建议

上线前优先锁定 DCloud 官方可用安全版本；如暂不能升级，应限制 H5 dev server 暴露、禁用未使用的 uni-cloud/uni-push/uni-stat 能力，并通过小程序运行环境和网关策略降低可利用面。
