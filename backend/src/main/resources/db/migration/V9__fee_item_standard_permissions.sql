INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(85001, 0, '收费项目列表', 'API', 'fee:item:list', '/fee/items', '/api/fee/items', 'fee', 501, 1, 'ACTIVE'),
(85002, 0, '收费项目创建', 'API', 'fee:item:create', '/fee/items', '/api/fee/items', 'fee', 502, 1, 'ACTIVE'),
(85003, 0, '收费项目详情', 'API', 'fee:item:view', '/fee/items/:itemId', '/api/fee/items/{itemId}', 'fee', 503, 1, 'ACTIVE'),
(85004, 0, '收费项目修改', 'API', 'fee:item:update', '/fee/items/:itemId', '/api/fee/items/{itemId}', 'fee', 504, 1, 'ACTIVE'),
(85005, 0, '收费标准列表', 'API', 'fee:standard:list', '/fee/standards', '/api/fee/standards', 'fee', 505, 1, 'ACTIVE'),
(85006, 0, '收费标准创建', 'API', 'fee:standard:create', '/fee/standards', '/api/fee/standards', 'fee', 506, 1, 'ACTIVE'),
(85007, 0, '收费标准详情', 'API', 'fee:standard:view', '/fee/standards/:standardId', '/api/fee/standards/{standardId}', 'fee', 507, 1, 'ACTIVE'),
(85008, 0, '收费标准修改', 'API', 'fee:standard:update', '/fee/standards/:standardId', '/api/fee/standards/{standardId}', 'fee', 508, 1, 'ACTIVE'),
(85009, 0, '收费标准绑定列表', 'API', 'fee:standardBind:list', '/fee/standard-binds', '/api/fee/standard-binds', 'fee', 509, 1, 'ACTIVE'),
(85010, 0, '收费标准绑定创建', 'API', 'fee:standardBind:create', '/fee/standard-binds', '/api/fee/standard-binds', 'fee', 510, 1, 'ACTIVE')
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
(889001, 1, 1001, 85001),
(889002, 1, 1001, 85002),
(889003, 1, 1001, 85003),
(889004, 1, 1001, 85004),
(889005, 1, 1001, 85005),
(889006, 1, 1001, 85006),
(889007, 1, 1001, 85007),
(889008, 1, 1001, 85008),
(889009, 1, 1001, 85009),
(889010, 1, 1001, 85010),
(890001, 2, 2001, 85001),
(890002, 2, 2001, 85002),
(890003, 2, 2001, 85003),
(890004, 2, 2001, 85004),
(890005, 2, 2001, 85005),
(890006, 2, 2001, 85006),
(890007, 2, 2001, 85007),
(890008, 2, 2001, 85008),
(890009, 2, 2001, 85009),
(890010, 2, 2001, 85010)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
