CREATE TABLE IF NOT EXISTS message_template (
  template_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  template_code VARCHAR(80) NOT NULL,
  template_name VARCHAR(100) NOT NULL,
  channel VARCHAR(32) NOT NULL COMMENT 'SITE/SMS/WECHAT',
  title_template VARCHAR(200) NULL,
  content_template TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_message_template_code (tenant_id, template_code, channel),
  KEY idx_message_template (tenant_id, channel, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板表';

INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(89401, 0, '消息模板列表', 'API', 'service:messageTemplate:list', '/service/message-templates', '/api/service/message-templates', 'service', 807, 1, 'ACTIVE'),
(89402, 0, '消息模板创建', 'API', 'service:messageTemplate:create', '/service/message-templates', '/api/service/message-templates', 'service', 808, 1, 'ACTIVE'),
(89403, 0, '消息模板修改', 'API', 'service:messageTemplate:update', '/service/message-templates/:templateId', '/api/service/message-templates/{templateId}', 'service', 809, 1, 'ACTIVE'),
(89404, 0, '消息派发补偿', 'API', 'service:message:dispatch', '/service/messages/dispatch-pending', '/api/service/messages/dispatch-pending', 'service', 810, 1, 'ACTIVE'),
(89405, 0, '消息失败重试', 'API', 'service:message:retry', '/service/messages/retry-failed', '/api/service/messages/retry-failed', 'service', 811, 1, 'ACTIVE')
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
(894010, 1, 1001, 89401),
(894011, 1, 1001, 89402),
(894012, 1, 1001, 89403),
(894013, 1, 1001, 89404),
(894014, 1, 1001, 89405),
(894020, 2, 2001, 89401),
(894021, 2, 2001, 89402),
(894022, 2, 2001, 89403),
(894023, 2, 2001, 89404),
(894024, 2, 2001, 89405)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
