INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(87001, 0, '支付订单创建', 'API', 'payment:order:create', '/payment/orders', '/api/payment/orders', 'payment', 701, 1, 'ACTIVE'),
(87002, 0, '支付订单列表', 'API', 'payment:order:list', '/payment/orders', '/api/payment/orders', 'payment', 702, 1, 'ACTIVE'),
(87003, 0, '支付流水列表', 'API', 'payment:transaction:list', '/payment/transactions', '/api/payment/transactions', 'payment', 703, 1, 'ACTIVE')
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
(893001, 1, 1001, 87001),
(893002, 1, 1001, 87002),
(893003, 1, 1001, 87003),
(894001, 2, 2001, 87001),
(894002, 2, 2001, 87002),
(894003, 2, 2001, 87003)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
