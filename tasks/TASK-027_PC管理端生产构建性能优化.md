# TASK-027 PC 管理端生产构建性能优化

## 目标

降低 PC 管理端生产构建单个 JS chunk 体积，关闭上线前风险清单中的 `R007`。

## 已完成内容

1. 将 PC 管理端路由页面改为动态导入，实现路由级懒加载。
2. 在 Vite 中配置 `manualChunks`，拆分 `vendor-vue`、`vendor-http`、`vendor-element` 和通用 vendor。
3. 将 Element Plus 从全量 `app.use(ElementPlus)` 改为按当前页面实际使用组件注册。
4. 将 `ElMessage` 顶层导入改为子模块导入，避免重新拉入完整组件库。

## 验收结果

执行：

```bash
npm run build
```

结果：通过。

关键构建产物变化：

| 项目 | 优化前 | 优化后 |
| --- | --- | --- |
| 最大 JS chunk | `1,133.85 kB` | `365.75 kB` |
| 500 kB chunk 警告 | 存在 | 已消除 |
| 入口业务 JS | 约 `1.13 MB` 混合包 | `34.25 kB` |

## 未完成事项

Element Plus CSS 仍为全量样式包，当前约 `357.33 kB`，但 gzip 后约 `47.79 kB`，且不再触发 JS chunk 风险。后续如继续追求极致首屏，可引入样式按需加载插件。
