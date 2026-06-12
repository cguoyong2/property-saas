# backend/AGENTS.md

## 后端开发约束

1. 使用 Java 17 + Spring Boot 3.x + Maven。
2. 推荐基础包名：`com.yongquan.propertysaas`。
3. 每个业务模块采用：`controller`、`service`、`service.impl`、`mapper`、`domain.entity`、`domain.dto`、`domain.vo`、`domain.query`。
4. MyBatis-Plus 仅用于基础 CRUD，复杂账务、报表、权限查询必须显式 SQL 并写清条件。
5. 所有金额字段使用 `BigDecimal`，数据库使用 `DECIMAL(12,2)` 或更高精度。
6. 所有新增/修改/删除/审核/作废/退款/导入/导出接口必须记录操作日志。
7. 支付回调、退款回调、第三方接口回调必须幂等。
8. 所有列表查询必须分页，默认 pageSize 不超过 20，最大不超过 200。
9. 所有导出必须异步化或限制数据量，禁止一次性导出全库。
10. 所有涉及租户业务数据的 Mapper 必须走租户拦截器或显式 tenant_id 条件。

## 必须实现的公共能力

- TenantContext：当前租户上下文。
- ProjectDataScope：当前用户项目/楼栋/本人数据范围。
- OperationLogAspect：高风险操作日志。
- IdempotentService：支付、导入、回调幂等。
- FileService：统一文件鉴权与对象存储路径。
- DictService：租户级字典与平台级字典。
- ModuleLicenseService：套餐模块授权和额度校验。
