INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(88101, 0, '工单列表', 'API', 'service:workorder:list', '/service/workorders', '/api/service/workorders', 'service', 811, 1, 'ACTIVE'),
(88102, 0, '工单创建', 'API', 'service:workorder:create', '/service/workorders', '/api/service/workorders', 'service', 812, 1, 'ACTIVE'),
(88103, 0, '工单详情', 'API', 'service:workorder:view', '/service/workorders/:workOrderId', '/api/service/workorders/{workOrderId}', 'service', 813, 1, 'ACTIVE'),
(88104, 0, '工单受理', 'API', 'service:workorder:accept', '/service/workorders/:workOrderId/accept', '/api/service/workorders/{workOrderId}/accept', 'service', 814, 1, 'ACTIVE'),
(88105, 0, '工单驳回', 'API', 'service:workorder:reject', '/service/workorders/:workOrderId/reject', '/api/service/workorders/{workOrderId}/reject', 'service', 815, 1, 'ACTIVE'),
(88106, 0, '工单处理', 'API', 'service:workorder:process', '/service/workorders/:workOrderId/process', '/api/service/workorders/{workOrderId}/start', 'service', 816, 1, 'ACTIVE'),
(88107, 0, '工单回访', 'API', 'service:workorder:revisit', '/service/workorders/:workOrderId/revisit', '/api/service/workorders/{workOrderId}/revisit', 'service', 817, 1, 'ACTIVE'),
(88108, 0, '工单取消', 'API', 'service:workorder:cancel', '/service/workorders/:workOrderId/cancel', '/api/service/workorders/{workOrderId}/cancel', 'service', 818, 1, 'ACTIVE'),
(88109, 0, '工单SLA', 'API', 'service:workorder:sla', '/service/workorders/sla', '/api/service/workorders/sla/mark-overdue', 'service', 819, 1, 'ACTIVE'),
(88110, 0, '小程序工单创建', 'API', 'app:workorder:create', '/pages/workorder/create', '/api/app/workorders', 'app', 911, 1, 'ACTIVE'),
(88111, 0, '小程序工单列表', 'API', 'app:workorder:list', '/pages/workorder/list', '/api/app/workorders', 'app', 912, 1, 'ACTIVE'),
(88112, 0, '小程序工单详情', 'API', 'app:workorder:view', '/pages/workorder/detail', '/api/app/workorders/{workOrderId}', 'app', 913, 1, 'ACTIVE'),
(88113, 0, '小程序工单评价', 'API', 'app:workorder:evaluate', '/pages/workorder/detail', '/api/app/workorders/{workOrderId}/evaluate', 'app', 914, 1, 'ACTIVE'),
(88114, 0, '小程序投诉建议', 'API', 'app:complaint:create', '/pages/workorder/complaint', '/api/app/complaints', 'app', 915, 1, 'ACTIVE')
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
(899001, 1, 1001, 88101),
(899002, 1, 1001, 88102),
(899003, 1, 1001, 88103),
(899004, 1, 1001, 88104),
(899005, 1, 1001, 88105),
(899006, 1, 1001, 6),
(899007, 1, 1001, 88106),
(899008, 1, 1001, 88107),
(899009, 1, 1001, 88108),
(899010, 1, 1001, 88109),
(899011, 1, 1001, 88110),
(899012, 1, 1001, 88111),
(899013, 1, 1001, 88112),
(899014, 1, 1001, 88113),
(899015, 1, 1001, 88114),
(899101, 2, 2001, 88101),
(899102, 2, 2001, 88102),
(899103, 2, 2001, 88103),
(899104, 2, 2001, 88104),
(899105, 2, 2001, 88105),
(899106, 2, 2001, 6),
(899107, 2, 2001, 88106),
(899108, 2, 2001, 88107),
(899109, 2, 2001, 88108),
(899110, 2, 2001, 88109),
(899111, 2, 2001, 88110),
(899112, 2, 2001, 88111),
(899113, 2, 2001, 88112),
(899114, 2, 2001, 88113),
(899115, 2, 2001, 88114)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
