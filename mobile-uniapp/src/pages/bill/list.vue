<template>
  <view class="page">
    <view class="current">当前房屋：{{ member.currentHouseNo || '未选择' }}</view>
    <view v-for="bill in bills" :key="String(bill.billId)" class="card" @click="detail(bill)">
      <view class="row">
        <text class="title">{{ bill.billNo }}</text>
        <text class="amount">{{ bill.remainingAmount }} 元</text>
      </view>
      <text class="meta">账期：{{ bill.billPeriod }} ｜ 状态：{{ bill.status }}</text>
      <view class="actions">
        <button class="secondary" @click.stop="detail(bill)">详情</button>
        <button v-if="canPay(bill)" @click.stop="pay(bill)">去支付</button>
      </view>
    </view>
    <view v-if="!bills.length" class="empty">暂无账单</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBills } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const bills = ref<Record<string, unknown>[]>([])

onShow(load)

async function load() {
  if (!member.currentHouseId) {
    uni.navigateTo({ url: '/pages/house/switch' })
    return
  }
  bills.value = (await fetchBills({ houseId: member.currentHouseId, pageNo: 1, pageSize: 50 })).records
}

function canPay(bill: Record<string, unknown>) {
  return ['UNPAID', 'OVERDUE', 'PARTIAL_PAID'].includes(String(bill.status))
}

function pay(bill: Record<string, unknown>) {
  uni.navigateTo({ url: `/pages-sub/payment/cashier?billId=${bill.billId}&amount=${bill.remainingAmount}` })
}

function detail(bill: Record<string, unknown>) {
  uni.navigateTo({ url: `/pages/bill/detail?billId=${bill.billId}` })
}
</script>

<style scoped>
.page { padding: 24rpx; }
.current { margin-bottom: 20rpx; color: #475569; }
.card { margin-bottom: 18rpx; padding: 28rpx; background: #fff; border-radius: 14rpx; }
.row { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 28rpx; font-weight: 700; }
.amount { color: #dc2626; font-weight: 800; }
.meta { display: block; margin: 14rpx 0; color: #64748b; font-size: 24rpx; }
.actions { display: flex; gap: 16rpx; margin-top: 14rpx; }
button { flex: 1; background: #0f766e; color: #fff; }
.secondary { background: #e2e8f0; color: #334155; }
.empty { padding: 80rpx 0; text-align: center; color: #94a3b8; }
</style>
