ALTER TABLE message_record
  ADD COLUMN read_status VARCHAR(32) NOT NULL DEFAULT 'UNREAD' AFTER send_status,
  ADD COLUMN read_at DATETIME NULL AFTER read_status,
  ADD KEY idx_message_receiver_read (tenant_id, receiver_type, receiver_id, channel, read_status, created_at);
