<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">缴费记录</text>
        <text class="page-sub">查看已支付订单和收款凭证</text>
      </view>
      <text class="pill">共 {{ total }} 笔</text>
    </view>

    <view class="summary-card">
      <text class="summary-label">已收合计</text>
      <text class="summary-amount">¥{{ money(paidTotal) }}</text>
      <text class="summary-copy">包含现金、收款码、小程序支付及后续退款状态。</text>
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

    <view v-if="orders.length" class="order-list">
      <button v-for="order in orders" :key="String(order.orderNo)" class="order-card" @click="receipt(order)">
        <view class="order-head">
          <view>
            <text class="order-title">{{ order.projectName || '物业缴费' }}</text>
            <text class="order-sub">{{ order.houseNo || '-' }} · {{ channelText(order.payChannel) }}</text>
          </view>
          <text class="status" :class="String(order.status).toLowerCase()">{{ statusText(order.status) }}</text>
        </view>
        <view class="amount-row">
          <view>
            <text class="label">订单金额</text>
            <text class="value">¥{{ money(order.amount) }}</text>
          </view>
          <view>
            <text class="label">实收</text>
            <text class="value paid">¥{{ money(order.transactionAmount) }}</text>
          </view>
          <view>
            <text class="label">已退</text>
            <text class="value refund">¥{{ money(order.refundedAmount) }}</text>
          </view>
        </view>
        <view class="detail-line">
          <text>{{ order.billSummary || order.subject || '-' }}</text>
        </view>
        <view class="foot">
          <text>{{ order.paidAt || order.createdAt || '-' }}</text>
          <text>查看凭证 ›</text>
        </view>
      </button>
    </view>

    <view v-else-if="!loading" class="empty-card">
      <text class="empty-title">暂无缴费记录</text>
      <text class="empty-text">完成缴费后，可以在这里查看记录和凭证。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchPayOrders } from '@/api/app'

const orders = ref<Record<string, unknown>[]>([])
const total = ref(0)
const loading = ref(false)
const statusFilter = ref('SUCCESS')
const tabs = [
  { label: '已完成', value: 'SUCCESS' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已退款', value: 'REFUNDED' },
  { label: '全部', value: 'ALL' },
]

const paidTotal = computed(() => orders.value.reduce((sum, order) => sum + Number(order.transactionAmount ?? 0), 0))

onShow(load)

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageNo: 1, pageSize: 50 }
    if (statusFilter.value !== 'ALL') params.status = statusFilter.value
    const page = await fetchPayOrders(params)
    orders.value = page.records
    total.value = page.total
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '缴费记录加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function receipt(order: Record<string, unknown>) {
  uni.navigateTo({ url: `/pages/payment/receipt?orderNo=${order.orderNo}` })
}

function statusText(status: unknown) {
  const map: Record<string, string> = {
    PENDING: '待支付',
    PAYING: '待支付',
    PAID: '已支付',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    PARTIAL_REFUNDED: '部分退款',
    CLOSED: '已关闭',
    FAILED: '失败',
  }
  return map[String(status)] ?? String(status ?? '-')
}

function channelText(channel: unknown) {
  const map: Record<string, string> = {
    WECHAT: '微信',
    CASH: '现金',
    OFFLINE: '线下收款',
    POS: 'POS',
    BANK_TRANSFER: '转账',
  }
  return map[String(channel)] ?? String(channel ?? '-')
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
  flex: none;
  padding: 12rpx 20rpx;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 900;
}

.summary-card {
  padding: 34rpx;
  color: #fff;
  background:
    radial-gradient(circle at 88% 18%, rgba(255, 255, 255, .24), transparent 24%),
    linear-gradient(135deg, #172554 0%, #0f766e 100%);
  border-radius: 36rpx;
}

.summary-label,
.summary-copy {
  display: block;
  color: #d7f7ef;
  font-size: 23rpx;
  font-weight: 800;
}

.summary-amount {
  display: block;
  margin: 14rpx 0 8rpx;
  font-size: 60rpx;
  font-weight: 950;
}

.tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  margin: 22rpx 0;
}

.tabs button {
  height: 64rpx;
  color: #64748b;
  background: #fff;
  border: 1rpx solid #dfe9e6;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 900;
}

.tabs button.active {
  color: #fff;
  background: #0f766e;
}

.order-list {
  display: grid;
  gap: 18rpx;
}

.order-card,
.empty-card {
  width: 100%;
  padding: 28rpx;
  background: #fff;
  border: 1rpx solid #dfe9e6;
  border-radius: 28rpx;
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, .055);
  text-align: left;
}

.order-head,
.amount-row,
.foot {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
}

.order-title {
  display: block;
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
}

.order-sub,
.label,
.foot,
.empty-text {
  color: #64748b;
  font-size: 23rpx;
}

.order-sub {
  display: block;
  margin-top: 8rpx;
}

.status {
  flex: none;
  align-self: flex-start;
  padding: 8rpx 16rpx;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 900;
}

.status.refunding,
.status.partial_refunded {
  color: #9a5b00;
  background: #fff4dd;
}

.status.refunded,
.status.closed,
.status.failed {
  color: #64748b;
  background: #e2e8f0;
}

.amount-row {
  margin-top: 22rpx;
}

.value {
  display: block;
  margin-top: 6rpx;
  color: #172033;
  font-size: 28rpx;
  font-weight: 950;
}

.value.paid {
  color: #0f766e;
}

.value.refund {
  color: #dc2626;
}

.detail-line {
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #edf2f3;
  color: #475569;
  font-size: 24rpx;
  line-height: 1.6;
}

.foot {
  margin-top: 18rpx;
}

.foot text:last-child {
  color: #0f766e;
  font-weight: 900;
}

.empty-title {
  display: block;
  color: #172033;
  font-size: 30rpx;
  font-weight: 900;
}

.empty-text {
  display: block;
  margin-top: 10rpx;
}
</style>
