INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_order, visible, status)
VALUES (89306, 0, '账单自动生成任务', 'API', 'job:fee:bill:generate', '/jobs/fee-bill-generate', '/api/jobs/fee-bill-generate', 'job', 946, 1, 'ACTIVE')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), permission_code = VALUES(permission_code), api_path = VALUES(api_path), status = VALUES(status);
