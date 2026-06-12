INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(88301, 0, '可租资源列表', 'API', 'lease:resource:list', '/lease/resources', '/api/lease/resources', 'lease', 841, 1, 'ACTIVE'),
(88302, 0, '可租资源创建', 'API', 'lease:resource:create', '/lease/resources', '/api/lease/resources', 'lease', 842, 1, 'ACTIVE'),
(88303, 0, '意向客户列表', 'API', 'lease:customer:list', '/lease/customers', '/api/lease/customers', 'lease', 843, 1, 'ACTIVE'),
(88304, 0, '意向客户创建', 'API', 'lease:customer:create', '/lease/customers', '/api/lease/customers', 'lease', 844, 1, 'ACTIVE'),
(88305, 0, '客户跟进', 'API', 'lease:customer:follow', '/lease/customers/:customerId/follows', '/api/lease/customers/{customerId}/follows', 'lease', 845, 1, 'ACTIVE'),
(88306, 0, '租赁合同列表', 'API', 'lease:contract:list', '/lease/contracts', '/api/lease/contracts', 'lease', 846, 1, 'ACTIVE'),
(88307, 0, '租赁合同创建', 'API', 'lease:contract:create', '/lease/contracts', '/api/lease/contracts', 'lease', 847, 1, 'ACTIVE'),
(88308, 0, '租赁合同详情', 'API', 'lease:contract:view', '/lease/contracts/:contractId', '/api/lease/contracts/{contractId}', 'lease', 848, 1, 'ACTIVE'),
(88309, 0, '租赁合同生效', 'API', 'lease:contract:activate', '/lease/contracts/:contractId/activate', '/api/lease/contracts/{contractId}/activate', 'lease', 849, 1, 'ACTIVE'),
(88310, 0, '租赁合同终止', 'API', 'lease:contract:terminate', '/lease/contracts/:contractId/terminate', '/api/lease/contracts/{contractId}/terminate', 'lease', 850, 1, 'ACTIVE'),
(88311, 0, '合同到期提醒', 'API', 'lease:contract:remind', '/lease/contracts/expire-remind', '/api/lease/contracts/expire-remind', 'lease', 851, 1, 'ACTIVE'),
(88312, 0, '小程序租赁合同', 'API', 'app:lease:contract:list', '/pages/lease/contracts', '/api/app/lease/contracts', 'app', 921, 1, 'ACTIVE')
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
(899401, 1, 1001, 88301),
(899402, 1, 1001, 88302),
(899403, 1, 1001, 88303),
(899404, 1, 1001, 88304),
(899405, 1, 1001, 88305),
(899406, 1, 1001, 88306),
(899407, 1, 1001, 88307),
(899408, 1, 1001, 88308),
(899409, 1, 1001, 88309),
(899410, 1, 1001, 88310),
(899411, 1, 1001, 88311),
(899412, 1, 1001, 88312),
(899501, 2, 2001, 88301),
(899502, 2, 2001, 88302),
(899503, 2, 2001, 88303),
(899504, 2, 2001, 88304),
(899505, 2, 2001, 88305),
(899506, 2, 2001, 88306),
(899507, 2, 2001, 88307),
(899508, 2, 2001, 88308),
(899509, 2, 2001, 88309),
(899510, 2, 2001, 88310),
(899511, 2, 2001, 88311),
(899512, 2, 2001, 88312)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
