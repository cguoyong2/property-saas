ALTER TABLE visitor_record
  ADD COLUMN house_id BIGINT NULL COMMENT '到访房屋ID' AFTER inviter_member_id,
  ADD KEY idx_visitor_house (tenant_id, house_id);
