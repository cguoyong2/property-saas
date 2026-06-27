SET @sql := (
    SELECT IF(
        NOT EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'base_vehicle'
              AND index_name = 'uk_vehicle_space'
        ),
        'ALTER TABLE base_vehicle ADD UNIQUE KEY uk_vehicle_space (tenant_id, space_id)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
