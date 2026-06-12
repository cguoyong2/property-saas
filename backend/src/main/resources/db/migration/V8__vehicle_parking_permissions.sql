INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(84001, 0, '车位列表', 'API', 'base:parkingSpace:list', '/base/parking-spaces', '/api/base/parking-spaces', 'base', 401, 1, 'ACTIVE'),
(84002, 0, '车位创建', 'API', 'base:parkingSpace:create', '/base/parking-spaces', '/api/base/parking-spaces', 'base', 402, 1, 'ACTIVE'),
(84003, 0, '车位详情', 'API', 'base:parkingSpace:view', '/base/parking-spaces/:spaceId', '/api/base/parking-spaces/{spaceId}', 'base', 403, 1, 'ACTIVE'),
(84004, 0, '车位修改', 'API', 'base:parkingSpace:update', '/base/parking-spaces/:spaceId', '/api/base/parking-spaces/{spaceId}', 'base', 404, 1, 'ACTIVE'),
(84005, 0, '车辆列表', 'API', 'base:vehicle:list', '/base/vehicles', '/api/base/vehicles', 'base', 405, 1, 'ACTIVE'),
(84006, 0, '车辆创建', 'API', 'base:vehicle:create', '/base/vehicles', '/api/base/vehicles', 'base', 406, 1, 'ACTIVE'),
(84007, 0, '车辆详情', 'API', 'base:vehicle:view', '/base/vehicles/:vehicleId', '/api/base/vehicles/{vehicleId}', 'base', 407, 1, 'ACTIVE'),
(84008, 0, '车辆修改', 'API', 'base:vehicle:update', '/base/vehicles/:vehicleId', '/api/base/vehicles/{vehicleId}', 'base', 408, 1, 'ACTIVE'),
(84009, 0, '车辆月租', 'API', 'base:vehicle:monthlyRent', '/base/vehicles/:vehicleId/monthly-rent', '/api/base/vehicles/{vehicleId}/monthly-rent', 'base', 409, 1, 'ACTIVE'),
(84010, 0, '停车同步记录', 'API', 'device:parking:list', '/device/parking', '/api/device/parking', 'device', 410, 1, 'ACTIVE')
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
(887001, 1, 1001, 84001),
(887002, 1, 1001, 84002),
(887003, 1, 1001, 84003),
(887004, 1, 1001, 84004),
(887005, 1, 1001, 84005),
(887006, 1, 1001, 84006),
(887007, 1, 1001, 84007),
(887008, 1, 1001, 84008),
(887009, 1, 1001, 84009),
(887010, 1, 1001, 84010),
(888001, 2, 2001, 84001),
(888002, 2, 2001, 84002),
(888003, 2, 2001, 84003),
(888004, 2, 2001, 84004),
(888005, 2, 2001, 84005),
(888006, 2, 2001, 84006),
(888007, 2, 2001, 84007),
(888008, 2, 2001, 84008),
(888009, 2, 2001, 84009),
(888010, 2, 2001, 84010)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
