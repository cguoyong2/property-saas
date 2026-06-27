CREATE TABLE payment_reconcile_exception_history (
  history_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  exception_key VARCHAR(160) NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  before_status VARCHAR(32) NULL,
  after_status VARCHAR(32) NULL,
  before_review_status VARCHAR(32) NULL,
  after_review_status VARCHAR(32) NULL,
  remark VARCHAR(500) NULL,
  attachment_file_ids VARCHAR(500) NULL,
  operator_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_reconcile_exception_history_key (tenant_id, exception_key, created_at),
  KEY idx_reconcile_exception_history_project (tenant_id, project_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付对账异常处理历史表';
