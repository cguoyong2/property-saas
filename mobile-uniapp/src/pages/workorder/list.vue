<template>
  <view class="page">
    <view class="toolbar">
      <button class="primary" @click="goCreate">新增报修</button>
      <button class="ghost" @click="goComplaint">投诉建议</button>
    </view>
    <view v-for="item in records" :key="String(item.workOrderId)" class="card" @click="detail(item)">
      <text class="title">{{ item.title }}</text>
      <text class="meta">{{ item.orderNo }} ｜ {{ item.orderType }} ｜ {{ item.status }}</text>
      <text class="desc">{{ item.description }}</text>
    </view>
    <view v-if="!records.length" class="empty">暂无工单</view>
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
.page { padding: 24rpx; }
.toolbar { display: flex; gap: 16rpx; margin-bottom: 20rpx; }
.toolbar button { flex: 1; }
.primary { background: #0f766e; color: #fff; }
.ghost { background: #e2e8f0; color: #334155; }
.card { margin-bottom: 18rpx; padding: 28rpx; background: #fff; border-radius: 14rpx; }
.title { display: block; font-size: 30rpx; font-weight: 700; }
.meta, .desc { display: block; margin-top: 12rpx; color: #64748b; font-size: 24rpx; }
.empty { padding: 80rpx 0; text-align: center; color: #94a3b8; }
</style>
