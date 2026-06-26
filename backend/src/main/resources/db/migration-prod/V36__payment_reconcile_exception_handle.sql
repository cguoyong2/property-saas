CREATE TABLE payment_reconcile_exception_handle (
  handle_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  exception_key VARCHAR(160) NOT NULL,
  exception_type VARCHAR(64) NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  business_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'HANDLED',
  handle_remark VARCHAR(500) NOT NULL,
  handled_by BIGINT NULL,
  handled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_reconcile_exception_key (tenant_id, exception_key, deleted),
  KEY idx_reconcile_exception_project (tenant_id, project_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付对账异常处理记录表';
