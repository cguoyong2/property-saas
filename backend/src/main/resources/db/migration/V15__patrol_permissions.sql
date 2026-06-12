INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(88201, 0, '巡检资产列表', 'API', 'patrol:asset:list', '/patrol/assets', '/api/patrol/assets', 'patrol', 821, 1, 'ACTIVE'),
(88202, 0, '巡检资产创建', 'API', 'patrol:asset:create', '/patrol/assets', '/api/patrol/assets', 'patrol', 822, 1, 'ACTIVE'),
(88203, 0, '巡检点位列表', 'API', 'patrol:point:list', '/patrol/points', '/api/patrol/points', 'patrol', 823, 1, 'ACTIVE'),
(88204, 0, '巡检点位创建', 'API', 'patrol:point:create', '/patrol/points', '/api/patrol/points', 'patrol', 824, 1, 'ACTIVE'),
(88205, 0, '巡检计划列表', 'API', 'patrol:plan:list', '/patrol/plans', '/api/patrol/plans', 'patrol', 825, 1, 'ACTIVE'),
(88206, 0, '巡检计划创建', 'API', 'patrol:plan:create', '/patrol/plans', '/api/patrol/plans', 'patrol', 826, 1, 'ACTIVE'),
(88207, 0, '巡检任务列表', 'API', 'patrol:task:list', '/patrol/tasks', '/api/patrol/tasks', 'patrol', 827, 1, 'ACTIVE'),
(88208, 0, '巡检任务创建', 'API', 'patrol:task:create', '/patrol/tasks', '/api/patrol/tasks', 'patrol', 828, 1, 'ACTIVE'),
(88209, 0, '巡检任务详情', 'API', 'patrol:task:view', '/patrol/tasks/:taskId', '/api/patrol/tasks/{taskId}', 'patrol', 829, 1, 'ACTIVE'),
(88210, 0, '提交巡检结果', 'API', 'patrol:task:submit', '/patrol/tasks/:taskId/submit', '/api/patrol/tasks/{taskId}/submit', 'patrol', 830, 1, 'ACTIVE'),
(88211, 0, '巡检异常整改', 'API', 'patrol:task:rectify', '/patrol/tasks/:taskId/items/:itemId/rectify', '/api/patrol/tasks/{taskId}/items/{itemId}/rectify', 'patrol', 831, 1, 'ACTIVE'),
(88212, 0, '巡检漏检标记', 'API', 'patrol:task:missed', '/patrol/tasks/mark-missed', '/api/patrol/tasks/mark-missed', 'patrol', 832, 1, 'ACTIVE')
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
(899201, 1, 1001, 88201),
(899202, 1, 1001, 88202),
(899203, 1, 1001, 88203),
(899204, 1, 1001, 88204),
(899205, 1, 1001, 88205),
(899206, 1, 1001, 88206),
(899207, 1, 1001, 88207),
(899208, 1, 1001, 88208),
(899209, 1, 1001, 88209),
(899210, 1, 1001, 88210),
(899211, 1, 1001, 88211),
(899212, 1, 1001, 88212),
(899301, 2, 2001, 88201),
(899302, 2, 2001, 88202),
(899303, 2, 2001, 88203),
(899304, 2, 2001, 88204),
(899305, 2, 2001, 88205),
(899306, 2, 2001, 88206),
(899307, 2, 2001, 88207),
(899308, 2, 2001, 88208),
(899309, 2, 2001, 88209),
(899310, 2, 2001, 88210),
(899311, 2, 2001, 88211),
(899312, 2, 2001, 88212)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
