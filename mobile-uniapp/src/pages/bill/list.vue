<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">账单缴费</text>
        <text class="page-sub">房屋、车位费用统一核对</text>
      </view>
      <text class="pill" @click="goHouse">{{ member.currentHouseNo || '选房屋' }}</text>
    </view>

    <view class="bill-total">
      <text class="total-label">{{ member.currentHouseNo || '当前未选择房屋' }} 待缴合计</text>
      <text class="total-amount">¥{{ unpaidTotal }}</text>
      <view class="total-meta">
        <text>待缴 {{ payableCount }} 笔</text>
        <text>已缴 {{ paidCount }} 笔</text>
      </view>
      <view class="total-actions">
        <button class="accent" @click="payFirst">立即缴费</button>
        <button class="light" @click="statusFilter = 'PAID'; load()">历史账单</button>
      </view>
    </view>

    <view class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="{ active: statusFilter === tab.value }"
        @click="statusFilter = tab.value; load()"
      >
        {{ tab.label }}
      </button>
    </view>

    <view v-if="!member.currentHouseId" class="empty-card">
      <text class="empty-title">请选择房屋</text>
      <text class="empty-text">绑定并选择房屋后，可以查看待缴账单和缴费记录。</text>
      <button @click="goHouse">去选择</button>
    </view>

    <view v-else class="section">
      <view class="section-head">
        <text>{{ statusFilter === 'PAID' ? '历史缴费' : '账单明细' }}</text>
        <text>{{ bills.length }} 笔</text>
      </view>
      <view v-if="bills.length" class="row-list">
        <view v-for="bill in bills" :key="String(bill.billId)" class="bill-card" @click="detail(bill)">
          <view class="bill-main">
            <view>
              <text class="row-title">{{ bill.feeItemName || bill.billNo }}</text>
              <text class="row-sub">{{ bill.houseNo || member.currentHouseNo || '-' }} · {{ bill.billPeriod || '-' }}</text>
            </view>
            <text class="status" :class="String(bill.status).toLowerCase()">{{ statusText(bill.status) }}</text>
          </view>
          <view class="amount-grid">
            <view>
              <text class="amount-label">应收</text>
              <text class="amount-value">¥{{ money(bill.receivableAmount) }}</text>
            </view>
            <view>
              <text class="amount-label">已收</text>
              <text class="amount-value paid">¥{{ money(bill.paidAmount) }}</text>
            </view>
            <view>
              <text class="amount-label">待收</text>
              <text class="amount-value due">¥{{ money(bill.remainingAmount) }}</text>
            </view>
          </view>
          <view class="card-foot">
            <text>{{ bill.paymentSummary ? '已有支付记录' : '暂无支付记录' }}</text>
            <button v-if="canPay(bill)" @click.stop="pay(bill)">缴费</button>
          </view>
        </view>
      </view>
      <view v-else-if="!loading" class="empty-card compact">
        <text class="empty-title">暂无账单</text>
        <text class="empty-text">当前筛选条件下没有账单记录。</text>
      </view>
    </view>

    <view class="section">
      <view class="tip-card">
        <text class="tip-title">缴费说明</text>
        <text class="tip-copy">当前演示环境使用模拟支付完成闭环；正式接入微信支付后，支付结果会由微信回调自动同步。</text>
      </view>
    </view>
    <AppTabBar active="service" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '@/components/AppTabBar.vue'
import { fetchBills } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const bills = ref<Record<string, unknown>[]>([])
const summaryBills = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const statusFilter = ref('ALL')
const tabs = [
  { label: '全部', value: 'ALL' },
  { label: '待缴', value: 'UNPAID' },
  { label: '部分缴', value: 'PARTIAL_PAID' },
  { label: '已缴', value: 'PAID' },
]

const unpaidTotal = computed(() => {
  const total = summaryBills.value
    .filter((bill) => canPay(bill))
    .reduce((sum, bill) => sum + Number(bill.remainingAmount ?? 0), 0)
  return money(total)
})

const payableCount = computed(() => summaryBills.value.filter((bill) => canPay(bill)).length)
const paidCount = computed(() => summaryBills.value.filter((bill) => String(bill.status) === 'PAID').length)

onShow(load)

