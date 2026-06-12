INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(83001, 0, '会员列表', 'API', 'base:member:list', '/base/members', '/api/base/members', 'base', 301, 1, 'ACTIVE'),
(83002, 0, '房屋绑定审核列表', 'API', 'base:memberBinding:list', '/base/member-bindings', '/api/base/member-bindings', 'base', 302, 1, 'ACTIVE'),
(83003, 0, '房屋绑定审核', 'API', 'base:memberBinding:audit', '/base/member-bindings/:bindId/audit', '/api/base/member-bindings/{bindId}/audit', 'base', 303, 1, 'ACTIVE'),
(83004, 0, '房屋绑定解绑', 'API', 'base:memberBinding:unbind', '/base/member-bindings/:bindId/unbind', '/api/base/member-bindings/{bindId}/unbind', 'base', 304, 1, 'ACTIVE')
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
(885001, 1, 1001, 83001),
(885002, 1, 1001, 83002),
(885003, 1, 1001, 83003),
(885004, 1, 1001, 83004),
(886001, 2, 2001, 83001),
(886002, 2, 2001, 83002),
(886003, 2, 2001, 83003),
(886004, 2, 2001, 83004)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
