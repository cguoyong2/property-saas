CREATE TABLE sys_login_log (
  login_log_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NULL,
  user_id BIGINT NULL,
  username VARCHAR(80) NOT NULL,
  user_type VARCHAR(32) NULL COMMENT 'PLATFORM/TENANT',
  login_result VARCHAR(32) NOT NULL COMMENT 'SUCCESS/FAILED',
  fail_reason VARCHAR(500) NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_login_log_user (tenant_id, user_id, created_at),
  KEY idx_login_log_username (username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台登录日志表';
