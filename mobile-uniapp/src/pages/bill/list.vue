<template>
  <view class="page">
    <view class="header">
      <view>
        <text class="caption">当前房屋</text>
        <text class="house">{{ member.currentHouseNo || '未选择房屋' }}</text>
      </view>
      <button class="switch" @click="goHouse">切换</button>
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

    <view v-for="bill in bills" v-else :key="String(bill.billId)" class="card" @click="detail(bill)">
      <view class="row">
        <text class="title">{{ bill.billNo }}</text>
        <text class="amount">￥{{ bill.remainingAmount }}</text>
      </view>
      <view class="meta-row">
        <text class="meta">账期：{{ bill.billPeriod }}</text>
        <text class="status" :class="String(bill.status).toLowerCase()">{{ bill.status }}</text>
      </view>
      <view class="actions">
        <button class="secondary" @click.stop="detail(bill)">详情</button>
        <button v-if="canPay(bill)" @click.stop="pay(bill)">去支付</button>
      </view>
    </view>
    <view v-if="member.currentHouseId && !loading && !bills.length" class="empty-card">
      <text class="empty-title">暂无账单</text>
      <text class="empty-text">当前筛选条件下没有账单记录。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBills } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const bills = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const statusFilter = ref('ALL')
const tabs = [
  { label: '全部', value: 'ALL' },
  { label: '待缴', value: 'UNPAID' },
  { label: '已缴', value: 'PAID' },
]

onShow(load)

async function load() {
  if (!member.currentHouseId) {
    bills.value = []
    return
  }
  loading.value = true
  try {
    const params: Record<string, unknown> = { houseId: member.currentHouseId, pageNo: 1, pageSize: 50 }
    if (statusFilter.value !== 'ALL') params.status = statusFilter.value
    bills.value = (await fetchBills(params)).records
  } finally {
    loading.value = false
  }
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

function goHouse() {
  uni.navigateTo({ url: '/pages/house/switch' })
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #eef4f3; }
.header { display: flex; justify-content: space-between; align-items: center; padding: 30rpx; background: #0f766e; border-radius: 22rpx; color: #fff; }
.caption { display: block; color: #ccfbf1; font-size: 23rpx; }
.house { display: block; margin-top: 8rpx; font-size: 34rpx; font-weight: 800; }
.switch { width: 120rpx; height: 60rpx; border-radius: 30rpx; background: #fff; color: #0f766e; font-size: 25rpx; font-weight: 700; }
.tabs { display: flex; gap: 14rpx; margin: 22rpx 0; }
.tabs button { flex: 1; height: 64rpx; border-radius: 32rpx; background: #fff; color: #475569; font-size: 25rpx; }
.tabs button.active { background: #172554; color: #fff; }
.card { margin-bottom: 18rpx; padding: 28rpx; background: #fff; border-radius: 18rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.row { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 28rpx; font-weight: 700; }
.amount { color: #b45309; font-size: 34rpx; font-weight: 900; }
.meta-row { display: flex; justify-content: space-between; align-items: center; margin: 18rpx 0; }
.meta { color: #64748b; font-size: 24rpx; }
.status { padding: 8rpx 16rpx; border-radius: 999rpx; background: #e2e8f0; color: #334155; font-size: 22rpx; }
.status.unpaid, .status.overdue, .status.partial_paid { background: #fef3c7; color: #92400e; }
.status.paid { background: #dcfce7; color: #166534; }
.actions { display: flex; gap: 16rpx; margin-top: 14rpx; }
.actions button { flex: 1; height: 72rpx; border-radius: 36rpx; background: #0f766e; color: #fff; font-size: 26rpx; }
.actions .secondary { background: #e2e8f0; color: #334155; }
.empty-card { margin-top: 24rpx; padding: 56rpx 34rpx; text-align: center; background: #fff; border-radius: 20rpx; }
.empty-title { display: block; font-size: 32rpx; font-weight: 800; }
.empty-text { display: block; margin: 14rpx 0 26rpx; color: #64748b; font-size: 25rpx; line-height: 1.6; }
.empty-card button { width: 180rpx; height: 68rpx; border-radius: 34rpx; background: #0f766e; color: #fff; font-size: 26rpx; }
</style>
