INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(80001, 0, '平台工作台', 'API', 'platform:dashboard:view', '/platform/dashboard', '/api/platform/dashboard', 'platform', 1, 1, 'ACTIVE'),
(80002, 0, '租户创建', 'API', 'platform:tenant:create', '/platform/tenants', '/api/platform/tenants', 'platform', 2, 1, 'ACTIVE'),
(80003, 0, '租户详情', 'API', 'platform:tenant:view', '/platform/tenants/:tenantId', '/api/platform/tenants/{tenantId}', 'platform', 3, 1, 'ACTIVE'),
(80004, 0, '租户修改', 'API', 'platform:tenant:update', '/platform/tenants/:tenantId', '/api/platform/tenants/{tenantId}', 'platform', 4, 1, 'ACTIVE'),
(80005, 0, '租户状态', 'API', 'platform:tenant:status', '/platform/tenants/:tenantId/status', '/api/platform/tenants/{tenantId}/status', 'platform', 5, 1, 'ACTIVE'),
(80006, 0, '租户配置查看', 'API', 'platform:tenant:config:view', '/platform/tenants/:tenantId/config', '/api/platform/tenants/{tenantId}/config', 'platform', 6, 1, 'ACTIVE'),
(80007, 0, '租户配置修改', 'API', 'platform:tenant:config:update', '/platform/tenants/:tenantId/config', '/api/platform/tenants/{tenantId}/config', 'platform', 7, 1, 'ACTIVE'),
(80008, 0, '套餐列表', 'API', 'platform:package:list', '/platform/packages', '/api/platform/packages', 'platform', 8, 1, 'ACTIVE'),
(80009, 0, '套餐创建', 'API', 'platform:package:create', '/platform/packages', '/api/platform/packages', 'platform', 9, 1, 'ACTIVE'),
(80010, 0, '用量统计', 'API', 'platform:usage:view', '/platform/usage', '/api/platform/usage', 'platform', 10, 1, 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE
  menu_name = new.menu_name,
  route_path = new.route_path,
  api_path = new.api_path,
  module_code = new.module_code,
  sort_no = new.sort_no,
  visible = new.visible,
  status = new.status;

INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id)
VALUES
(880001, NULL, 1, 80001),
(880002, NULL, 1, 80002),
(880003, NULL, 1, 80003),
(880004, NULL, 1, 80004),
(880005, NULL, 1, 80005),
(880006, NULL, 1, 80006),
(880007, NULL, 1, 80007),
(880008, NULL, 1, 80008),
(880009, NULL, 1, 80009),
(880010, NULL, 1, 80010)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
