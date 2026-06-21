ALTER TABLE base_vehicle
    ADD UNIQUE KEY uk_vehicle_space (tenant_id, space_id);
