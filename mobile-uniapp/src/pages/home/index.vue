<template>
  <view class="page">
    <view class="hero">
      <text class="hello">您好，{{ member.realName || '业主' }}</text>
      <text class="house">{{ member.currentHouseNo || '请选择房屋' }}</text>
      <button class="switch" @click="go('/pages/house/switch')">切换房屋</button>
    </view>
    <view class="metrics">
      <view class="metric"><text>{{ summary.houseCount ?? 0 }}</text><label>房屋</label></view>
      <view class="metric"><text>{{ summary.unpaidBillCount ?? 0 }}</text><label>待缴</label></view>
      <view class="metric"><text>{{ summary.workOrderCount ?? 0 }}</text><label>工单</label></view>
      <view class="metric"><text>{{ summary.vehicleCount ?? 0 }}</text><label>车辆</label></view>
    </view>
    <view class="grid">
      <button @click="go('/pages/bill/list')">物业缴费</button>
      <button @click="go('/pages/workorder/create')">报事报修</button>
      <button @click="go('/pages/workorder/complaint')">投诉建议</button>
      <button @click="go('/pages/notice/list')">通知公告</button>
      <button @click="go('/pages/visitor/create')">访客邀请</button>
      <button @click="go('/pages/vehicle/list')">我的车辆</button>
      <button @click="go('/pages/lease/contracts')">我的租赁</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchHome } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const summary = reactive<Record<string, unknown>>({})

onShow(async () => {
  if (!member.token) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  Object.assign(summary, await fetchHome())
})

function go(url: string) {
  uni.navigateTo({ url })
}
</script>

<style scoped>
.page { padding: 28rpx; }
.hero { position: relative; padding: 36rpx; background: #0f766e; border-radius: 18rpx; color: #fff; }
.hello { display: block; font-size: 36rpx; font-weight: 700; }
.house { display: block; margin-top: 16rpx; color: #d9f99d; }
.switch { position: absolute; right: 24rpx; bottom: 24rpx; width: 180rpx; height: 64rpx; color: #0f766e; background: #fff; font-size: 24rpx; }
.metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14rpx; margin: 24rpx 0; }
.metric { padding: 22rpx 8rpx; text-align: center; background: #fff; border-radius: 14rpx; }
.metric text { display: block; font-size: 34rpx; font-weight: 800; color: #0f766e; }
.metric label { color: #64748b; font-size: 24rpx; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 18rpx; }
.grid button { height: 112rpx; background: #fff; border-radius: 14rpx; color: #1f2937; font-size: 28rpx; }
</style>
