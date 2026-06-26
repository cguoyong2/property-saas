<template>
  <view class="page">
    <view class="panel">
      <text class="label">本次支付金额</text>
      <text class="amount">¥{{ money(amount) }}</text>
      <view class="meta">
        <text>支付方式</text>
        <text>微信支付</text>
      </view>
      <view v-if="orderNo" class="order-card">
        <text class="order-label">支付订单</text>
        <text class="order-no">{{ orderNo }}</text>
        <text class="order-tip">当前为演示环境，确认后会模拟微信支付成功并刷新账单状态。</text>
      </view>
      <button v-if="!orderNo" class="primary" :loading="loading" @click="createOrder">创建支付订单</button>
      <button v-else class="primary" :loading="confirming" @click="confirmPayment">确认模拟支付完成</button>
      <button class="light" @click="goBack">返回账单</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { confirmDemoPayOrder, createPayOrder } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const billId = ref('')
const amount = ref('0.00')
const orderNo = ref('')
const loading = ref(false)
const confirming = ref(false)

onLoad((query) => {
  billId.value = String(query?.billId ?? '')
  amount.value = String(query?.amount ?? '0.00')
})

async function createOrder() {
  if (!billId.value) {
    uni.showToast({ title: '账单信息缺失', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const order = await createPayOrder({
      projectId: member.currentProjectId,
      billIds: [Number(billId.value)],
      payChannel: 'WECHAT',
      amount: Number(amount.value),
    })
    orderNo.value = String(order.orderNo ?? '')
    uni.showToast({ title: '订单已创建', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '创建失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function confirmPayment() {
  if (!orderNo.value) return
  confirming.value = true
  try {
    await confirmDemoPayOrder(orderNo.value)
    uni.showToast({ title: '支付已完成', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 700)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '支付确认失败', icon: 'none' })
  } finally {
    confirming.value = false
  }
}

function goBack() {
  uni.navigateBack()
}

function money(value: unknown) {
  return Number(value ?? 0).toFixed(2)
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 32rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% -2%, rgba(15, 118, 110, .18), transparent 35%),
    linear-gradient(180deg, #eef8f5 0%, #f8fafc 45%, #f3f6f8 100%);
}

.panel {
  padding: 40rpx;
  background: #fff;
  border: 1rpx solid #dfe9e6;
  border-radius: 32rpx;
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, .08);
}

.label {
  display: block;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 800;
}

.amount {
  display: block;
  margin: 18rpx 0 34rpx;
  color: #dc2626;
  font-size: 60rpx;
  font-weight: 950;
}

.meta {
  display: flex;
  justify-content: space-between;
  padding: 22rpx 0;
  border-top: 1rpx solid #edf2f3;
  border-bottom: 1rpx solid #edf2f3;
  color: #475569;
  font-size: 25rpx;
}

.meta text:last-child {
  color: #172033;
  font-weight: 900;
}

.order-card {
  margin-top: 28rpx;
  padding: 24rpx;
  background: #f0fdfa;
  border: 1rpx solid #cae9e4;
  border-radius: 24rpx;
}

.order-label {
  display: block;
  color: #0f766e;
  font-size: 22rpx;
  font-weight: 900;
}

.order-no {
  display: block;
  margin-top: 8rpx;
  color: #172033;
  font-size: 25rpx;
  font-weight: 900;
}

.order-tip {
  display: block;
  margin-top: 12rpx;
  color: #64748b;
  font-size: 22rpx;
  line-height: 1.55;
}

button {
  height: 82rpx;
  margin-top: 28rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 900;
}

.primary {
  color: #fff;
  background: #0f766e;
}

.light {
  color: #0b5f59;
  background: #e7f4f1;
}
</style>
