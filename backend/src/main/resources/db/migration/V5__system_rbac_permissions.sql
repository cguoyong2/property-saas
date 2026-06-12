INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(81001, 0, '部门列表', 'API', 'system:dept:list', '/system/depts', '/api/system/depts', 'system', 101, 1, 'ACTIVE'),
(81002, 0, '部门创建', 'API', 'system:dept:create', '/system/depts', '/api/system/depts', 'system', 102, 1, 'ACTIVE'),
(81003, 0, '部门修改', 'API', 'system:dept:update', '/system/depts/:deptId', '/api/system/depts/{deptId}', 'system', 103, 1, 'ACTIVE'),
(81004, 0, '用户列表', 'API', 'system:user:list', '/system/users', '/api/system/users', 'system', 104, 1, 'ACTIVE'),
(81005, 0, '用户创建', 'API', 'system:user:create', '/system/users', '/api/system/users', 'system', 105, 1, 'ACTIVE'),
(81006, 0, '用户修改', 'API', 'system:user:update', '/system/users/:userId', '/api/system/users/{userId}', 'system', 106, 1, 'ACTIVE'),
(81007, 0, '用户状态', 'API', 'system:user:status', '/system/users/:userId/status', '/api/system/users/{userId}/status', 'system', 107, 1, 'ACTIVE'),
(81008, 0, '用户项目授权', 'API', 'system:user:project', '/system/users/:userId/projects', '/api/system/users/{userId}/projects', 'system', 108, 1, 'ACTIVE'),
(81009, 0, '角色列表', 'API', 'system:role:list', '/system/roles', '/api/system/roles', 'system', 109, 1, 'ACTIVE'),
(81010, 0, '角色创建', 'API', 'system:role:create', '/system/roles', '/api/system/roles', 'system', 110, 1, 'ACTIVE'),
(81011, 0, '角色修改', 'API', 'system:role:update', '/system/roles/:roleId', '/api/system/roles/{roleId}', 'system', 111, 1, 'ACTIVE'),
(81012, 0, '角色权限查看', 'API', 'system:role:menu:view', '/system/roles/:roleId/menus', '/api/system/roles/{roleId}/menus', 'system', 112, 1, 'ACTIVE'),
(81013, 0, '角色权限修改', 'API', 'system:role:menu:update', '/system/roles/:roleId/menus', '/api/system/roles/{roleId}/menus', 'system', 113, 1, 'ACTIVE'),
(81014, 0, '菜单权限列表', 'API', 'system:menu:list', '/system/menus', '/api/system/menus', 'system', 114, 1, 'ACTIVE')
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
(881001, 1, 1001, 81001),
(881002, 1, 1001, 81002),
(881003, 1, 1001, 81003),
(881004, 1, 1001, 81004),
(881005, 1, 1001, 81005),
(881006, 1, 1001, 81006),
(881007, 1, 1001, 81007),
(881008, 1, 1001, 81008),
(881009, 1, 1001, 81009),
(881010, 1, 1001, 81010),
(881011, 1, 1001, 81011),
(881012, 1, 1001, 81012),
(881013, 1, 1001, 81013),
(881014, 1, 1001, 81014),
(882001, 2, 2001, 81001),
(882002, 2, 2001, 81002),
(882003, 2, 2001, 81003),
(882004, 2, 2001, 81004),
(882005, 2, 2001, 81005),
(882006, 2, 2001, 81006),
(882007, 2, 2001, 81007),
(882008, 2, 2001, 81008),
(882009, 2, 2001, 81009),
(882010, 2, 2001, 81010),
(882011, 2, 2001, 81011),
(882012, 2, 2001, 81012),
(882013, 2, 2001, 81013),
(882014, 2, 2001, 81014)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
