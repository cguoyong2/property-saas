<template>
  <view class="page">
    <view v-if="bill.billId" class="hero">
      <view class="hero-head">
        <view>
          <text class="eyebrow">{{ bill.feeItemName || '物业账单' }}</text>
          <text class="title">{{ bill.houseNo || '-' }}</text>
        </view>
        <text class="status" :class="String(bill.status).toLowerCase()">{{ statusText(bill.status) }}</text>
      </view>
      <text class="amount">¥{{ money(bill.remainingAmount) }}</text>
      <text class="sub">账期 {{ bill.billPeriod || '-' }} · 待缴金额</text>
    </view>

    <view v-if="bill.billId" class="card">
      <view class="card-title">
        <text>费用组成</text>
        <text>{{ bill.billNo }}</text>
      </view>
      <view class="line"><text>收费项目</text><text>{{ bill.feeItemName || '-' }}</text></view>
      <view class="line"><text>应收金额</text><text>¥{{ money(bill.receivableAmount) }}</text></view>
      <view class="line"><text>优惠减免</text><text>¥{{ money(bill.discountAmount) }}</text></view>
      <view class="line"><text>预存抵扣</text><text>¥{{ money(bill.prepaymentAppliedAmount) }}</text></view>
      <view class="line"><text>已收金额</text><text>¥{{ money(bill.paidAmount) }}</text></view>
      <view class="line"><text>已退金额</text><text>¥{{ money(bill.refundAmount) }}</text></view>
      <view class="line strong"><text>剩余待缴</text><text>¥{{ money(bill.remainingAmount) }}</text></view>
      <view class="line"><text>到期日</text><text>{{ bill.dueDate || '-' }}</text></view>
    </view>

    <view v-if="bill.billId" class="card">
      <view class="card-title">
        <text>支付记录</text>
        <text>{{ bill.paymentSummary ? '有记录' : '暂无' }}</text>
      </view>
      <text class="summary">{{ readableSummary(bill.paymentSummary, '暂无支付记录') }}</text>
    </view>

    <view v-if="bill.billId" class="card">
      <view class="card-title">
        <text>退款记录</text>
        <text>{{ bill.refundSummary ? '有记录' : '暂无' }}</text>
      </view>
      <text class="summary">{{ readableSummary(bill.refundSummary, '暂无退款记录') }}</text>
    </view>

    <view v-if="bill.billId" class="bottom-actions">
      <button v-if="canPay" class="primary" @click="pay">支付剩余 ¥{{ money(bill.remainingAmount) }}</button>
      <button v-else class="disabled">{{ String(bill.status) === 'PAID' ? '已缴清' : statusText(bill.status) }}</button>
    </view>

    <view v-else class="empty">账单加载中</view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchBillDetail } from '@/api/app'

const billId = ref('')
const bill = reactive<Record<string, unknown>>({})
const canPay = computed(() => ['UNPAID', 'OVERDUE', 'PARTIAL_PAID'].includes(String(bill.status)))

onLoad((query) => {
  billId.value = String(query?.billId ?? '')
})

onShow(load)

async function load() {
  if (!billId.value) return
  try {
    Object.assign(bill, await fetchBillDetail(billId.value))
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' })
  }
}

function pay() {
  uni.navigateTo({ url: `/pages-sub/payment/cashier?billId=${bill.billId}&amount=${bill.remainingAmount}` })
}

function statusText(status: unknown) {
  const value = String(status)
  const map: Record<string, string> = {
    UNPAID: '待缴',
    OVERDUE: '逾期',
    PARTIAL_PAID: '部分缴',
    PAID: '已缴',
    VOID: '作废',
    VOIDED: '作废',
  }
  return map[value] ?? value
}

function readableSummary(value: unknown, fallback: string) {
  const text = String(value ?? '').trim()
  if (!text) return fallback
  return text
    .replaceAll('WECHAT', '微信')
    .replaceAll('CASH', '现金')
    .replaceAll('OFFLINE', '线下')
    .replaceAll('POS', 'POS')
    .replaceAll('PAID', '已支付')
    .replaceAll('PAYING', '待支付')
    .replaceAll('PENDING', '待处理')
    .replaceAll('APPLYING', '申请中')
    .replaceAll('REFUNDED', '已退款')
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

.hero {
  padding: 34rpx;
  color: #fff;
  background:
    radial-gradient(circle at 86% 18%, rgba(255, 255, 255, .24), transparent 24%),
    linear-gradient(135deg, #172554 0%, #0f766e 100%);
  border-radius: 36rpx;
}

.hero-head {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
}

.eyebrow {
  display: block;
  color: #d7f7ef;
  font-size: 23rpx;
  font-weight: 800;
}

.title {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 950;
}

.status {
  flex: none;
  align-self: flex-start;
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

.amount {
  display: block;
  margin-top: 30rpx;
  font-size: 62rpx;
  font-weight: 950;
}

.sub {
  display: block;
  margin-top: 8rpx;
  color: #d7f7ef;
  font-size: 23rpx;
}

.card {
  margin-top: 22rpx;
  padding: 28rpx;
  background: #fff;
  border: 1rpx solid #dfe9e6;
  border-radius: 28rpx;
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, .055);
}

.card-title {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 14rpx;
}

.card-title text:first-child {
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
}

.card-title text:last-child {
  color: #64748b;
  font-size: 22rpx;
}

.line {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  padding: 18rpx 0;
  border-top: 1rpx solid #edf2f3;
  color: #475569;
  font-size: 25rpx;
}

.line text:last-child {
  color: #172033;
  font-weight: 850;
  text-align: right;
}

.line.strong text {
  color: #dc2626;
  font-weight: 950;
}

.summary {
  display: block;
  color: #475569;
  font-size: 24rpx;
  line-height: 1.6;
}

.bottom-actions {
  margin: 30rpx 0 24rpx;
}

.bottom-actions button {
  height: 84rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 900;
}

.primary {
  color: #fff;
  background: #0f766e;
}

.disabled {
  color: #64748b;
  background: #e2e8f0;
}

.empty {
  padding: 80rpx 0;
  text-align: center;
  color: #94a3b8;
}
</style>
