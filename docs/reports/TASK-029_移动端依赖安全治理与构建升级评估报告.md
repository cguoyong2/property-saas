# TASK-029 移动端依赖安全治理与构建升级评估报告

## 改动摘要

- `mobile-uniapp/package.json`：升级 DCloud Vue3 工具链到 `3.0.0-5000720260410001`，升级 Vue、Vite 和类型包，新增 npm `overrides`。
- `mobile-uniapp/package-lock.json`：同步锁定升级后的依赖树。

## 升级策略

本轮没有执行 `npm audit fix --force`。npm 建议的强制修复会将部分 DCloud 包降级到不适配当前 Vue3/uni-app 编译链路的历史版本，容易破坏 H5 和微信小程序构建。

采用的策略是：

1. 选择 DCloud `3.0.0-5000720260410001` 稳定线，对齐 HBuilderX `5.07` 编译器。
2. 保持 DCloud Vue3 相关包版本一致，避免混用 alpha 与稳定线。
3. 使用 `overrides` 修复可安全覆盖的底层漏洞包，包括 `@intlify/*` 和 `esbuild`。
4. 对 DCloud 插件开发服务链路中的残余漏洞做风险记录，不强行跨生态替换。

## Audit 结果

| 检查 | 治理前 | 治理后 |
| --- | ---: | ---: |
| `npm audit --json` | `18` 个漏洞，含 `16 high` | `21` 个漏洞，含 `2 high` |
| `npm audit --omit=dev --json` | `14` 个漏洞，含 `13 high` | `11` 个漏洞，`0 high` |

全量漏洞总数增加是因为 DCloud 5.07 稳定线引入了更多开发服务依赖，但高危数量从 `16` 降到 `2`。生产依赖口径已降到 `0 high`、`0 critical`。

## 构建验证

| 命令 | 结果 |
| --- | --- |
| `npx vue-tsc --noEmit` | 通过 |
| `npm run build:h5` | 通过，编译器 `5.07（vue3）` |
| `npm run build:mp-weixin` | 通过 |

## 残余风险

1. 全量 audit 剩余高危来自 `express/path-to-regexp`，由 `@dcloudio/vite-plugin-uni` 开发服务链路间接引入。
2. 生产依赖口径仍有 `11 moderate`，来自 DCloud 内置模块链路；当前项目未启用 uni-cloud、uni-push、uni-stat。
3. 上线时不应对公网暴露 `npm run dev`、`uni -p`、Vite dev server 或 H5 预览服务。

## 结论

TASK-029 完成。移动端依赖高危风险已显著降低，生产依赖 audit 已无 high/critical，H5 与微信小程序构建保持通过。
