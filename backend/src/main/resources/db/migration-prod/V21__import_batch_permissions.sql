INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(89201, 0, '导入批次列表', 'API', 'import:batch:list', '/import/batches', '/api/import/batches', 'import', 931, 1, 'ACTIVE'),
(89202, 0, '导入批次详情', 'API', 'import:batch:view', '/import/batches/:batchId', '/api/import/batches/{batchId}', 'import', 932, 1, 'ACTIVE'),
(89203, 0, '导入错误明细', 'API', 'import:batch:errors', '/import/batches/:batchId/errors', '/api/import/batches/{batchId}/errors', 'import', 933, 1, 'ACTIVE')
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
(892010, 1, 1001, 89201),
(892011, 1, 1001, 89202),
(892012, 1, 1001, 89203),
(892020, 2, 2001, 89201),
(892021, 2, 2001, 89202),
(892022, 2, 2001, 89203)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
