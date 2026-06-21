ALTER TABLE base_vehicle
    ADD COLUMN vehicle_brand VARCHAR(80) NULL AFTER vehicle_type,
    ADD COLUMN vehicle_model VARCHAR(80) NULL AFTER vehicle_brand;