async function load() {
  if (member.currentBindRole !== 'OWNER' && !member.currentAllowBill) {
    bills.value = []
    summaryBills.value = []
    uni.showToast({ title: '当前房屋未授权查看账单', icon: 'none' })
    return
  }
  if (!member.currentHouseId) {
    bills.value = []
    summaryBills.value = []
    return
  }
  loading.value = true
  try {
    const baseParams: Record<string, unknown> = { houseId: member.currentHouseId, pageNo: 1, pageSize: 100 }
    const listParams: Record<string, unknown> = { ...baseParams }
    if (statusFilter.value !== 'ALL') listParams.status = statusFilter.value
    const [list, summary] = await Promise.all([
      fetchBills(listParams),
      fetchBills(baseParams),
    ])
    bills.value = list.records
    summaryBills.value = summary.records
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '账单加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function statusText(status: unknown) {
  const value = String(status)
  const map: Record<string, string> = {
    UNPAID: '待缴',
    OVERDUE: '逾期',
    PARTIAL_PAID: '部分缴',
    PAID: '已缴',
    VOIDED: '作废',
  }
  return map[value] ?? value
}

function canPay(bill: Record<string, unknown>) {
  return ['UNPAID', 'OVERDUE', 'PARTIAL_PAID'].includes(String(bill.status))
}

function payFirst() {
  const bill = summaryBills.value.find(canPay)
  if (!bill) {
    uni.showToast({ title: '暂无待缴账单', icon: 'none' })
    return
  }
  pay(bill)
}

function pay(bill: Record<string, unknown>) {
  if (member.currentBindRole !== 'OWNER' && !member.currentAllowPayment) {
    uni.showToast({ title: '当前房屋未授权缴费', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages-sub/payment/cashier?billId=${bill.billId}&amount=${bill.remainingAmount}` })
}

function detail(bill: Record<string, unknown>) {
  uni.navigateTo({ url: `/pages/bill/detail?billId=${bill.billId}` })
}

function goHouse() {
  uni.navigateTo({ url: '/pages/house/switch' })
}

function money(value: unknown) {
  return Number(value ?? 0).toFixed(2)
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% -2%, rgba(15, 118, 110, .18), transparent 35%),
    linear-gradient(180deg, #eef8f5 0%, #f8fafc 45%, #f3f6f8 100%);
}

.screen-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 28rpx;
}

.page-title {
  display: block;
  color: #172033;
  font-size: 38rpx;
  font-weight: 900;
}

.page-sub {
  display: block;
  margin-top: 8rpx;
  color: #65758a;
  font-size: 24rpx;
}

.pill {
  max-width: 220rpx;
  padding: 12rpx 20rpx;
  overflow: hidden;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 900;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.bill-total {
  padding: 36rpx;
  color: #fff;
  background:
    radial-gradient(circle at 88% 20%, rgba(255, 255, 255, .26), transparent 24%),
    linear-gradient(135deg, #172554 0%, #0f766e 100%);
  border-radius: 36rpx;
}

.total-label {
  display: block;
  color: #d7f7ef;
  font-size: 24rpx;
  font-weight: 900;
}

.total-amount {
  display: block;
  margin-top: 16rpx;
  font-size: 64rpx;
  font-weight: 950;
  line-height: 1.1;
}

.total-meta {
  display: flex;
  gap: 22rpx;
  margin-top: 12rpx;
  color: #d7f7ef;
  font-size: 22rpx;
  font-weight: 800;
}

.total-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 28rpx;
}

.total-actions button {
  height: 72rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 900;
}

.accent {
  color: #1f2937;
  background: #f59e0b;
}

.light {
  color: #0b5f59;
  background: #fff;
}

.tabs {
  display: flex;
  gap: 14rpx;
  margin: 24rpx 0;
}

.tabs button {
  flex: 1;
  height: 64rpx;
  color: #475569;
  background: #fff;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 800;
}

.tabs button.active {
  color: #fff;
  background: #0f766e;
}

.section {
  margin-top: 28rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  margin: 0 4rpx 18rpx;
}

.section-head text:first-child {
  color: #172033;
  font-size: 31rpx;
  font-weight: 900;
}

.section-head text:last-child {
  color: #65758a;
  font-size: 23rpx;
  font-weight: 800;
}

.row-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.bill-card,
.tip-card,
.empty-card {
  background: rgba(255, 255, 255, .96);
  border: 1rpx solid #dfe9e6;
  border-radius: 28rpx;
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, .055);
}

.bill-card {
  width: 100%;
  padding: 26rpx;
  text-align: left;
}

.bill-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.row-title {
  display: block;
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
}

.row-sub {
  display: block;
  margin-top: 8rpx;
  color: #65758a;
  font-size: 22rpx;
}

.status {
  flex: none;
  padding: 8rpx 16rpx;
  color: #334155;
  background: #e2e8f0;
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 900;
}

.status.unpaid,
.status.overdue,
.status.partial_paid {
  color: #9a5b00;
  background: #fff4dd;
}

.status.paid {
  color: #0b5f59;
  background: #dff5ef;
}

.amount-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 22rpx 0;
  border-top: 1rpx solid #edf2f3;
  border-bottom: 1rpx solid #edf2f3;
}

.amount-label {
  display: block;
  color: #64748b;
  font-size: 21rpx;
}

.amount-value {
  display: block;
  margin-top: 8rpx;
  color: #172033;
  font-size: 27rpx;
  font-weight: 950;
}

.amount-value.paid {
  color: #0f766e;
}

.amount-value.due {
  color: #dc2626;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
  margin-top: 20rpx;
}

.card-foot text {
  color: #64748b;
  font-size: 22rpx;
}

.card-foot button {
  width: 142rpx;
  height: 58rpx;
  color: #fff;
  background: #0f766e;
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 900;
}

.tip-card {
  padding: 28rpx;
}

.tip-title {
  display: block;
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
}

.tip-copy {
  display: block;
  margin-top: 12rpx;
  color: #65758a;
  font-size: 23rpx;
  line-height: 1.55;
}

.empty-card {
  margin-top: 24rpx;
  padding: 56rpx 34rpx;
  text-align: center;
}

.empty-card.compact {
  margin-top: 0;
}

.empty-title {
  display: block;
  color: #172033;
  font-size: 32rpx;
  font-weight: 900;
}

.empty-text {
  display: block;
  margin: 14rpx 0 26rpx;
  color: #65758a;
  font-size: 25rpx;
  line-height: 1.6;
}

.empty-card button {
  width: 180rpx;
  height: 68rpx;
  color: #fff;
  background: #0f766e;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 900;
}
</style>
