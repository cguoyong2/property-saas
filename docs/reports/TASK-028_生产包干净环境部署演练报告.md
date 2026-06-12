# TASK-028 生产包干净环境部署演练报告

## 演练范围

- 干净 zip 解压检查。
- 生产环境变量检查。
- 生产 profile 空库迁移。
- 后端测试和可执行 jar 打包。
- PC 管理端依赖安装和生产构建。
- uni-app 移动端依赖安装、类型检查、H5 构建和微信小程序构建。

## 发现并修复的问题

### 非 Web 启动无法创建操作日志服务

- 问题：`OperationLogService` 构造器直接注入 `HttpServletRequest`，在 `--spring.main.web-application-type=none` 的迁移验证场景下没有请求对象，导致应用上下文启动失败。
- 修复：改为通过 `RequestContextHolder` 在记录日志时读取当前请求；无请求上下文时 IP 和 User-Agent 记录为 `null`，操作人类型仍按租户上下文兜底。

### 非 Web 启动仍创建 Servlet 安全链

- 问题：`SecurityConfig.securityFilterChain` 在非 Web 应用上下文中仍尝试注入 `HttpSecurity`，导致启动失败。
- 修复：为 `securityFilterChain` 增加 `@ConditionalOnWebApplication(type = SERVLET)`，保留 `PasswordEncoder` 普通 Bean。

## 验证结果

| 类别 | 命令 | 结果 |
| --- | --- | --- |
| 生产环境变量检查 | `scripts/prod-env-check.sh` | 通过；可拦截不安全默认账号 |
| 后端测试 | `mvn clean test` | 通过，`4` 个测试全部成功 |
| 后端打包 | `mvn -DskipTests package` | 通过 |
| 生产迁移 | `java -jar ... --spring.main.web-application-type=none` | 通过，空库迁移到 v19 |
| PC 依赖安装 | `npm install` | 通过，`0` 个漏洞 |
| PC 构建 | `npm run build` | 通过，最大 JS chunk `365.75 kB` |
| 移动端依赖安装 | `npm install --legacy-peer-deps --ignore-scripts` | 通过，audit 提示 `17` 个漏洞 |
| 移动端类型检查 | `npx vue-tsc --noEmit` | 通过 |
| H5 构建 | `npm run build:h5` | 通过 |
| 微信小程序构建 | `npm run build:mp-weixin` | 通过 |

## 生产迁移抽样

| 检查项 | 结果 |
| --- | ---: |
| 表数量 | `58` |
| `sys_tenant` | `0` |
| `sys_user` | `0` |
| `sys_menu` | `141` |
| `system:operationLog:list` 权限 | `1` |
| 成功迁移数 | `18` |
| 最新迁移 | `19 operation log permissions` |

## 结论

生产包干净环境部署演练通过。当前工程包可从无构建产物的 zip 干净副本完成生产迁移、后端测试打包、PC 生产构建和移动端双端构建。

## 后续建议

1. MySQL 8.0 `VALUES()` 弃用警告已在 TASK-031 通过迁移 SQL 兼容性清理关闭。
2. 按 TASK-026 建议处理移动端依赖漏洞，优先评估 uni-app 相关依赖升级影响。
3. 在真实上线前完成生产上线资料收集表中的外部资料确认。
