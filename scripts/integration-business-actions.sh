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

skip() {
  printf '[SKIP] %s\n' "$1"
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

call_ok() {
  local name="$1"
  local method="$2"
  local path="$3"
  local body="${4:-}"
  local output="${tmp_dir}/${name// /-}.json"
  local status
  status="$(request "$method" "$path" "$body" "$tenant_token" "$output")"
  assert_status "$name" 200 "$status" "$output"
  assert_json_code "${name}响应" 0 "$output"
}

login_body="${tmp_dir}/login.json"
login_status="$(request POST /api/auth/login '{"username":"tenant_a_admin","password":"Admin@123"}' "" "$login_body")"
assert_status "后台账号登录" 200 "$login_status" "$login_body"
assert_json_code "后台账号登录响应" 0 "$login_body"
tenant_token="$(json_value "$login_body" 'data => data.data.token')"

create_workorder_body="${tmp_dir}/create-workorder.json"
create_workorder_status="$(request POST /api/service/workorders '{"projectId":101,"memberId":10001,"houseId":1000001,"orderType":"REPAIR","title":"业务动作验收工单","description":"TASK-048 自动化验收创建","location":"1栋1单元101","priority":"NORMAL"}' "$tenant_token" "$create_workorder_body")"
assert_status "创建验收工单" 200 "$create_workorder_status" "$create_workorder_body"
assert_json_code "创建验收工单响应" 0 "$create_workorder_body"
work_order_id="$(json_value "$create_workorder_body" 'data => data.data.workOrderId')"

call_ok "工单受理" PUT "/api/service/workorders/${work_order_id}/accept" '{"content":"自动化受理"}'
call_ok "工单派单" PUT "/api/service/workorders/${work_order_id}/dispatch" '{"handlerUserId":1001,"content":"自动化派单"}'
call_ok "工单开始处理" PUT "/api/service/workorders/${work_order_id}/start" '{"content":"自动化开始处理"}'
call_ok "工单挂起" PUT "/api/service/workorders/${work_order_id}/hang-up" '{"content":"自动化挂起"}'
call_ok "工单恢复" PUT "/api/service/workorders/${work_order_id}/resume" '{"content":"自动化恢复"}'
call_ok "工单提交结果" PUT "/api/service/workorders/${work_order_id}/submit-result" '{"content":"自动化完工"}'
call_ok "工单SLA扫描" POST "/api/service/workorders/sla/mark-overdue?limit=10" ''

call_ok "站内消息派发" POST "/api/service/messages/dispatch-pending?limit=10" ''
call_ok "失败消息重试" POST "/api/service/messages/retry-failed?limit=10" ''
call_ok "租赁到期提醒" POST "/api/lease/contracts/expire-remind?days=30" ''
call_ok "门禁权限同步" POST "/api/device/access/sync" '{"projectId":101,"limit":5}'

batches_body="${tmp_dir}/import-batches.json"
batches_status="$(request GET '/api/import/batches?pageNo=1&pageSize=1' "" "$tenant_token" "$batches_body")"
assert_status "导入批次查询" 200 "$batches_status" "$batches_body"
assert_json_code "导入批次查询响应" 0 "$batches_body"
batch_id="$(json_value "$batches_body" 'data => data.data.records && data.data.records[0] && data.data.records[0].batchId')"
if [[ -n "$batch_id" ]]; then
  csv_body="${tmp_dir}/import-errors.csv"
  csv_status="$(request GET "/api/import/batches/${batch_id}/errors.csv" "" "$tenant_token" "$csv_body")"
  assert_status "导入错误CSV下载" 200 "$csv_status" "$csv_body"
else
  skip "当前环境没有导入批次，跳过错误CSV下载"
fi

printf 'Business action integration checks passed against %s\n' "$base_url"
