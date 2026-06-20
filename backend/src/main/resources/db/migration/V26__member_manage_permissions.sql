INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(83005, 0, '会员创建', 'API', 'base:member:create', '/base/members', '/api/base/members', 'base', 305, 1, 'ACTIVE'),
(83006, 0, '会员修改', 'API', 'base:member:update', '/base/members/:memberId', '/api/base/members/{memberId}', 'base', 306, 1, 'ACTIVE')
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
(885005, 1, 1001, 83005),
(885006, 1, 1001, 83006),
(886005, 2, 2001, 83005),
(886006, 2, 2001, 83006)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
