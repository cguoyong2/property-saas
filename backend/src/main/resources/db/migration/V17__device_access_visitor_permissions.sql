INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(88401, 0, '设备配置列表', 'API', 'device:config:list', '/device/configs', '/api/device/configs', 'device', 861, 1, 'ACTIVE'),
(88402, 0, '设备配置创建', 'API', 'device:config:create', '/device/configs', '/api/device/configs', 'device', 862, 1, 'ACTIVE'),
(88403, 0, '设备配置详情', 'API', 'device:config:view', '/device/configs/:deviceId', '/api/device/configs/{deviceId}', 'device', 863, 1, 'ACTIVE'),
(88404, 0, '设备配置修改', 'API', 'device:config:update', '/device/configs/:deviceId', '/api/device/configs/{deviceId}', 'device', 864, 1, 'ACTIVE'),
(88405, 0, '访客列表', 'API', 'device:visitor:list', '/device/visitors', '/api/device/visitors', 'device', 865, 1, 'ACTIVE'),
(88406, 0, '访客邀请创建', 'API', 'device:visitor:create', '/device/visitors', '/api/device/visitors', 'device', 866, 1, 'ACTIVE'),
(88407, 0, '门禁权限列表', 'API', 'device:access:list', '/device/access', '/api/device/access', 'device', 867, 1, 'ACTIVE'),
(88408, 0, '门禁权限创建', 'API', 'device:access:create', '/device/access', '/api/device/access', 'device', 868, 1, 'ACTIVE'),
(88409, 0, '门禁权限同步', 'API', 'device:access:sync', '/device/access/sync', '/api/device/access/sync', 'device', 869, 1, 'ACTIVE'),
(88410, 0, '门禁记录列表', 'API', 'device:access:record:list', '/device/access/records', '/api/device/access/records', 'device', 870, 1, 'ACTIVE'),
(88411, 0, '门禁记录写入', 'API', 'device:access:record:create', '/device/access/records', '/api/device/access/records', 'device', 871, 1, 'ACTIVE'),
(88412, 0, '小程序访客邀请', 'API', 'app:visitor:create', '/pages/visitor/create', '/api/app/visitors', 'app', 922, 1, 'ACTIVE')
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
(899601, 1, 1001, 88401),
(899602, 1, 1001, 88402),
(899603, 1, 1001, 88403),
(899604, 1, 1001, 88404),
(899605, 1, 1001, 88405),
(899606, 1, 1001, 88406),
(899607, 1, 1001, 88407),
(899608, 1, 1001, 88408),
(899609, 1, 1001, 88409),
(899610, 1, 1001, 88410),
(899611, 1, 1001, 88411),
(899612, 1, 1001, 88412),
(899701, 2, 2001, 88401),
(899702, 2, 2001, 88402),
(899703, 2, 2001, 88403),
(899704, 2, 2001, 88404),
(899705, 2, 2001, 88405),
(899706, 2, 2001, 88406),
(899707, 2, 2001, 88407),
(899708, 2, 2001, 88408),
(899709, 2, 2001, 88409),
(899710, 2, 2001, 88410),
(899711, 2, 2001, 88411),
(899712, 2, 2001, 88412)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
