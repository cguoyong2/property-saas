ALTER TABLE member_user
    DROP INDEX idx_member_mobile,
    ADD UNIQUE KEY uk_member_mobile (tenant_id, mobile);

ALTER TABLE base_vehicle
    DROP INDEX uk_vehicle_plate,
    ADD UNIQUE KEY uk_vehicle_plate (tenant_id, plate_no);
