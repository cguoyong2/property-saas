INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(87004, 0, '退款列表', 'API', 'payment:refund:list', '/payment/refunds', '/api/payment/refunds', 'payment', 704, 1, 'ACTIVE'),
(87005, 0, '退款审批', 'API', 'payment:refund:audit', '/payment/refunds/:refundId/audit', '/api/payment/refunds/{refundId}/audit', 'payment', 705, 1, 'ACTIVE'),
(87006, 0, '支付对账', 'API', 'payment:reconcile:view', '/payment/reconcile', '/api/payment/reconcile', 'payment', 706, 1, 'ACTIVE')
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
(895001, 1, 1001, 5),
(895002, 1, 1001, 87004),
(895003, 1, 1001, 87005),
(895004, 1, 1001, 87006),
(896001, 2, 2001, 5),
(896002, 2, 2001, 87004),
(896003, 2, 2001, 87005),
(896004, 2, 2001, 87006)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
