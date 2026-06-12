# TASK-027 PC 管理端生产构建性能优化报告

## 改动摘要

- `admin-web/src/router/index.ts`：页面组件改为动态导入。
- `admin-web/vite.config.ts`：新增 `manualChunks`，拆分 Vue、HTTP、Element Plus 和通用 vendor。
- `admin-web/src/main.ts`：Element Plus 改为按需注册当前页面实际使用组件。
- `admin-web/src/views/auth/LoginView.vue`、`admin-web/src/views/common/GenericListView.vue`：`ElMessage` 改为子模块导入。

## 构建验证

命令：

```bash
npm run build
```

结果：通过。

构建输出中不再出现：

```text
Some chunks are larger than 500 kB after minification
```

## 构建体积

| Chunk | 体积 | gzip |
| --- | ---: | ---: |
| `vendor-element` JS | `365.75 kB` | `115.23 kB` |
| `vendor-vue` JS | `113.83 kB` | `44.39 kB` |
| `vendor` JS | `96.42 kB` | `34.31 kB` |
| `vendor-http` JS | `43.37 kB` | `16.99 kB` |
| 入口 `index` JS | `34.25 kB` | `7.68 kB` |

## 结论

`R007` 已关闭。PC 管理端生产构建不再存在超过 500 kB 的 JS chunk。
