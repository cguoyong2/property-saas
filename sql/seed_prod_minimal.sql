-- 生产最小初始化模板
-- 使用方式：
-- 1. 先通过生产迁移目录创建表和权限菜单；
-- 2. 将下方占位符替换为真实值后执行；
-- 3. 禁止把演示租户、演示支付配置、演示密码带入生产环境。

USE property_saas;

-- 初始化平台套餐，可按真实售卖策略调整。
INSERT INTO sys_tenant_package(package_id, package_name, package_code, price, billing_cycle, enabled_modules, quotas, status)
VALUES
(1, '标准版', 'STANDARD', 0.00, 'YEAR', JSON_ARRAY('base','fee','payment','service','report'), JSON_OBJECT('project',5,'user',50,'house',5000,'storageMb',10240,'sms',5000), 'ACTIVE'),
(2, '旗舰版', 'ULTIMATE', 0.00, 'YEAR', JSON_ARRAY('base','fee','payment','service','lease','patrol','device','report'), JSON_OBJECT('project',20,'user',200,'house',30000,'storageMb',102400,'sms',30000), 'ACTIVE');

-- 初始化首个平台管理员。
-- 替换项：
--   ${PLATFORM_ADMIN_ID}：平台管理员ID，例如 1。
--   ${PLATFORM_ADMIN_USERNAME}：平台管理员登录名。
--   ${PLATFORM_ADMIN_REAL_NAME}：平台管理员姓名。
--   ${PLATFORM_ADMIN_MOBILE}：平台管理员手机号。
--   ${PLATFORM_ADMIN_BCRYPT_HASH}：用 BCrypt 生成的初始密码哈希，不要填写明文密码。
--   ${PLATFORM_ADMIN_ROLE_ID}：平台管理员角色ID，例如 1。
--   ${PLATFORM_ADMIN_USER_ROLE_ID}：用户角色关系ID，例如 1。
--   ${PLATFORM_ADMIN_AUDIT_ROLE_MENU_ID}：操作审计菜单授权关系ID，例如 2。
INSERT INTO sys_user(user_id, tenant_id, username, real_name, mobile, password_hash, user_type, status)
VALUES
(${PLATFORM_ADMIN_ID}, NULL, '${PLATFORM_ADMIN_USERNAME}', '${PLATFORM_ADMIN_REAL_NAME}', '${PLATFORM_ADMIN_MOBILE}', '${PLATFORM_ADMIN_BCRYPT_HASH}', 'PLATFORM', 'ACTIVE');

INSERT INTO sys_role(role_id, tenant_id, role_name, role_code, role_level, data_scope, status)
VALUES
(${PLATFORM_ADMIN_ROLE_ID}, NULL, '平台超级管理员', 'PLATFORM_ADMIN', 'PLATFORM', 'ALL_TENANT', 'ACTIVE');

INSERT INTO sys_user_role(id, tenant_id, user_id, role_id)
VALUES
(${PLATFORM_ADMIN_USER_ROLE_ID}, NULL, ${PLATFORM_ADMIN_ID}, ${PLATFORM_ADMIN_ROLE_ID});

-- 生产租户、项目、支付配置应通过平台端开通流程创建；
-- 若必须离线初始化，需使用真实租户资料、真实商户号和密钥引用，禁止写入演示值。

-- 生产迁移已创建操作审计权限菜单 89001；首个平台管理员需要显式授权该菜单。
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id)
VALUES
(${PLATFORM_ADMIN_AUDIT_ROLE_MENU_ID}, NULL, ${PLATFORM_ADMIN_ROLE_ID}, 89001)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
