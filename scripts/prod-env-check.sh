#!/usr/bin/env bash
set -euo pipefail

required_vars=(
  SPRING_PROFILES_ACTIVE
  SERVER_PORT
  DB_URL
  DB_USERNAME
  DB_PASSWORD
  REDIS_HOST
  REDIS_PORT
  REDIS_PASSWORD
  PROPERTY_SAAS_JWT_SECRET
  PROPERTY_SAAS_JWT_TTL_SECONDS
  PROPERTY_SAAS_FILE_LOCAL_ROOT_DIR
  PROPERTY_SAAS_FILE_MAX_SIZE_BYTES
  PROPERTY_SAAS_JOB_ENABLED
  WECHAT_PAY_MODE
  WECHAT_PAY_CALLBACK_TOLERANCE_SECONDS
  WECHAT_PAY_MERCHANT_PRIVATE_KEY_PATH
  WECHAT_PAY_MERCHANT_CERTIFICATE_PATH
  WECHAT_PAY_PLATFORM_CERTIFICATE_PATH
  WECHAT_PAY_API_V3_KEY_REF
)

failed=0

for name in "${required_vars[@]}"; do
  value="${!name:-}"
  if [[ -z "$value" ]]; then
    printf '[FAIL] %s is required\n' "$name" >&2
    failed=1
    continue
  fi
  case "$value" in
    *change_me*|*change-me*|*CHANGE_ME*|*placeholder*|*example.com*|property|root|root123456|minioadmin|Admin@123|password|jwt-secret)
      printf '[FAIL] %s contains an unsafe default or placeholder\n' "$name" >&2
      failed=1
      ;;
    *)
      printf '[PASS] %s is set\n' "$name"
      ;;
  esac
done

if [[ "${SPRING_PROFILES_ACTIVE:-}" != "prod" ]]; then
  printf '[FAIL] SPRING_PROFILES_ACTIVE must be prod\n' >&2
  failed=1
fi

if ! [[ "${SERVER_PORT:-}" =~ ^[0-9]+$ ]] || (( SERVER_PORT < 1 || SERVER_PORT > 65535 )); then
  printf '[FAIL] SERVER_PORT must be a valid TCP port\n' >&2
  failed=1
fi

if ! [[ "${REDIS_PORT:-}" =~ ^[0-9]+$ ]] || (( REDIS_PORT < 1 || REDIS_PORT > 65535 )); then
  printf '[FAIL] REDIS_PORT must be a valid TCP port\n' >&2
  failed=1
fi

if [[ "${DB_URL:-}" != jdbc:mysql:* ]]; then
  printf '[FAIL] DB_URL must be a MySQL JDBC URL\n' >&2
  failed=1
fi

if [[ "${DB_URL:-}" != *useSSL=true* ]]; then
  printf '[FAIL] DB_URL must enable useSSL=true in production\n' >&2
  failed=1
fi

jwt_secret="${PROPERTY_SAAS_JWT_SECRET:-}"
if [[ "${#jwt_secret}" -lt 32 ]]; then
  printf '[FAIL] PROPERTY_SAAS_JWT_SECRET must be at least 32 characters\n' >&2
  failed=1
fi

if ! [[ "${PROPERTY_SAAS_JWT_TTL_SECONDS:-}" =~ ^[0-9]+$ ]] || (( PROPERTY_SAAS_JWT_TTL_SECONDS < 300 || PROPERTY_SAAS_JWT_TTL_SECONDS > 86400 )); then
  printf '[FAIL] PROPERTY_SAAS_JWT_TTL_SECONDS must be between 300 and 86400\n' >&2
  failed=1
fi

file_root="${PROPERTY_SAAS_FILE_LOCAL_ROOT_DIR:-}"
if [[ "$file_root" != /* ]]; then
  printf '[FAIL] PROPERTY_SAAS_FILE_LOCAL_ROOT_DIR must be an absolute path\n' >&2
  failed=1
fi

case "$file_root" in
  /tmp|/tmp/*|/var/tmp|/var/tmp/*|/Users/*|/home/*/Downloads/*)
    printf '[FAIL] PROPERTY_SAAS_FILE_LOCAL_ROOT_DIR must not point to temporary or personal directories\n' >&2
    failed=1
    ;;
esac

if ! [[ "${PROPERTY_SAAS_FILE_MAX_SIZE_BYTES:-}" =~ ^[0-9]+$ ]] || (( PROPERTY_SAAS_FILE_MAX_SIZE_BYTES < 1048576 || PROPERTY_SAAS_FILE_MAX_SIZE_BYTES > 104857600 )); then
  printf '[FAIL] PROPERTY_SAAS_FILE_MAX_SIZE_BYTES must be between 1MiB and 100MiB\n' >&2
  failed=1
fi

if [[ "${PROPERTY_SAAS_JOB_ENABLED:-}" != "true" && "${PROPERTY_SAAS_JOB_ENABLED:-}" != "false" ]]; then
  printf '[FAIL] PROPERTY_SAAS_JOB_ENABLED must be true or false\n' >&2
  failed=1
fi

if [[ "${WECHAT_PAY_MODE:-}" != "REAL_WECHAT_V3" ]]; then
  printf '[FAIL] WECHAT_PAY_MODE must be REAL_WECHAT_V3 in production\n' >&2
  failed=1
fi

if ! [[ "${WECHAT_PAY_CALLBACK_TOLERANCE_SECONDS:-}" =~ ^[0-9]+$ ]] || (( WECHAT_PAY_CALLBACK_TOLERANCE_SECONDS < 60 || WECHAT_PAY_CALLBACK_TOLERANCE_SECONDS > 600 )); then
  printf '[FAIL] WECHAT_PAY_CALLBACK_TOLERANCE_SECONDS must be between 60 and 600\n' >&2
  failed=1
fi

for path_var in WECHAT_PAY_MERCHANT_PRIVATE_KEY_PATH WECHAT_PAY_MERCHANT_CERTIFICATE_PATH WECHAT_PAY_PLATFORM_CERTIFICATE_PATH; do
  path_value="${!path_var:-}"
  if [[ "$path_value" != /* && "$path_value" != kms:* && "$path_value" != s3:* && "$path_value" != oss:* && "$path_value" != cos:* ]]; then
    printf '[FAIL] %s must be an absolute path or a KMS/object-storage reference\n' "$path_var" >&2
    failed=1
  fi
done

if [[ "$failed" -ne 0 ]]; then
  exit 1
fi

printf 'Production environment check passed.\n'
