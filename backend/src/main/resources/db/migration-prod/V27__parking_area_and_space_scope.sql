CREATE TABLE IF NOT EXISTS base_parking_area (
  area_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  area_name VARCHAR(80) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_parking_area_name (tenant_id, project_id, area_name),
  KEY idx_parking_area_project (tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车位区域表';

ALTER TABLE base_parking_space
  ADD COLUMN building_id BIGINT NULL AFTER project_id,
  ADD COLUMN unit_id BIGINT NULL AFTER building_id,
  ADD COLUMN area_id BIGINT NULL AFTER unit_id;

ALTER TABLE base_parking_space
  ADD KEY idx_space_house (tenant_id, project_id, building_id, unit_id, house_id),
  ADD KEY idx_space_area (tenant_id, project_id, area_id);

INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(84011, 0, '车位区域列表', 'API', 'base:parkingArea:list', '/base/parking-areas', '/api/base/parking-areas', 'base', 411, 1, 'ACTIVE'),
(84012, 0, '车位区域创建', 'API', 'base:parkingArea:create', '/base/parking-areas', '/api/base/parking-areas', 'base', 412, 1, 'ACTIVE'),
(84013, 0, '车位区域修改', 'API', 'base:parkingArea:update', '/base/parking-areas/:areaId', '/api/base/parking-areas/{areaId}', 'base', 413, 1, 'ACTIVE')
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
(887011, 1, 1001, 84011),
(887012, 1, 1001, 84012),
(887013, 1, 1001, 84013),
(888011, 2, 2001, 84011),
(888012, 2, 2001, 84012),
(888013, 2, 2001, 84013)
AS new
ON DUPLICATE KEY UPDATE role_id = new.role_id, menu_id = new.menu_id;
