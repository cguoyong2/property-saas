<template>
  <view class="page">
    <view class="panel">
      <text class="label">待支付金额</text>
      <text class="amount">{{ amount }} 元</text>
      <button class="primary" :loading="loading" @click="createOrder">创建微信支付订单</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createPayOrder } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const billId = ref('')
const amount = ref('')
const loading = ref(false)

onLoad((query) => {
  billId.value = String(query?.billId ?? '')
  amount.value = String(query?.amount ?? '0.00')
})

async function createOrder() {
  loading.value = true
  try {
    const order = await createPayOrder({
      projectId: member.currentProjectId,
      billIds: [Number(billId.value)],
      payChannel: 'WECHAT',
    })
    uni.showModal({ title: '支付订单已创建', content: `订单号：${order.orderNo}`, showCancel: false })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '创建失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { padding: 32rpx; }
.panel { padding: 40rpx; background: #fff; border-radius: 16rpx; text-align: center; }
.label { display: block; color: #64748b; }
.amount { display: block; margin: 24rpx 0 40rpx; color: #dc2626; font-size: 52rpx; font-weight: 800; }
.primary { background: #0f766e; color: #fff; }
</style>
