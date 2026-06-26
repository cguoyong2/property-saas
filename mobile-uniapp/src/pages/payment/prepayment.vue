<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">预存款</text>
        <text class="page-sub">超收余额和账单抵扣明细</text>
      </view>
      <text class="pill">余额 ¥{{ money(summary.remainingAmount) }}</text>
    </view>

    <view class="balance-card">
      <text class="balance-label">当前可用余额</text>
      <text class="balance-amount">¥{{ money(summary.remainingAmount) }}</text>
      <view class="balance-meta">
        <text>累计转入 ¥{{ money(summary.totalAmount) }}</text>
        <text>累计抵扣 ¥{{ money(summary.usedAmount) }}</text>
      </view>
    </view>

    <view class="section-head">
      <text>收支明细</text>
      <text>{{ total }} 条</text>
    </view>

    <view v-if="records.length" class="ledger-list">
      <view v-for="record in records" :key="`${record.direction}-${record.businessNo}-${record.createdAt}`" class="ledger-card">
        <view class="ledger-head">
          <view>
            <text class="ledger-title">{{ title(record) }}</text>
            <text class="ledger-sub">{{ record.projectName || '-' }} · {{ record.createdAt || '-' }}</text>
          </view>
          <text class="amount" :class="String(record.direction).toLowerCase()">
            {{ record.direction === 'IN' ? '+' : '-' }}¥{{ money(record.amount) }}
          </text>
        </view>
        <view class="ledger-detail">
          <text>{{ record.houseNo || record.businessNo || '-' }}</text>
          <text v-if="record.remainingAmount !== null && record.remainingAmount !== undefined">
            剩余 ¥{{ money(record.remainingAmount) }}
          </text>
        </view>
      </view>
    </view>

    <view v-else-if="!loading" class="empty-card">
      <text class="empty-title">暂无预存款</text>
      <text class="empty-text">当收款金额大于应收金额时，超出部分会自动进入预存款。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchPrepayments, fetchPrepaymentSummary } from '@/api/app'

const records = ref<Record<string, unknown>[]>([])
const total = ref(0)
const loading = ref(false)
const summary = reactive<Record<string, unknown>>({
  totalAmount: 0,
  usedAmount: 0,
  remainingAmount: 0,
})

onShow(load)

async function load() {
  loading.value = true
  try {
    const [page, stat] = await Promise.all([
      fetchPrepayments({ pageNo: 1, pageSize: 100 }),
      fetchPrepaymentSummary(),
    ])
    records.value = page.records
    total.value = page.total
    Object.assign(summary, stat)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '预存款加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function title(record: Record<string, unknown>) {
  if (record.direction === 'IN') return sourceText(record.source)
  return `${record.feeItemName || '账单抵扣'} ${record.billPeriod || ''}`.trim()
}

function sourceText(source: unknown) {
  const map: Record<string, string> = {
    OFFLINE_OVERPAY: '超收转入预存款',
    AUTO_BILL_OFFSET: '账单自动抵扣',
  }
  return map[String(source)] ?? String(source ?? '预存款')
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

.screen-title,
.balance-meta,
.section-head,
.ledger-head,
.ledger-detail {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.screen-title {
  align-items: flex-start;
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

.balance-card {
  padding: 34rpx;
  color: #fff;
  background:
    radial-gradient(circle at 88% 18%, rgba(255, 255, 255, .24), transparent 24%),
    linear-gradient(135deg, #172554 0%, #0f766e 100%);
  border-radius: 36rpx;
}

.balance-label,
.balance-meta {
  color: #d7f7ef;
  font-size: 23rpx;
  font-weight: 800;
}

.balance-amount {
  display: block;
  margin: 14rpx 0 16rpx;
  font-size: 64rpx;
  font-weight: 950;
}

.section-head {
  margin: 28rpx 4rpx 16rpx;
}

.section-head text:first-child {
  color: #172033;
  font-size: 31rpx;
  font-weight: 900;
}

.section-head text:last-child {
  color: #64748b;
  font-size: 23rpx;
  font-weight: 800;
}

.ledger-list {
  display: grid;
  gap: 18rpx;
}

.ledger-card,
.empty-card {
  padding: 28rpx;
  background: #fff;
  border: 1rpx solid #dfe9e6;
  border-radius: 28rpx;
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, .055);
}

.ledger-title {
  display: block;
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
}

.ledger-sub,
.ledger-detail,
.empty-text {
  color: #64748b;
  font-size: 23rpx;
}

.ledger-sub {
  display: block;
  margin-top: 8rpx;
}

.amount {
  flex: none;
  color: #dc2626;
  font-size: 32rpx;
  font-weight: 950;
}

.amount.in {
  color: #0f766e;
}

.ledger-detail {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #edf2f3;
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
