<template>
  <view class="page">
    <view class="header">
      <view>
        <text class="title-page">我的工单</text>
        <text class="sub-page">{{ member.currentHouseNo || '当前未选择房屋' }}</text>
      </view>
      <button @click="goCreate">报修</button>
    </view>

    <view class="toolbar">
      <button class="primary" @click="goCreate">新增报修</button>
      <button class="ghost" @click="goComplaint">投诉建议</button>
    </view>

    <view v-for="item in records" :key="String(item.workOrderId)" class="card" @click="detail(item)">
      <view class="card-head">
        <text class="title">{{ item.title }}</text>
        <text class="status" :class="String(item.status).toLowerCase()">{{ item.status }}</text>
      </view>
      <text class="meta">{{ item.orderNo }} ｜ {{ item.orderType }}</text>
      <text class="desc">{{ item.description }}</text>
    </view>
    <view v-if="!records.length" class="empty">
      <text class="empty-title">暂无工单</text>
      <text class="empty-text">提交报修或投诉后，可在这里查看处理进度。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchWorkOrders } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const records = ref<Record<string, unknown>[]>([])

onShow(async () => {
  if (!member.currentProjectId || !member.memberId) return
  records.value = (await fetchWorkOrders({
    projectId: member.currentProjectId,
    memberId: member.memberId,
    pageNo: 1,
    pageSize: 50,
  })).records
})

function goCreate() {
  uni.navigateTo({ url: '/pages/workorder/create' })
}

function goComplaint() {
  uni.navigateTo({ url: '/pages/workorder/complaint' })
}

function detail(item: Record<string, unknown>) {
  uni.navigateTo({ url: `/pages/workorder/detail?workOrderId=${item.workOrderId}` })
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #eef4f3; }
.header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18rpx; padding: 32rpx; border-radius: 22rpx; background: #172554; color: #fff; }
.title-page { display: block; font-size: 38rpx; font-weight: 900; }
.sub-page { display: block; margin-top: 8rpx; color: #bfdbfe; font-size: 24rpx; }
.header button { width: 120rpx; height: 62rpx; border-radius: 31rpx; background: #f59e0b; color: #111827; font-size: 25rpx; font-weight: 800; }
.toolbar { display: flex; gap: 16rpx; margin-bottom: 20rpx; }
.toolbar button { flex: 1; height: 72rpx; border-radius: 36rpx; font-size: 26rpx; }
.primary { background: #0f766e; color: #fff; }
.ghost { background: #e2e8f0; color: #334155; }
.card { margin-bottom: 18rpx; padding: 28rpx; background: #fff; border-radius: 18rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.card-head { display: flex; justify-content: space-between; gap: 18rpx; align-items: flex-start; }
.title { flex: 1; display: block; font-size: 30rpx; font-weight: 800; line-height: 1.35; }
.status { flex: none; padding: 8rpx 16rpx; border-radius: 999rpx; background: #e2e8f0; color: #334155; font-size: 22rpx; }
.status.pending, .status.accepted, .status.dispatched { background: #dbeafe; color: #1d4ed8; }
.status.processing, .status.completed { background: #dcfce7; color: #166534; }
.status.cancelled, .status.rejected { background: #fee2e2; color: #991b1b; }
.meta, .desc { display: block; margin-top: 12rpx; color: #64748b; font-size: 24rpx; }
.desc { line-height: 1.55; }
.empty { padding: 70rpx 34rpx; text-align: center; background: #fff; border-radius: 20rpx; }
.empty-title { display: block; font-size: 32rpx; font-weight: 800; color: #1f2937; }
.empty-text { display: block; margin-top: 14rpx; color: #64748b; font-size: 25rpx; line-height: 1.6; }
</style>
