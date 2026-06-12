INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(7, 0, '收费报表', 'API', 'report:fee:view', '/reports/fee', '/api/report/fee/summary', 'report', 871, 1, 'ACTIVE'),
(88501, 0, '报表中心', 'API', 'report:center:view', '/reports', '/api/reports', 'report', 870, 1, 'ACTIVE'),
(88502, 0, '工单报表', 'API', 'report:workorder:view', '/reports/workorders', '/api/report/workorders/summary', 'report', 872, 1, 'ACTIVE'),
(88503, 0, '巡检报表', 'API', 'report:patrol:view', '/reports/patrol', '/api/report/patrol/summary', 'report', 873, 1, 'ACTIVE'),
(88504, 0, '租赁报表', 'API', 'report:lease:view', '/reports/lease', '/api/report/lease/summary', 'report', 874, 1, 'ACTIVE'),
(88505, 0, '平台运营报表', 'API', 'report:platform:view', '/platform/reports', '/api/report/platform/summary', 'report', 875, 1, 'ACTIVE')
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
(899800, NULL, 1, 88505),
(899801, 1, 1001, 88501),
(899802, 1, 1001, 7),
(899803, 1, 1001, 88502),
(899804, 1, 1001, 88503),
(899805, 1, 1001, 88504),
(899901, 2, 2001, 88501),
(899902, 2, 2001, 7),
(899903, 2, 2001, 88502),
(899904, 2, 2001, 88503),
(899905, 2, 2001, 88504)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
