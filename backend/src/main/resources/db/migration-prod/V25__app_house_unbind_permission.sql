INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(89501, 0, '小程序房屋解绑', 'API', 'app:house:unbind', '/pages/house/list', '/api/app/house-bindings/{bindId}/unbind', 'app', 928, 1, 'ACTIVE'),
(89502, 0, '投诉建议列表', 'API', 'service:complaint:list', '/service/complaints', '/api/service/complaints', 'service', 820, 1, 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE
  menu_name = new.menu_name,
  permission_code = new.permission_code,
  route_path = new.route_path,
  api_path = new.api_path,
  module_code = new.module_code,
  sort_no = new.sort_no,
  visible = new.visible,
  status = new.status;

INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id)
VALUES
(895020, 1, 1001, 89502),
(895021, 2, 2001, 89502)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
