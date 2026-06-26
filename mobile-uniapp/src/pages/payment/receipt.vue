<template>
  <view class="page">
    <view v-if="order.orderNo" class="receipt">
      <view class="receipt-top">
        <text class="mark">收款凭证</text>
        <text class="status">{{ statusText(order.status) }}</text>
      </view>
      <text class="amount">¥{{ money(order.transactionAmount || order.amount) }}</text>
      <text class="receipt-no">{{ order.orderNo }}</text>
    </view>

    <view v-if="order.orderNo" class="card">
      <view class="card-title">
        <text>付款信息</text>
        <text>{{ channelText(order.payChannel) }}</text>
      </view>
      <view class="line"><text>小区名称</text><text>{{ order.projectName || '-' }}</text></view>
      <view class="line"><text>房号</text><text>{{ order.houseNo || '-' }}</text></view>
      <view class="line"><text>账单笔数</text><text>{{ order.billCount || 0 }} 笔</text></view>
      <view class="line"><text>订单金额</text><text>¥{{ money(order.amount) }}</text></view>
      <view class="line"><text>实收金额</text><text>¥{{ money(order.transactionAmount) }}</text></view>
      <view class="line"><text>已退金额</text><text>¥{{ money(order.refundedAmount) }}</text></view>
      <view class="line"><text>转预存款</text><text>¥{{ money(order.prepaymentAmount) }}</text></view>
      <view class="line"><text>支付时间</text><text>{{ order.paidAt || '-' }}</text></view>
    </view>

    <view v-if="order.orderNo" class="card">
      <view class="card-title">
        <text>费用明细</text>
        <text>{{ order.billAppliedAmount ? `核销 ¥${money(order.billAppliedAmount)}` : '-' }}</text>
      </view>
      <text class="summary">{{ order.billSummary || '暂无账单明细' }}</text>
    </view>

    <view v-if="order.orderNo" class="card">
      <view class="card-title">
        <text>凭证说明</text>
        <text>系统生成</text>
      </view>
      <text class="summary">本凭证根据物业系统收款记录生成，可用于核对缴费、退款和预存款。正式接入微信支付后，第三方流水号会同步展示。</text>
    </view>

    <view v-else class="empty">凭证加载中</view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchPayOrderDetail } from '@/api/app'

const orderNo = ref('')
const order = reactive<Record<string, unknown>>({})

onLoad((query) => {
  orderNo.value = String(query?.orderNo ?? '')
})

onShow(load)

async function load() {
  if (!orderNo.value) return
  try {
    Object.assign(order, await fetchPayOrderDetail(orderNo.value))
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '凭证加载失败', icon: 'none' })
  }
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
    WECHAT: '微信支付',
    CASH: '现金收款',
    OFFLINE: '线下收款',
    POS: 'POS收款',
    BANK_TRANSFER: '银行转账',
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

.receipt {
  padding: 36rpx;
  color: #fff;
  background:
    radial-gradient(circle at 88% 18%, rgba(255, 255, 255, .24), transparent 24%),
    linear-gradient(135deg, #172554 0%, #0f766e 100%);
  border-radius: 36rpx;
}

.receipt-top,
.line,
.card-title {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.mark {
  color: #d7f7ef;
  font-size: 24rpx;
  font-weight: 900;
}

.status {
  padding: 8rpx 16rpx;
  color: #0b5f59;
  background: #fff;
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 900;
}

.amount {
  display: block;
  margin-top: 30rpx;
  font-size: 64rpx;
  font-weight: 950;
}

.receipt-no {
  display: block;
  margin-top: 10rpx;
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
  padding: 18rpx 0;
  border-top: 1rpx solid #edf2f3;
  color: #475569;
  font-size: 25rpx;
}

.line text:last-child {
  max-width: 60%;
  color: #172033;
  font-weight: 850;
  text-align: right;
}

.summary {
  display: block;
  color: #475569;
  font-size: 24rpx;
  line-height: 1.7;
}

.empty {
  padding: 80rpx 0;
  color: #94a3b8;
  text-align: center;
}
</style>
