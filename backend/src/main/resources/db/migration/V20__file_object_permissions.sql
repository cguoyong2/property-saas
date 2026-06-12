INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(89101, 0, '文件列表', 'API', 'file:object:list', '/files', '/api/files', 'file', 921, 1, 'ACTIVE'),
(89102, 0, '文件上传', 'API', 'file:object:upload', '/files/upload', '/api/files', 'file', 922, 1, 'ACTIVE'),
(89103, 0, '文件详情', 'API', 'file:object:view', '/files/:fileId', '/api/files/{fileId}', 'file', 923, 1, 'ACTIVE'),
(89104, 0, '文件下载', 'API', 'file:object:download', '/files/:fileId/content', '/api/files/{fileId}/content', 'file', 924, 1, 'ACTIVE'),
(89105, 0, '文件删除', 'API', 'file:object:delete', '/files/:fileId/delete', '/api/files/{fileId}', 'file', 925, 1, 'ACTIVE'),
(89106, 0, '小程序文件上传', 'API', 'app:file:upload', '/pages/file/upload', '/api/app/files', 'app', 926, 1, 'ACTIVE'),
(89107, 0, '小程序文件下载', 'API', 'app:file:download', '/pages/file/download', '/api/app/files/{fileId}/content', 'app', 927, 1, 'ACTIVE')
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
(891010, NULL, 1, 89101),
(891011, NULL, 1, 89102),
(891012, NULL, 1, 89103),
(891013, NULL, 1, 89104),
(891014, NULL, 1, 89105),
(891015, 1, 1001, 89101),
(891016, 1, 1001, 89102),
(891017, 1, 1001, 89103),
(891018, 1, 1001, 89104),
(891019, 1, 1001, 89105),
(891020, 2, 2001, 89101),
(891021, 2, 2001, 89102),
(891022, 2, 2001, 89103),
(891023, 2, 2001, 89104),
(891024, 2, 2001, 89105)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
