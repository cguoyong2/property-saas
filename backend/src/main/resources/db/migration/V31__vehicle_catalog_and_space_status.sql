CREATE TABLE IF NOT EXISTS base_vehicle_brand (
  brand_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  brand_name VARCHAR(80) NOT NULL,
  brand_code VARCHAR(80) NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vehicle_brand_name (tenant_id, brand_name),
  KEY idx_vehicle_brand_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆品牌档案表';

CREATE TABLE IF NOT EXISTS base_vehicle_model (
  model_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  brand_id BIGINT NOT NULL,
  model_name VARCHAR(100) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vehicle_model_name (tenant_id, brand_id, model_name),
  KEY idx_vehicle_model_brand (tenant_id, brand_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆型号档案表';

INSERT INTO base_vehicle_brand(brand_id, tenant_id, brand_name, brand_code, sort_no, status)
VALUES
(9100001, 1, '比亚迪', 'BYD', 10, 'ACTIVE'),
(9100002, 1, '特斯拉', 'TESLA', 20, 'ACTIVE'),
(9100003, 1, '丰田', 'TOYOTA', 30, 'ACTIVE'),
(9100004, 1, '本田', 'HONDA', 40, 'ACTIVE'),
(9100005, 1, '大众', 'VOLKSWAGEN', 50, 'ACTIVE'),
(9100006, 1, '奔驰', 'BENZ', 60, 'ACTIVE'),
(9100007, 1, '宝马', 'BMW', 70, 'ACTIVE'),
(9100008, 1, '奥迪', 'AUDI', 80, 'ACTIVE'),
(9100009, 1, '吉利', 'GEELY', 90, 'ACTIVE'),
(9100010, 1, '长安', 'CHANGAN', 100, 'ACTIVE'),
(9100011, 1, '长城', 'GWM', 110, 'ACTIVE'),
(9100012, 1, '奇瑞', 'CHERY', 120, 'ACTIVE'),
(9100013, 1, '红旗', 'HONGQI', 130, 'ACTIVE'),
(9100014, 1, '蔚来', 'NIO', 140, 'ACTIVE'),
(9100015, 1, '小鹏', 'XPENG', 150, 'ACTIVE'),
(9100016, 1, '理想', 'LI', 160, 'ACTIVE'),
(9100017, 1, '问界', 'AITO', 170, 'ACTIVE'),
(9100018, 1, '五菱', 'WULING', 180, 'ACTIVE'),
(9100019, 1, '别克', 'BUICK', 190, 'ACTIVE'),
(9100020, 1, '日产', 'NISSAN', 200, 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE brand_name = new.brand_name, brand_code = new.brand_code, sort_no = new.sort_no, status = new.status;

INSERT INTO base_vehicle_model(model_id, tenant_id, brand_id, model_name, sort_no, status)
VALUES
(9200001, 1, 9100001, '秦PLUS', 10, 'ACTIVE'),
(9200002, 1, 9100001, '宋PLUS', 20, 'ACTIVE'),
(9200003, 1, 9100001, '汉', 30, 'ACTIVE'),
(9200004, 1, 9100001, '唐', 40, 'ACTIVE'),
(9200005, 1, 9100001, '海豚', 50, 'ACTIVE'),
(9200006, 1, 9100002, 'Model 3', 10, 'ACTIVE'),
(9200007, 1, 9100002, 'Model Y', 20, 'ACTIVE'),
(9200008, 1, 9100003, '卡罗拉', 10, 'ACTIVE'),
(9200009, 1, 9100003, '凯美瑞', 20, 'ACTIVE'),
(9200010, 1, 9100003, 'RAV4荣放', 30, 'ACTIVE'),
(9200011, 1, 9100004, '雅阁', 10, 'ACTIVE'),
(9200012, 1, 9100004, '思域', 20, 'ACTIVE'),
(9200013, 1, 9100004, 'CR-V', 30, 'ACTIVE'),
(9200014, 1, 9100005, '朗逸', 10, 'ACTIVE'),
(9200015, 1, 9100005, '速腾', 20, 'ACTIVE'),
(9200016, 1, 9100005, '迈腾', 30, 'ACTIVE'),
(9200017, 1, 9100006, 'C级', 10, 'ACTIVE'),
(9200018, 1, 9100006, 'E级', 20, 'ACTIVE'),
(9200019, 1, 9100006, 'GLC', 30, 'ACTIVE'),
(9200020, 1, 9100007, '3系', 10, 'ACTIVE'),
(9200021, 1, 9100007, '5系', 20, 'ACTIVE'),
(9200022, 1, 9100007, 'X3', 30, 'ACTIVE'),
(9200023, 1, 9100008, 'A4L', 10, 'ACTIVE'),
(9200024, 1, 9100008, 'A6L', 20, 'ACTIVE'),
(9200025, 1, 9100008, 'Q5L', 30, 'ACTIVE'),
(9200026, 1, 9100009, '帝豪', 10, 'ACTIVE'),
(9200027, 1, 9100009, '星瑞', 20, 'ACTIVE'),
(9200028, 1, 9100010, 'CS75 PLUS', 10, 'ACTIVE'),
(9200029, 1, 9100011, '哈弗H6', 10, 'ACTIVE'),
(9200030, 1, 9100012, '瑞虎8', 10, 'ACTIVE'),
(9200031, 1, 9100013, 'H5', 10, 'ACTIVE'),
(9200032, 1, 9100014, 'ES6', 10, 'ACTIVE'),
(9200033, 1, 9100015, 'P7', 10, 'ACTIVE'),
(9200034, 1, 9100016, 'L7', 10, 'ACTIVE'),
(9200035, 1, 9100017, 'M7', 10, 'ACTIVE'),
(9200036, 1, 9100018, '宏光MINIEV', 10, 'ACTIVE'),
(9200037, 1, 9100019, 'GL8', 10, 'ACTIVE'),
(9200038, 1, 9100020, '轩逸', 10, 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE model_name = new.model_name, sort_no = new.sort_no, status = new.status;

UPDATE base_parking_space s
SET s.status = 'OCCUPIED'
WHERE s.deleted = 0
  AND s.status = 'AVAILABLE'
  AND EXISTS (
    SELECT 1
    FROM base_vehicle v
    WHERE v.tenant_id = s.tenant_id
      AND v.space_id = s.space_id
      AND v.deleted = 0
  );
