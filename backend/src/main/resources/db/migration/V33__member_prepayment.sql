CREATE TABLE IF NOT EXISTS member_prepayment (
  prepayment_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  member_id BIGINT NULL,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  remaining_amount DECIMAL(12,2) NOT NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'OFFLINE_OVERPAY',
  remark VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_prepayment_member (tenant_id, member_id, deleted),
  KEY idx_prepayment_project (tenant_id, project_id, deleted),
  KEY idx_prepayment_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业主/住户预存款表';
