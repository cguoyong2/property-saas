CREATE TABLE IF NOT EXISTS member_prepayment_usage (
  usage_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  prepayment_id BIGINT NOT NULL,
  member_id BIGINT NULL,
  bill_id BIGINT NOT NULL,
  bill_no VARCHAR(64) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  usage_type VARCHAR(32) NOT NULL DEFAULT 'AUTO_BILL_OFFSET',
  remark VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_usage_bill (tenant_id, bill_id, deleted),
  KEY idx_usage_prepayment (tenant_id, prepayment_id, deleted),
  KEY idx_usage_member (tenant_id, member_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预存款抵扣明细表';
