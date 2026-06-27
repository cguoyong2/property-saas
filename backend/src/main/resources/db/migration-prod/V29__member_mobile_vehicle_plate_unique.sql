SET @sql := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'member_user'
              AND index_name = 'idx_member_mobile'
        ),
        'ALTER TABLE member_user DROP INDEX idx_member_mobile',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        NOT EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'member_user'
              AND index_name = 'uk_member_mobile'
        ),
        'ALTER TABLE member_user ADD UNIQUE KEY uk_member_mobile (tenant_id, mobile)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'base_vehicle'
              AND index_name = 'uk_vehicle_plate'
        ),
        'ALTER TABLE base_vehicle DROP INDEX uk_vehicle_plate',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        NOT EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'base_vehicle'
              AND index_name = 'uk_vehicle_plate'
        ),
        'ALTER TABLE base_vehicle ADD UNIQUE KEY uk_vehicle_plate (tenant_id, plate_no)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
