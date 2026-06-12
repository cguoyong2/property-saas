<template>
  <view class="page">
    <view v-if="bill.billId" class="card">
      <view class="row">
        <text class="title">{{ bill.billNo }}</text>
        <text class="status">{{ bill.status }}</text>
      </view>
      <view class="amount">{{ bill.remainingAmount }} 元</view>
      <view class="line"><text>账期</text><text>{{ bill.billPeriod }}</text></view>
      <view class="line"><text>应收</text><text>{{ bill.receivableAmount }} 元</text></view>
      <view class="line"><text>优惠</text><text>{{ bill.discountAmount }} 元</text></view>
      <view class="line"><text>已付</text><text>{{ bill.paidAmount }} 元</text></view>
      <view class="line"><text>退款</text><text>{{ bill.refundAmount }} 元</text></view>
      <view class="line"><text>到期日</text><text>{{ bill.dueDate || '-' }}</text></view>
      <button v-if="canPay" class="primary" @click="pay">去支付</button>
    </view>
    <view v-else class="empty">账单加载中</view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchBillDetail } from '@/api/app'

const billId = ref('')
const bill = reactive<Record<string, unknown>>({})
const canPay = computed(() => ['UNPAID', 'OVERDUE', 'PARTIAL_PAID'].includes(String(bill.status)))

onLoad(async (query) => {
  billId.value = String(query?.billId ?? '')
  if (!billId.value) return
  try {
    Object.assign(bill, await fetchBillDetail(billId.value))
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' })
  }
})

function pay() {
  uni.navigateTo({ url: `/pages-sub/payment/cashier?billId=${bill.billId}&amount=${bill.remainingAmount}` })
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { padding: 30rpx; background: #fff; border-radius: 14rpx; }
.row, .line { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 30rpx; font-weight: 800; }
.status { color: #0f766e; font-size: 24rpx; font-weight: 700; }
.amount { margin: 28rpx 0; color: #dc2626; font-size: 44rpx; font-weight: 900; }
.line { padding: 18rpx 0; border-top: 1rpx solid #e2e8f0; color: #475569; font-size: 26rpx; }
.primary { margin-top: 28rpx; background: #0f766e; color: #fff; }
.empty { padding: 80rpx 0; text-align: center; color: #94a3b8; }
</style>
