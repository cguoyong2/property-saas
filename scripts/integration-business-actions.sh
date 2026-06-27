#!/usr/bin/env bash
set -euo pipefail

base_url="${API_BASE_URL:-http://localhost:8080}"
run_accounting_mutations="${RUN_MUTATING_ACCOUNTING_CHECKS:-false}"
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

assert_page_query_ok() {
  local name="$1"
  local path="$2"
  local output="${tmp_dir}/${name// /-}.json"
  local status
  status="$(request GET "$path" "" "$tenant_token" "$output")"
  assert_status "$name" 200 "$status" "$output"
  assert_json_code "${name}响应" 0 "$output"
}

first_record_value() {
  local file="$1"
  local expr="$2"
  json_value "$file" "data => { const rows = data.data && data.data.records || []; const row = rows[0] || {}; return (${expr})(row); }"
}

records_count() {
  local file="$1"
  json_value "$file" 'data => data.data && data.data.records ? data.data.records.length : 0'
}

urlencode() {
  node -e "process.stdout.write(encodeURIComponent(process.argv[1] || ''))" "$1"
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

members_body="${tmp_dir}/members-search.json"
member_keyword="$(urlencode '王')"
members_status="$(request GET "/api/base/members?keyword=${member_keyword}&pageNo=1&pageSize=10" "" "$tenant_token" "$members_body")"
assert_status "业主/住户自动检索" 200 "$members_status" "$members_body"
assert_json_code "业主/住户自动检索响应" 0 "$members_body"
member_count="$(records_count "$members_body")"
if [[ "$member_count" == "0" ]]; then
  skip "当前环境没有匹配“王”的业主/住户，跳过业主检索字段断言"
else
  member_project_name="$(first_record_value "$members_body" 'row => row.projectName')"
  member_house_no="$(first_record_value "$members_body" 'row => row.houseNo')"
  if [[ -z "$member_project_name" || -z "$member_house_no" ]]; then
    fail "业主/住户检索结果缺少小区或房号核对字段" "$members_body"
  fi
  pass "业主/住户检索结果包含小区和房号核对字段"
fi

assert_page_query_ok "房屋绑定审核列表" '/api/base/member-bindings?pageNo=1&pageSize=5'
assert_page_query_ok "车位区域列表" '/api/base/parking-areas?pageNo=1&pageSize=5'
assert_page_query_ok "车位列表" '/api/base/parking-spaces?pageNo=1&pageSize=5'
assert_page_query_ok "车辆列表" '/api/base/vehicles?pageNo=1&pageSize=5'
assert_page_query_ok "车辆品牌列表" '/api/base/vehicle-brands?pageNo=1&pageSize=5'
assert_page_query_ok "车辆型号列表" '/api/base/vehicle-models?pageNo=1&pageSize=5'

bills_body="${tmp_dir}/fee-bills.json"
bills_status="$(request GET '/api/fee/bills?projectId=101&pageNo=1&pageSize=10' "" "$tenant_token" "$bills_body")"
assert_status "账单自动计算结果查询" 200 "$bills_status" "$bills_body"
assert_json_code "账单自动计算结果查询响应" 0 "$bills_body"
bill_count="$(records_count "$bills_body")"
if [[ "$bill_count" == "0" ]]; then
  skip "当前环境没有小区 101 的账单，跳过账单明细字段断言"
else
  bill_id="$(first_record_value "$bills_body" 'row => row.billId')"
  bill_member_name="$(first_record_value "$bills_body" 'row => row.memberName')"
  bill_house_no="$(first_record_value "$bills_body" 'row => row.houseNo')"
  bill_detail="$(first_record_value "$bills_body" 'row => row.detailSummary')"
  if [[ -z "$bill_member_name" || -z "$bill_house_no" || -z "$bill_detail" ]]; then
    fail "账单列表缺少业主/住户、房号或应收明细字段" "$bills_body"
  fi
  pass "账单列表包含业主/住户、房号和应收明细字段"

  bill_detail_body="${tmp_dir}/fee-bill-detail.json"
  bill_detail_status="$(request GET "/api/fee/bills/${bill_id}" "" "$tenant_token" "$bill_detail_body")"
  assert_status "账单详情查询" 200 "$bill_detail_status" "$bill_detail_body"
  assert_json_code "账单详情查询响应" 0 "$bill_detail_body"
fi

orders_body="${tmp_dir}/payment-orders.json"
orders_status="$(request GET '/api/payment/orders?pageNo=1&pageSize=10' "" "$tenant_token" "$orders_body")"
assert_status "收款订单列表" 200 "$orders_status" "$orders_body"
assert_json_code "收款订单列表响应" 0 "$orders_body"
order_count="$(records_count "$orders_body")"
if [[ "$order_count" == "0" ]]; then
  skip "当前环境没有收款订单，跳过可退款订单候选检查"
else
  refundable_project_id="$(first_record_value "$orders_body" 'row => row.projectId')"
  refundable_member_id="$(first_record_value "$orders_body" 'row => row.memberId')"
  if [[ -n "$refundable_project_id" && -n "$refundable_member_id" ]]; then
    refundable_body="${tmp_dir}/refundable-orders.json"
    refundable_status="$(request GET "/api/payment/refundable-orders?projectId=${refundable_project_id}&memberId=${refundable_member_id}" "" "$tenant_token" "$refundable_body")"
    assert_status "退款可选已收款账单" 200 "$refundable_status" "$refundable_body"
    assert_json_code "退款可选已收款账单响应" 0 "$refundable_body"
  else
    skip "收款订单缺少小区或业主/住户ID，跳过可退款订单候选检查"
  fi
fi

if [[ "$run_accounting_mutations" == "true" && "$bill_count" != "0" ]]; then
  collect_bill_id="$(json_value "$bills_body" 'data => { const rows = data.data.records || []; const row = rows.find(item => Number(item.remainingAmount || 0) > 1); return row && row.billId; }')"
  if [[ -n "$collect_bill_id" ]]; then
    collect_body="${tmp_dir}/offline-collect.json"
    collect_status="$(request POST /api/payment/offline-collections "{\"projectId\":101,\"billIds\":[${collect_bill_id}],\"payChannel\":\"CASH\",\"amount\":1}" "$tenant_token" "$collect_body")"
    assert_status "现金部分收款" 200 "$collect_status" "$collect_body"
    assert_json_code "现金部分收款响应" 0 "$collect_body"
  else
    skip "没有剩余金额大于 1 元的账单，跳过现金部分收款变更检查"
  fi
else
  skip "未设置 RUN_MUTATING_ACCOUNTING_CHECKS=true，跳过会改变账务金额的收款检查"
fi

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
