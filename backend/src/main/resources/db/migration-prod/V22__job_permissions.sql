INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(89301, 0, '任务中心一键执行', 'API', 'job:run', '/jobs/run-all', '/api/jobs/run-all', 'job', 941, 1, 'ACTIVE'),
(89302, 0, '工单SLA超时任务', 'API', 'job:workorder:sla', '/jobs/workorder-sla', '/api/jobs/workorder-sla', 'job', 942, 1, 'ACTIVE'),
(89303, 0, '巡检漏检任务', 'API', 'job:patrol:missed', '/jobs/patrol-missed', '/api/jobs/patrol-missed', 'job', 943, 1, 'ACTIVE'),
(89304, 0, '合同到期提醒任务', 'API', 'job:lease:remind', '/jobs/lease-expire-remind', '/api/jobs/lease-expire-remind', 'job', 944, 1, 'ACTIVE'),
(89305, 0, '站内信派发任务', 'API', 'job:message:dispatch', '/jobs/message-dispatch', '/api/jobs/message-dispatch', 'job', 945, 1, 'ACTIVE')
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
(893000, NULL, 1, 89301),
(893010, 1, 1001, 89301),
(893011, 1, 1001, 89302),
(893012, 1, 1001, 89303),
(893013, 1, 1001, 89304),
(893014, 1, 1001, 89305),
(893020, 2, 2001, 89301),
(893021, 2, 2001, 89302),
(893022, 2, 2001, 89303),
(893023, 2, 2001, 89304),
(893024, 2, 2001, 89305)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
