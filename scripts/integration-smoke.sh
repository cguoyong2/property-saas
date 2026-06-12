#!/usr/bin/env bash
set -euo pipefail

base_url="${API_BASE_URL:-http://localhost:8080}"
tmp_dir="$(mktemp -d)"

cleanup() {
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

pass() {
  printf '[PASS] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  if [[ -f "${2:-}" ]]; then
    printf 'Response body:\n' >&2
    cat "$2" >&2
    printf '\n' >&2
  fi
  exit 1
}

json_value() {
  local file="$1"
  local expr="$2"
  node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); const value=(${expr})(data); if (value !== undefined && value !== null) process.stdout.write(String(value));" "$file"
}

request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  local token="${4:-}"
  local output="$5"
  local args=(-sS -o "$output" -w '%{http_code}' -X "$method" "${base_url}${path}" -H 'Content-Type: application/json')
  if [[ -n "$token" ]]; then
    args+=(-H "Authorization: Bearer ${token}")
  fi
  if [[ -n "$body" ]]; then
    args+=(-d "$body")
  fi
  curl "${args[@]}"
}

assert_status() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  local body_file="$4"
  if [[ "$actual" != "$expected" ]]; then
    fail "${name}: expected HTTP ${expected}, got ${actual}" "$body_file"
  fi
  pass "${name}"
}

assert_status_in() {
  local name="$1"
  local actual="$2"
  local body_file="$3"
  shift 3
  for expected in "$@"; do
    if [[ "$actual" == "$expected" ]]; then
      pass "${name}"
      return
    fi
  done
  fail "${name}: unexpected HTTP ${actual}" "$body_file"
}

assert_json_code() {
  local name="$1"
  local expected="$2"
  local body_file="$3"
  local actual
  actual="$(json_value "$body_file" 'data => data.code')"
  if [[ "$actual" != "$expected" ]]; then
    fail "${name}: expected JSON code ${expected}, got ${actual}" "$body_file"
  fi
  pass "${name}"
}

login_backend() {
  local username="$1"
  local body_file="${tmp_dir}/login-${username}.json"
  local status
  status="$(request POST /api/auth/login "{\"username\":\"${username}\",\"password\":\"Admin@123\"}" "" "$body_file")"
  assert_status "后台账号登录 ${username}" 200 "$status" "$body_file" >&2
  assert_json_code "后台账号登录响应 ${username}" 0 "$body_file" >&2
  json_value "$body_file" 'data => data.data.token'
}

login_member() {
  local tenant_id="$1"
  local openid="$2"
  local mobile="$3"
  local real_name="$4"
  local body_file="${tmp_dir}/member-${tenant_id}.json"
  local status
  status="$(request POST /api/app/auth/wx-login "{\"tenantId\":${tenant_id},\"openid\":\"${openid}\",\"mobile\":\"${mobile}\",\"realName\":\"${real_name}\"}" "" "$body_file")"
  assert_status "小程序会员登录 tenant=${tenant_id}" 200 "$status" "$body_file" >&2
  assert_json_code "小程序会员登录响应 tenant=${tenant_id}" 0 "$body_file" >&2
  json_value "$body_file" 'data => data.data.token'
}

health_body="${tmp_dir}/health.json"
health_status="$(request GET /actuator/health "" "" "$health_body")"
assert_status "健康检查" 200 "$health_status" "$health_body"
assert_json_code "健康检查响应" 0 "$health_body"

unauth_body="${tmp_dir}/unauth-home.json"
unauth_status="$(request GET /api/app/home "" "" "$unauth_body")"
assert_status_in "未登录访问小程序业务接口被拒绝" "$unauth_status" "$unauth_body" 401 403

tenant_a_token="$(login_backend tenant_a_admin)"
platform_body="${tmp_dir}/tenant-platform-denied.json"
platform_status="$(request GET /api/platform/tenants "" "$tenant_a_token" "$platform_body")"
assert_status "租户账号访问平台接口被拒绝" 403 "$platform_status" "$platform_body"
assert_json_code "租户账号访问平台接口错误码" 403001 "$platform_body"

member_a_token="$(login_member 1 openid_tenant_a_10001 13810000001 王业主)"
member_b_token="$(login_member 2 openid_tenant_b_20001 13820000001 赵业主)"

houses_body="${tmp_dir}/app-houses-a.json"
houses_status="$(request GET /api/app/houses "" "$member_a_token" "$houses_body")"
assert_status "A租户会员查询本人房屋" 200 "$houses_status" "$houses_body"
assert_json_code "A租户会员查询本人房屋响应" 0 "$houses_body"

bills_body="${tmp_dir}/app-bills-a.json"
bills_status="$(request GET '/api/app/bills?houseId=1000001' "" "$member_a_token" "$bills_body")"
assert_status "A租户会员查询本人账单" 200 "$bills_status" "$bills_body"
assert_json_code "A租户会员查询本人账单响应" 0 "$bills_body"

cross_bill_body="${tmp_dir}/app-bills-cross.json"
cross_bill_status="$(request GET '/api/app/bills?houseId=1000001' "" "$member_b_token" "$cross_bill_body")"
assert_status "B租户会员访问A租户房屋账单被拒绝" 400 "$cross_bill_status" "$cross_bill_body"
assert_json_code "B租户会员访问A租户房屋账单错误码" 400002 "$cross_bill_body"

anonymous_bind_body="${tmp_dir}/anonymous-bind.json"
anonymous_bind_status="$(request POST /api/app/house-bindings '{"tenantId":1,"projectId":101,"memberId":10001,"houseId":1000001,"bindRole":"OWNER","realName":"王业主","mobile":"13810000001"}' "" "$anonymous_bind_body")"
assert_status_in "未登录提交房屋绑定被拒绝" "$anonymous_bind_status" "$anonymous_bind_body" 401 403

mismatch_bind_body="${tmp_dir}/mismatch-bind.json"
mismatch_bind_status="$(request POST /api/app/house-bindings '{"tenantId":1,"projectId":101,"memberId":20001,"houseId":1000001,"bindRole":"OWNER","realName":"赵业主","mobile":"13820000001"}' "$member_a_token" "$mismatch_bind_body")"
assert_status "会员代他人提交房屋绑定被拒绝" 403 "$mismatch_bind_status" "$mismatch_bind_body"
assert_json_code "会员代他人提交房屋绑定错误码" 403001 "$mismatch_bind_body"

bad_pay_body="${tmp_dir}/bad-pay-notify.json"
bad_pay_status="$(request POST /api/payment/wechat/notify '{"orderNo":"PAY-A-202606-0001","thirdTradeNo":"SMOKE-BAD-SIGN","amount":150.00,"paidAt":"2026-06-10T00:00:00","signature":"bad-signature","raw":{"source":"integration-smoke"}}' "" "$bad_pay_body")"
assert_status "微信支付错误签名回调被拒绝" 403 "$bad_pay_status" "$bad_pay_body"
assert_json_code "微信支付错误签名回调错误码" 403001 "$bad_pay_body"

printf 'Integration smoke checks passed against %s\n' "$base_url"
