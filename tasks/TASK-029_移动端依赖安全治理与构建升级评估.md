# TASK-029 移动端依赖安全治理与构建升级评估

## 目标

治理 `mobile-uniapp` 在 TASK-028 中遗留的 npm audit 风险，优先降低高危漏洞，同时保证 H5 和微信小程序构建不回退。

## 已完成内容

1. 基于当前 `package-lock.json` 执行 `npm audit --json` 和 `npm audit --omit=dev --json`，确认漏洞集中在 DCloud 工具链、Vite、vue-i18n、esbuild 和开发服务链路。
2. 查询 DCloud npm dist-tags 和版本依赖，确认 `3.0.0-5000720260410001` 对应 HBuilderX `5.07` 稳定线，`vue3` 标签为 alpha 线。
3. 将移动端 DCloud Vue3 工具链升级到 `3.0.0-5000720260410001`，并同步 Vue 到 `3.4.21`、`@dcloudio/types` 到 `3.4.31`。
4. 将直接 `vite` 升级到 `6.4.3`，并通过 npm `overrides` 覆盖以下高风险间接依赖：
   - `@intlify/core-base`
   - `@intlify/message-compiler`
   - `@intlify/message-resolver`
   - `@intlify/runtime`
   - `@intlify/vue-devtools`
   - `esbuild`
5. 验证 `vue-tsc`、H5 构建和微信小程序构建均通过。

## 依赖审计结果

| 口径 | 治理前 | 治理后 |
| --- | ---: | ---: |
| 全量 audit | `18` 个漏洞：`1 low`、`1 moderate`、`16 high` | `21` 个漏洞：`3 low`、`16 moderate`、`2 high` |
| 生产依赖 audit | `14` 个漏洞：`1 moderate`、`13 high` | `11` 个漏洞：`11 moderate`、`0 high` |

## 残余风险说明

1. 全量 audit 剩余 `2 high` 来自 DCloud `@dcloudio/vite-plugin-uni` 间接引入的 `express/path-to-regexp` 开发服务链路。
2. 生产依赖口径已无 high/critical，但仍有 `11 moderate`，主要来自 DCloud 内置 `uni-cloud`、`uni-push`、`uni-stat`、`uni-nvue-styler/postcss` 链路。
3. 未执行 `npm audit fix --force`，因为 npm 给出的修复路径会把 DCloud 包降到不兼容的历史版本或跨越未验证的大版本，风险高于收益。
4. 当前项目未启用 uni-cloud、uni-push、uni-stat 业务能力；上线时仍建议只发布构建产物，不对公网暴露 Vite/uni 开发服务。

## 验收命令

```bash
npm install --legacy-peer-deps --ignore-scripts
npm audit --json
npm audit --omit=dev --json
npx vue-tsc --noEmit
npm run build:h5
npm run build:mp-weixin
```

结果：通过。

## 未完成事项

DCloud 工具链自身仍存在中低风险依赖链，需等待 DCloud 官方发布兼容安全版本后再继续降低 audit 数量。
