INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(86002, 0, '账单创建', 'API', 'fee:bill:create', '/fee/bills', '/api/fee/bills', 'fee', 602, 1, 'ACTIVE'),
(86003, 0, '账单详情', 'API', 'fee:bill:view', '/fee/bills/:billId', '/api/fee/bills/{billId}', 'fee', 603, 1, 'ACTIVE'),
(86004, 0, '批量生成账单', 'API', 'fee:bill:generate', '/fee/bills/generate', '/api/fee/bills/generate', 'fee', 604, 1, 'ACTIVE'),
(86005, 0, '账单导入', 'API', 'fee:bill:import', '/fee/bills/import', '/api/fee/bills/import', 'fee', 605, 1, 'ACTIVE'),
(86006, 0, '账单催缴', 'API', 'fee:bill:remind', '/fee/bills/remind', '/api/fee/bills/remind', 'fee', 606, 1, 'ACTIVE'),
(86007, 0, '账单作废', 'API', 'fee:bill:void', '/fee/bills/:billId/void', '/api/fee/bills/{billId}/void', 'fee', 607, 1, 'ACTIVE')
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
(891002, 1, 1001, 86002),
(891003, 1, 1001, 86003),
(891004, 1, 1001, 86004),
(891005, 1, 1001, 86005),
(891006, 1, 1001, 86006),
(891007, 1, 1001, 86007),
(892002, 2, 2001, 86002),
(892003, 2, 2001, 86003),
(892004, 2, 2001, 86004),
(892005, 2, 2001, 86005),
(892006, 2, 2001, 86006),
(892007, 2, 2001, 86007)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
