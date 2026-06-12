#!/usr/bin/env bash
set -euo pipefail

database="${MYSQL_DATABASE:-property_saas}"
user="${MYSQL_USER:-property}"
password="${MYSQL_PASSWORD:-property}"

docker compose exec -T mysql mysql -u"${user}" -p"${password}" "${database}" -e "
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = '${database}';

SELECT tenant_id, tenant_name, status
FROM sys_tenant
ORDER BY tenant_id;

SELECT user_id, tenant_id, username, user_type, status
FROM sys_user
ORDER BY user_id;

SHOW TABLES LIKE 'sys_login_log';
"
