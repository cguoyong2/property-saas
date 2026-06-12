-- 智慧物业管理系统 SaaS 多租户版 MySQL 8 建表脚本
-- 注意：除平台全局表外，所有业务表必须包含 tenant_id。
-- 字符集：utf8mb4，金额字段：DECIMAL，逻辑删除：deleted=0/1。

-- =========================
-- SaaS 平台与租户
-- =========================

CREATE TABLE sys_tenant_package (
  package_id BIGINT PRIMARY KEY COMMENT '套餐ID',
  package_name VARCHAR(100) NOT NULL COMMENT '套餐名称',
  package_code VARCHAR(64) NOT NULL COMMENT '套餐编码',
  price DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
  billing_cycle VARCHAR(32) NOT NULL DEFAULT 'YEAR' COMMENT '计费周期：MONTH/YEAR/ONCE',
  enabled_modules JSON NULL COMMENT '启用模块JSON数组',
  quotas JSON NULL COMMENT '额度JSON：project/user/house/storage/sms等',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_package_code (package_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户套餐表';

CREATE TABLE sys_tenant (
  tenant_id BIGINT PRIMARY KEY COMMENT '租户ID',
  tenant_name VARCHAR(200) NOT NULL COMMENT '物业公司/租户名称',
  tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
  unified_social_credit_code VARCHAR(64) NULL COMMENT '统一社会信用代码',
  contact_name VARCHAR(50) NULL COMMENT '联系人',
  contact_mobile VARCHAR(30) NULL COMMENT '联系电话',
  package_id BIGINT NULL COMMENT '套餐ID',
  service_start_date DATE NULL COMMENT '服务开始日期',
  service_end_date DATE NULL COMMENT '服务结束日期',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/TRIAL/ACTIVE/ARREARS_LIMITED/SUSPENDED/ARCHIVED',
  remark VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_code (tenant_code),
  KEY idx_tenant_status (status),
  KEY idx_tenant_package (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SaaS租户表';

CREATE TABLE sys_tenant_config (
  config_id BIGINT PRIMARY KEY COMMENT '配置ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  logo_url VARCHAR(500) NULL,
  service_phone VARCHAR(30) NULL,
  domain VARCHAR(200) NULL COMMENT '租户域名/子域名',
  wechat_appid VARCHAR(100) NULL,
  wechat_secret_encrypted VARCHAR(500) NULL,
  sms_channel_code VARCHAR(64) NULL,
  storage_policy VARCHAR(32) NOT NULL DEFAULT 'SHARED_BUCKET',
  config_json JSON NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_config (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户配置表';

CREATE TABLE tenant_pay_config (
  pay_config_id BIGINT PRIMARY KEY COMMENT '支付配置ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  project_id BIGINT NULL COMMENT '为空表示租户默认支付配置',
  pay_channel VARCHAR(32) NOT NULL DEFAULT 'WECHAT' COMMENT 'WECHAT/ALI/OFFLINE',
  merchant_mode VARCHAR(32) NOT NULL COMMENT 'SERVICE_PROVIDER/NORMAL_MCH',
  mch_id VARCHAR(100) NOT NULL COMMENT '普通商户号或服务商商户号',
  sub_mch_id VARCHAR(100) NULL COMMENT '服务商模式下的子商户号',
  app_id VARCHAR(100) NOT NULL COMMENT '小程序或公众号AppID',
  api_v3_key_encrypted VARCHAR(500) NULL COMMENT '微信支付API v3密钥密文或密钥引用',
  cert_serial_no VARCHAR(128) NULL COMMENT '商户证书序列号',
  cert_object_key VARCHAR(500) NULL COMMENT '证书对象存储路径或密钥引用',
  settlement_mode VARCHAR(32) NOT NULL DEFAULT 'TENANT_DIRECT' COMMENT 'TENANT_DIRECT/SERVICE_PROVIDER_SETTLE',
  notify_url VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_pay_channel (tenant_id, project_id, pay_channel),
  KEY idx_pay_config_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户支付配置表';

CREATE TABLE sys_tenant_usage (
  usage_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  stat_date DATE NOT NULL,
  project_count INT NOT NULL DEFAULT 0,
  user_count INT NOT NULL DEFAULT 0,
  member_count INT NOT NULL DEFAULT 0,
  house_count INT NOT NULL DEFAULT 0,
  storage_mb DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  sms_count INT NOT NULL DEFAULT 0,
  api_call_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_stat_date (tenant_id, stat_date),
  KEY idx_usage_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户用量统计表';

-- =========================
-- 系统用户、权限、字典
-- =========================

CREATE TABLE sys_dept (
  dept_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '平台部门为空或0，租户部门必填',
  parent_id BIGINT NOT NULL DEFAULT 0,
  dept_name VARCHAR(100) NOT NULL,
  dept_type VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT 'PLATFORM/TENANT/PROJECT',
  project_id BIGINT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_dept_tenant (tenant_id),
  KEY idx_dept_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE sys_user (
  user_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '平台用户为空或0，租户用户必填',
  dept_id BIGINT NULL,
  username VARCHAR(80) NOT NULL,
  real_name VARCHAR(80) NOT NULL,
  mobile VARCHAR(30) NULL,
  password_hash VARCHAR(255) NOT NULL,
  user_type VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT 'PLATFORM/TENANT',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  KEY idx_user_tenant (tenant_id),
  KEY idx_user_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台用户表';

CREATE TABLE sys_role (
  role_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL,
  role_name VARCHAR(100) NOT NULL,
  role_code VARCHAR(80) NOT NULL,
  role_level VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT 'PLATFORM/TENANT/PROJECT',
  data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT 'ALL_TENANT/PROJECT/DEPT/SELF/CUSTOM',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_role_code (tenant_id, role_code),
  KEY idx_role_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE sys_menu (
  menu_id BIGINT PRIMARY KEY,
  parent_id BIGINT NOT NULL DEFAULT 0,
  menu_name VARCHAR(100) NOT NULL,
  menu_type VARCHAR(32) NOT NULL COMMENT 'MENU/BUTTON/API',
  permission_code VARCHAR(120) NULL,
  route_path VARCHAR(200) NULL,
  api_path VARCHAR(200) NULL,
  component VARCHAR(200) NULL,
  module_code VARCHAR(64) NULL COMMENT '套餐模块编码',
  sort_no INT NOT NULL DEFAULT 0,
  visible TINYINT NOT NULL DEFAULT 1,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/按钮/API权限表';

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_ur_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

CREATE TABLE sys_role_menu (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  KEY idx_rm_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关系表';

CREATE TABLE sys_user_project (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_project (tenant_id, user_id, project_id),
  KEY idx_user_project_tenant (tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户授权项目关系表';

CREATE TABLE sys_dict_type (
  dict_type_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '为空表示平台全局字典',
  dict_code VARCHAR(80) NOT NULL,
  dict_name VARCHAR(100) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_dict_code (tenant_id, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

CREATE TABLE sys_dict_data (
  dict_data_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL,
  dict_code VARCHAR(80) NOT NULL,
  item_label VARCHAR(100) NOT NULL,
  item_value VARCHAR(100) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_dict_data (tenant_id, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- =========================
-- 基础档案
-- =========================

CREATE TABLE base_project (
  project_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_code VARCHAR(64) NOT NULL,
  project_name VARCHAR(200) NOT NULL,
  project_type VARCHAR(32) NOT NULL DEFAULT 'RESIDENTIAL',
  province VARCHAR(50) NULL,
  city VARCHAR(50) NULL,
  district VARCHAR(50) NULL,
  address VARCHAR(300) NULL,
  manager_user_id BIGINT NULL,
  service_phone VARCHAR(30) NULL,
  collection_subject VARCHAR(200) NULL COMMENT '收款主体',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_project_code (tenant_id, project_code),
  KEY idx_project_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目/小区表';

CREATE TABLE base_building (
  building_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  building_name VARCHAR(100) NOT NULL,
  building_code VARCHAR(64) NULL,
  building_type VARCHAR(32) NOT NULL DEFAULT 'BUILDING',
  floor_count INT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_building (tenant_id, project_id, building_name),
  KEY idx_building_project (tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼栋/区域表';

CREATE TABLE base_unit (
  unit_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  building_id BIGINT NOT NULL,
  unit_name VARCHAR(100) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_unit (tenant_id, building_id, unit_name),
  KEY idx_unit_project (tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单元表';

CREATE TABLE base_house (
  house_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  building_id BIGINT NOT NULL,
  unit_id BIGINT NULL,
  house_no VARCHAR(80) NOT NULL,
  floor_no INT NULL,
  building_area DECIMAL(12,2) NULL,
  inner_area DECIMAL(12,2) NULL,
  house_usage VARCHAR(32) NOT NULL DEFAULT 'RESIDENTIAL',
  house_status VARCHAR(32) NOT NULL DEFAULT 'VACANT' COMMENT 'VACANT/OCCUPIED/RENTED/LOCKED/RENOVATING',
  charge_object VARCHAR(32) NOT NULL DEFAULT 'HOUSE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_house_no (tenant_id, project_id, building_id, unit_id, house_no),
  KEY idx_house_project (tenant_id, project_id),
  KEY idx_house_status (tenant_id, project_id, house_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋表';

CREATE TABLE member_user (
  member_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '会员所属租户；同一微信用户跨租户分别建会员档案',
  openid VARCHAR(100) NULL,
  unionid VARCHAR(100) NULL,
  mobile VARCHAR(30) NULL,
  real_name VARCHAR(80) NULL,
  avatar_url VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_openid (tenant_id, openid),
  KEY idx_member_mobile (tenant_id, mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信端会员用户表';

CREATE TABLE member_house_bind (
  bind_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  house_id BIGINT NOT NULL,
  bind_role VARCHAR(32) NOT NULL COMMENT 'OWNER/FAMILY/TENANT/RESIDENT',
  real_name VARCHAR(80) NOT NULL,
  mobile VARCHAR(30) NOT NULL,
  id_card_no_encrypted VARCHAR(500) NULL,
  proof_file_ids VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/UNBOUND/EXPIRED',
  effective_date DATE NULL,
  expire_date DATE NULL,
  audit_user_id BIGINT NULL,
  audit_at DATETIME NULL,
  audit_remark VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_bind_member (member_id),
  KEY idx_bind_house (tenant_id, project_id, house_id),
  KEY idx_bind_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员房屋绑定表';

CREATE TABLE base_parking_space (
  space_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  space_no VARCHAR(80) NOT NULL,
  space_type VARCHAR(32) NOT NULL DEFAULT 'UNDERGROUND',
  status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  house_id BIGINT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_space_no (tenant_id, project_id, space_no),
  KEY idx_space_project (tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车位表';

CREATE TABLE base_vehicle (
  vehicle_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  plate_no VARCHAR(20) NOT NULL,
  vehicle_type VARCHAR(32) NOT NULL DEFAULT 'CAR',
  member_id BIGINT NULL,
  house_id BIGINT NULL,
  space_id BIGINT NULL,
  monthly_rent_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
  start_date DATE NULL,
  end_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vehicle_plate (tenant_id, project_id, plate_no),
  KEY idx_vehicle_house (tenant_id, house_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆表';

-- =========================
-- 收费与支付
-- =========================

CREATE TABLE fee_item (
  item_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(100) NOT NULL,
  item_type VARCHAR(32) NOT NULL COMMENT 'PERIODIC/ONCE/DEPOSIT/PREPAID/AGENCY',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_fee_item_code (tenant_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收费项目表';

CREATE TABLE fee_standard (
  standard_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  item_id BIGINT NOT NULL,
  standard_name VARCHAR(100) NOT NULL,
  charge_method VARCHAR(32) NOT NULL COMMENT 'AREA/HOUSE/VEHICLE/SPACE/CONTRACT/FIXED/FORMULA',
  unit_price DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
  cycle VARCHAR(32) NOT NULL DEFAULT 'MONTH',
  formula TEXT NULL,
  effective_date DATE NOT NULL,
  expire_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_standard_item (tenant_id, item_id),
  KEY idx_standard_project (tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收费标准表';

CREATE TABLE fee_standard_bind (
  bind_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  standard_id BIGINT NOT NULL,
  object_type VARCHAR(32) NOT NULL COMMENT 'HOUSE/VEHICLE/SPACE/CONTRACT/RESOURCE',
  object_id BIGINT NOT NULL,
  effective_date DATE NOT NULL,
  expire_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_bind_object (tenant_id, project_id, object_type, object_id),
  KEY idx_bind_standard (tenant_id, standard_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收费标准绑定表';

CREATE TABLE fee_bill (
  bill_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  bill_no VARCHAR(64) NOT NULL,
  item_id BIGINT NOT NULL,
  standard_id BIGINT NULL,
  object_type VARCHAR(32) NOT NULL,
  object_id BIGINT NOT NULL,
  member_id BIGINT NULL,
  house_id BIGINT NULL,
  bill_period VARCHAR(32) NOT NULL COMMENT '账期，如2026-06',
  receivable_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remaining_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  due_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
  source_type VARCHAR(32) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED/IMPORT/MANUAL/CONTRACT',
  void_reason VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_bill_no (tenant_id, bill_no),
  KEY idx_bill_object (tenant_id, project_id, object_type, object_id),
  KEY idx_bill_house (tenant_id, project_id, house_id),
  KEY idx_bill_status (tenant_id, project_id, status),
  KEY idx_bill_period (tenant_id, project_id, bill_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单表';

CREATE TABLE pay_order (
  order_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  member_id BIGINT NULL,
  pay_channel VARCHAR(32) NOT NULL COMMENT 'WECHAT/ALI/OFFLINE/POS/CASH',
  amount DECIMAL(12,2) NOT NULL,
  subject VARCHAR(200) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  expire_at DATETIME NULL,
  paid_at DATETIME NULL,
  third_trade_no VARCHAR(128) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no (tenant_id, order_no),
  KEY idx_order_member (tenant_id, member_id),
  KEY idx_order_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

CREATE TABLE pay_order_bill (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  bill_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_bill (tenant_id, order_id, bill_id),
  KEY idx_ob_bill (tenant_id, bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单账单核销关系表';

CREATE TABLE pay_transaction (
  transaction_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  third_trade_no VARCHAR(128) NOT NULL,
  pay_channel VARCHAR(32) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  paid_at DATETIME NOT NULL,
  raw_notify JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_third_trade (tenant_id, pay_channel, third_trade_no),
  KEY idx_tx_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

CREATE TABLE pay_refund (
  refund_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  refund_no VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  transaction_id BIGINT NULL,
  refund_amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'APPLYING',
  third_refund_no VARCHAR(128) NULL,
  refunded_at DATETIME NULL,
  apply_user_id BIGINT NULL,
  audit_user_id BIGINT NULL,
  audit_at DATETIME NULL,
  raw_notify JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_refund_no (tenant_id, refund_no),
  KEY idx_refund_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款表';

CREATE TABLE pay_refund_transaction (
  refund_tx_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  refund_id BIGINT NOT NULL,
  refund_no VARCHAR(64) NOT NULL,
  third_refund_no VARCHAR(128) NOT NULL,
  pay_channel VARCHAR(32) NOT NULL,
  refund_amount DECIMAL(12,2) NOT NULL,
  refunded_at DATETIME NOT NULL,
  raw_notify JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_refund_third (tenant_id, pay_channel, third_refund_no),
  KEY idx_refund_tx_order (tenant_id, refund_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款流水表';

CREATE TABLE fee_receipt (
  receipt_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  receipt_no VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  member_id BIGINT NULL,
  amount DECIMAL(12,2) NOT NULL,
  receipt_url VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_receipt_no (tenant_id, receipt_no),
  KEY idx_receipt_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子收据表';

-- =========================
-- 物业服务
-- =========================

CREATE TABLE notice (
  notice_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  notice_type VARCHAR(32) NOT NULL DEFAULT 'PROPERTY',
  target_scope VARCHAR(32) NOT NULL DEFAULT 'PROJECT',
  publish_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  published_at DATETIME NULL,
  publisher_id BIGINT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_notice_project (tenant_id, project_id),
  KEY idx_notice_status (tenant_id, publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告通知表';

CREATE TABLE work_order (
  work_order_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  member_id BIGINT NULL,
  house_id BIGINT NULL,
  order_type VARCHAR(32) NOT NULL COMMENT 'REPAIR/COMPLAINT/SUGGESTION/HOUSEKEEPING/RENOVATION',
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  location VARCHAR(300) NULL,
  image_file_ids VARCHAR(1000) NULL,
  priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  accept_user_id BIGINT NULL,
  dispatch_user_id BIGINT NULL,
  handler_user_id BIGINT NULL,
  sla_deadline DATETIME NULL,
  completed_at DATETIME NULL,
  evaluated_at DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_work_order_no (tenant_id, order_no),
  KEY idx_wo_status (tenant_id, project_id, status),
  KEY idx_wo_handler (tenant_id, handler_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

CREATE TABLE work_order_event (
  event_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  work_order_id BIGINT NOT NULL,
  from_status VARCHAR(32) NULL,
  to_status VARCHAR(32) NULL,
  action VARCHAR(64) NOT NULL,
  operator_type VARCHAR(32) NOT NULL COMMENT 'USER/MEMBER/SYSTEM',
  operator_id BIGINT NULL,
  content TEXT NULL,
  image_file_ids VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wo_event (tenant_id, work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单处理记录表';

CREATE TABLE work_order_comment (
  comment_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  work_order_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  score INT NOT NULL,
  content VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wo_comment (tenant_id, work_order_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单评价表';

-- =========================
-- 招商租赁与合同
-- =========================

CREATE TABLE lease_resource (
  resource_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL COMMENT 'HOUSE/SHOP/SPACE/AD/WAREHOUSE',
  resource_name VARCHAR(200) NOT NULL,
  ref_object_id BIGINT NULL,
  area DECIMAL(12,2) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'VACANT',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_lease_resource (tenant_id, project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可租资源表';

CREATE TABLE lease_customer (
  customer_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  customer_name VARCHAR(100) NOT NULL,
  contact_mobile VARCHAR(30) NULL,
  source_channel VARCHAR(64) NULL,
  demand_area DECIMAL(12,2) NULL,
  budget_amount DECIMAL(12,2) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'POTENTIAL',
  owner_user_id BIGINT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_lease_customer (tenant_id, project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁意向客户表';

CREATE TABLE lease_follow_record (
  follow_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  follow_type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  next_follow_at DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_follow_customer (tenant_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁客户跟进表';

CREATE TABLE lease_contract (
  contract_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  contract_no VARCHAR(64) NOT NULL,
  customer_id BIGINT NULL,
  resource_id BIGINT NOT NULL,
  lessee_name VARCHAR(100) NOT NULL,
  lessee_mobile VARCHAR(30) NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  rent_amount DECIMAL(12,2) NOT NULL,
  deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  payment_cycle VARCHAR(32) NOT NULL DEFAULT 'MONTH',
  free_rent_days INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  attachment_file_ids VARCHAR(1000) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_contract_no (tenant_id, contract_no),
  KEY idx_contract_project (tenant_id, project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁合同表';

-- =========================
-- 资产巡检
-- =========================

CREATE TABLE asset_equipment (
  equipment_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  equipment_code VARCHAR(64) NOT NULL,
  equipment_name VARCHAR(100) NOT NULL,
  equipment_type VARCHAR(64) NOT NULL,
  location VARCHAR(300) NULL,
  responsible_user_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_equipment_code (tenant_id, project_id, equipment_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备资产表';

CREATE TABLE patrol_point (
  point_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  point_code VARCHAR(64) NOT NULL,
  point_name VARCHAR(100) NOT NULL,
  point_type VARCHAR(32) NOT NULL,
  equipment_id BIGINT NULL,
  location VARCHAR(300) NULL,
  qr_code VARCHAR(200) NULL,
  nfc_code VARCHAR(200) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_patrol_point (tenant_id, project_id, point_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检点位表';

CREATE TABLE patrol_plan (
  plan_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  plan_name VARCHAR(100) NOT NULL,
  cycle_type VARCHAR(32) NOT NULL COMMENT 'DAY/WEEK/MONTH/CUSTOM',
  executor_user_id BIGINT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_patrol_plan (tenant_id, project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检计划表';

CREATE TABLE patrol_task (
  task_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  plan_id BIGINT NULL,
  task_no VARCHAR(64) NOT NULL,
  task_name VARCHAR(100) NOT NULL,
  executor_user_id BIGINT NULL,
  planned_start_at DATETIME NOT NULL,
  planned_end_at DATETIME NOT NULL,
  actual_start_at DATETIME NULL,
  actual_end_at DATETIME NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_patrol_task_no (tenant_id, task_no),
  KEY idx_patrol_task (tenant_id, project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务表';

CREATE TABLE patrol_task_item (
  item_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  point_id BIGINT NOT NULL,
  result VARCHAR(32) NULL COMMENT 'NORMAL/EXCEPTION/MISSED',
  content TEXT NULL,
  image_file_ids VARCHAR(1000) NULL,
  checked_at DATETIME NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_task_item (tenant_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务明细表';

-- =========================
-- 设备、门禁、访客、停车
-- =========================

CREATE TABLE device_config (
  device_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  device_type VARCHAR(32) NOT NULL COMMENT 'ACCESS/ELEVATOR/PARKING/CAMERA',
  vendor_code VARCHAR(64) NOT NULL,
  device_code VARCHAR(100) NOT NULL,
  device_name VARCHAR(100) NOT NULL,
  location VARCHAR(300) NULL,
  config_json JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_device_code (tenant_id, project_id, device_type, device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备配置表';

CREATE TABLE access_permission (
  permission_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  member_id BIGINT NULL,
  user_id BIGINT NULL,
  visitor_id BIGINT NULL,
  device_id BIGINT NOT NULL,
  permission_type VARCHAR(32) NOT NULL COMMENT 'FACE/CARD/QRCODE/REMOTE',
  start_at DATETIME NOT NULL,
  end_at DATETIME NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_access_permission (tenant_id, project_id, device_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门禁权限表';

CREATE TABLE access_record (
  record_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  device_id BIGINT NULL,
  member_id BIGINT NULL,
  visitor_id BIGINT NULL,
  open_type VARCHAR(32) NOT NULL,
  open_result VARCHAR(32) NOT NULL,
  occurred_at DATETIME NOT NULL,
  raw_data JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_access_record (tenant_id, project_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门禁通行记录表';

CREATE TABLE visitor_record (
  visitor_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  inviter_member_id BIGINT NULL,
  visitor_name VARCHAR(80) NOT NULL,
  visitor_mobile VARCHAR(30) NULL,
  visit_reason VARCHAR(200) NULL,
  valid_start_at DATETIME NOT NULL,
  valid_end_at DATETIME NOT NULL,
  qr_code VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_visitor_project (tenant_id, project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客记录表';

CREATE TABLE parking_sync_record (
  sync_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  vendor_code VARCHAR(64) NOT NULL,
  plate_no VARCHAR(20) NULL,
  sync_type VARCHAR(32) NOT NULL,
  request_data JSON NULL,
  response_data JSON NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  error_message VARCHAR(1000) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_parking_sync (tenant_id, project_id, sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='停车系统同步记录表';

-- =========================
-- 数据导入
-- =========================

CREATE TABLE import_batch (
  batch_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  import_type VARCHAR(64) NOT NULL COMMENT 'HOUSE/MEMBER/VEHICLE/FEE_STANDARD/HISTORY_BILL等',
  batch_no VARCHAR(64) NOT NULL,
  source_file_id BIGINT NULL,
  total_count INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  fail_count INT NOT NULL DEFAULT 0,
  import_status VARCHAR(32) NOT NULL DEFAULT 'VALIDATING' COMMENT 'VALIDATING/VALID_FAILED/IMPORTING/SUCCESS/PARTIAL_SUCCESS/FAILED/VOIDED',
  error_report_file_id BIGINT NULL,
  can_rollback TINYINT NOT NULL DEFAULT 0,
  rollback_status VARCHAR(32) NULL COMMENT 'NONE/ROLLING_BACK/ROLLED_BACK/FAILED',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_import_batch_no (tenant_id, batch_no),
  KEY idx_import_batch (tenant_id, project_id, import_type, import_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导入批次表';

CREATE TABLE import_error_detail (
  error_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  batch_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  field_name VARCHAR(100) NULL,
  raw_value VARCHAR(1000) NULL,
  error_code VARCHAR(80) NULL,
  error_message VARCHAR(1000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_import_error_batch (tenant_id, batch_id, row_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导入错误明细表';

-- =========================
-- 公共能力
-- =========================

CREATE TABLE file_object (
  file_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '平台公共文件为空；租户业务文件必填',
  project_id BIGINT NULL COMMENT '项目级文件必填；租户级公共文件可为空',
  module_code VARCHAR(64) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  object_key VARCHAR(500) NOT NULL,
  file_ext VARCHAR(20) NULL,
  content_type VARCHAR(100) NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  is_sensitive TINYINT NOT NULL DEFAULT 0,
  uploader_type VARCHAR(32) NOT NULL,
  uploader_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_file_tenant (tenant_id, project_id, module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件对象表';

CREATE TABLE message_template (
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

CREATE TABLE message_record (
  message_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '平台消息为空；租户消息必填',
  project_id BIGINT NULL COMMENT '项目消息必填；租户级消息可为空',
  receiver_type VARCHAR(32) NOT NULL COMMENT 'USER/MEMBER/MOBILE',
  receiver_id BIGINT NULL,
  receiver_mobile VARCHAR(30) NULL,
  channel VARCHAR(32) NOT NULL COMMENT 'SMS/WECHAT/SITE',
  template_code VARCHAR(80) NULL,
  title VARCHAR(200) NULL,
  content TEXT NOT NULL,
  send_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  fail_reason VARCHAR(1000) NULL,
  sent_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_message_tenant (tenant_id, project_id, send_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录表';

CREATE TABLE interface_call_log (
  log_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL,
  project_id BIGINT NULL,
  interface_type VARCHAR(64) NOT NULL,
  vendor_code VARCHAR(64) NULL,
  request_id VARCHAR(100) NULL,
  request_url VARCHAR(500) NULL,
  request_body TEXT NULL,
  response_body TEXT NULL,
  success TINYINT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME NULL,
  cost_ms INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_interface_log (tenant_id, interface_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方接口调用日志表';

CREATE TABLE operation_log (
  log_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL,
  project_id BIGINT NULL,
  operator_type VARCHAR(32) NOT NULL COMMENT 'PLATFORM_USER/TENANT_USER/MEMBER/SYSTEM',
  operator_id BIGINT NULL,
  module_code VARCHAR(64) NOT NULL,
  action_code VARCHAR(80) NOT NULL,
  object_type VARCHAR(64) NULL,
  object_id BIGINT NULL,
  before_data JSON NULL,
  after_data JSON NULL,
  reason VARCHAR(500) NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_tenant (tenant_id, project_id, created_at),
  KEY idx_operation_object (tenant_id, object_type, object_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
