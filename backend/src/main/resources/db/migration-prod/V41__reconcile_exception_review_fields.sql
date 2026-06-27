ALTER TABLE payment_reconcile_exception_handle
  ADD COLUMN attachment_file_ids VARCHAR(500) NULL AFTER handle_remark,
  ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'NONE' AFTER handled_at,
  ADD COLUMN reviewed_by BIGINT NULL AFTER review_status,
  ADD COLUMN reviewed_at DATETIME NULL AFTER reviewed_by,
  ADD COLUMN review_remark VARCHAR(500) NULL AFTER reviewed_at;

UPDATE payment_reconcile_exception_handle
SET review_status = CASE WHEN status = 'HANDLED' THEN 'APPROVED' ELSE 'NONE' END
WHERE review_status = 'NONE';
