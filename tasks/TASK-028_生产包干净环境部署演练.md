# TASK-028 生产包干净环境部署演练

## 目标

从上一任务完成版 zip 解压出干净副本，验证生产 profile、生产迁移、后端测试、PC 管理端构建和 uni-app 构建在无本地构建产物依赖的环境中可重复执行。

## 已完成内容

1. 从 `TASK-027` 完成版 zip 解压干净副本，并确认包内未包含 `node_modules`、`dist`、`target`、`unpackage` 等构建产物目录。
2. 执行 `scripts/prod-env-check.sh`，验证生产环境变量检查脚本可拦截不安全默认账号，并可在提供生产占位配置后通过。
3. 使用空生产临时库 `property_saas_prod_task028` 执行 `prod` profile 迁移，验证 Flyway 从空库迁移至 v19。
4. 修复干净演练暴露的非 Web 启动问题：
   - `OperationLogService` 不再构造器注入 `HttpServletRequest`，改为运行时从当前请求上下文读取 IP 和 User-Agent。
   - `SecurityConfig.securityFilterChain` 限定为 Servlet Web 应用上下文创建，避免非 Web 迁移/任务启动时要求 `HttpSecurity`。
5. 在干净副本中完成后端测试、后端可执行 jar 打包、PC 管理端依赖安装与生产构建、移动端依赖安装、类型检查、H5 构建和微信小程序构建。

## 验收结果

生产迁移抽样结果：

| 检查项 | 结果 |
| --- | ---: |
| `information_schema.tables` 表数量 | `58` |
| `sys_tenant` 种子数据 | `0` |
| `sys_user` 种子数据 | `0` |
| `sys_menu` 菜单数量 | `141` |
| `system:operationLog:list` 权限 | `1` |
| Flyway 成功迁移数 | `18` |
| 最新迁移 | `19 operation log permissions` |

执行命令：

```bash
scripts/prod-env-check.sh
mvn clean test
mvn -DskipTests package
java -jar target/property-saas-backend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none
npm install
npm run build
npm install --legacy-peer-deps --ignore-scripts
npx vue-tsc --noEmit
npm run build:h5
npm run build:mp-weixin
```

结果：通过。

## 残余风险

1. MySQL 8.0 对迁移脚本中的 `VALUES()` 提示弃用警告，不影响本次迁移成功；该问题已在 TASK-031 通过迁移 SQL 兼容性清理关闭。
2. `mobile-uniapp` 依赖安装后 `npm audit` 仍提示 `17` 个漏洞（`1 moderate`、`16 high`），与 TASK-026 的依赖安全评估一致，建议在后续依赖治理任务中统一处理。
3. 真实生产部署仍需要项目方提供真实微信支付、域名、证书、Redis、数据库、对象存储和首个管理员资料。

## 未完成事项

无阻塞交付事项。生产部署资料闭环、移动端依赖升级和迁移 SQL 兼容性清理均已进入后续任务处理。
