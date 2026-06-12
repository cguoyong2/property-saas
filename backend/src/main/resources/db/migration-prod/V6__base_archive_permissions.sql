INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(82001, 0, '项目创建', 'API', 'base:project:create', '/base/projects', '/api/base/projects', 'base', 201, 1, 'ACTIVE'),
(82002, 0, '项目详情', 'API', 'base:project:view', '/base/projects/:projectId', '/api/base/projects/{projectId}', 'base', 202, 1, 'ACTIVE'),
(82003, 0, '项目修改', 'API', 'base:project:update', '/base/projects/:projectId', '/api/base/projects/{projectId}', 'base', 203, 1, 'ACTIVE'),
(82004, 0, '楼栋列表', 'API', 'base:building:list', '/base/buildings', '/api/base/buildings', 'base', 204, 1, 'ACTIVE'),
(82005, 0, '楼栋创建', 'API', 'base:building:create', '/base/buildings', '/api/base/buildings', 'base', 205, 1, 'ACTIVE'),
(82006, 0, '楼栋详情', 'API', 'base:building:view', '/base/buildings/:buildingId', '/api/base/buildings/{buildingId}', 'base', 206, 1, 'ACTIVE'),
(82007, 0, '楼栋修改', 'API', 'base:building:update', '/base/buildings/:buildingId', '/api/base/buildings/{buildingId}', 'base', 207, 1, 'ACTIVE'),
(82008, 0, '单元列表', 'API', 'base:unit:list', '/base/units', '/api/base/units', 'base', 208, 1, 'ACTIVE'),
(82009, 0, '单元创建', 'API', 'base:unit:create', '/base/units', '/api/base/units', 'base', 209, 1, 'ACTIVE'),
(82010, 0, '单元详情', 'API', 'base:unit:view', '/base/units/:unitId', '/api/base/units/{unitId}', 'base', 210, 1, 'ACTIVE'),
(82011, 0, '单元修改', 'API', 'base:unit:update', '/base/units/:unitId', '/api/base/units/{unitId}', 'base', 211, 1, 'ACTIVE'),
(82012, 0, '房屋创建', 'API', 'base:house:create', '/base/houses', '/api/base/houses', 'base', 212, 1, 'ACTIVE'),
(82013, 0, '房屋详情', 'API', 'base:house:view', '/base/houses/:houseId', '/api/base/houses/{houseId}', 'base', 213, 1, 'ACTIVE'),
(82014, 0, '房屋修改', 'API', 'base:house:update', '/base/houses/:houseId', '/api/base/houses/{houseId}', 'base', 214, 1, 'ACTIVE'),
(82015, 0, '房屋导入', 'API', 'base:house:import', '/base/houses/import', '/api/base/houses/import', 'base', 215, 1, 'ACTIVE')
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
(883001, 1, 1001, 82001),
(883002, 1, 1001, 82002),
(883003, 1, 1001, 82003),
(883004, 1, 1001, 82004),
(883005, 1, 1001, 82005),
(883006, 1, 1001, 82006),
(883007, 1, 1001, 82007),
(883008, 1, 1001, 82008),
(883009, 1, 1001, 82009),
(883010, 1, 1001, 82010),
(883011, 1, 1001, 82011),
(883012, 1, 1001, 82012),
(883013, 1, 1001, 82013),
(883014, 1, 1001, 82014),
(883015, 1, 1001, 82015),
(884001, 2, 2001, 82001),
(884002, 2, 2001, 82002),
(884003, 2, 2001, 82003),
(884004, 2, 2001, 82004),
(884005, 2, 2001, 82005),
(884006, 2, 2001, 82006),
(884007, 2, 2001, 82007),
(884008, 2, 2001, 82008),
(884009, 2, 2001, 82009),
(884010, 2, 2001, 82010),
(884011, 2, 2001, 82011),
(884012, 2, 2001, 82012),
(884013, 2, 2001, 82013),
(884014, 2, 2001, 82014),
(884015, 2, 2001, 82015)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
