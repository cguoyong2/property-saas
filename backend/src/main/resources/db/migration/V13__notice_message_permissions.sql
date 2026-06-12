INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(88001, 0, '平台公告列表', 'API', 'platform:notice:list', '/platform/notices', '/api/platform/notices', 'platform', 11, 1, 'ACTIVE'),
(88002, 0, '平台公告发布', 'API', 'platform:notice:create', '/platform/notices', '/api/platform/notices', 'platform', 12, 1, 'ACTIVE'),
(88003, 0, '公告列表', 'API', 'service:notice:list', '/service/notices', '/api/service/notices', 'service', 801, 1, 'ACTIVE'),
(88004, 0, '公告发布', 'API', 'service:notice:create', '/service/notices', '/api/service/notices', 'service', 802, 1, 'ACTIVE'),
(88005, 0, '公告详情', 'API', 'service:notice:view', '/service/notices/:noticeId', '/api/service/notices/{noticeId}', 'service', 803, 1, 'ACTIVE'),
(88006, 0, '公告正式发布', 'API', 'service:notice:publish', '/service/notices/:noticeId/publish', '/api/service/notices/{noticeId}/publish', 'service', 804, 1, 'ACTIVE'),
(88007, 0, '公告撤回', 'API', 'service:notice:withdraw', '/service/notices/:noticeId/withdraw', '/api/service/notices/{noticeId}/withdraw', 'service', 805, 1, 'ACTIVE'),
(88008, 0, '消息记录列表', 'API', 'service:message:list', '/service/messages', '/api/service/messages', 'service', 806, 1, 'ACTIVE'),
(88009, 0, '小程序公告列表', 'API', 'app:notice:list', '/pages/notice/list', '/api/app/notices', 'app', 901, 1, 'ACTIVE')
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
(880011, NULL, 1, 88001),
(880012, NULL, 1, 88002),
(897001, 1, 1001, 88003),
(897002, 1, 1001, 88004),
(897003, 1, 1001, 88005),
(897004, 1, 1001, 88006),
(897005, 1, 1001, 88007),
(897006, 1, 1001, 88008),
(897007, 1, 1001, 88009),
(898001, 2, 2001, 88003),
(898002, 2, 2001, 88004),
(898003, 2, 2001, 88005),
(898004, 2, 2001, 88006),
(898005, 2, 2001, 88007),
(898006, 2, 2001, 88008),
(898007, 2, 2001, 88009)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
