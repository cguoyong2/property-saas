ALTER TABLE member_prepayment
  ADD COLUMN refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER remaining_amount;
