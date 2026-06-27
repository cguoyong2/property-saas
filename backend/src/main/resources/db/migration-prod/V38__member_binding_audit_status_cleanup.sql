UPDATE member_house_bind
SET status = 'PENDING'
WHERE deleted = 0
  AND (status IS NULL OR status = '');

ALTER TABLE member_house_bind
  MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '绑定状态：PENDING/APPROVED/REJECTED/UNBOUND';
