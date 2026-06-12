<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="hello">您好，{{ member.realName || '业主' }}</text>
        <text class="house">{{ member.currentHouseNo || '请选择房屋' }}</text>
      </view>
      <button class="switch" @click="go('/pages/house/switch')">切换</button>
    </view>
    <view class="metrics">
      <view class="metric"><text>{{ summary.houseCount ?? 0 }}</text><label>房屋</label></view>
      <view class="metric hot"><text>{{ summary.unpaidBillCount ?? 0 }}</text><label>待缴</label></view>
      <view class="metric"><text>{{ summary.workOrderCount ?? 0 }}</text><label>工单</label></view>
      <view class="metric"><text>{{ summary.vehicleCount ?? 0 }}</text><label>车辆</label></view>
    </view>

    <view class="quick-card">
      <view class="quick-copy">
        <text class="quick-title">物业服务</text>
        <text class="quick-sub">报修、投诉、访客和缴费统一入口</text>
      </view>
      <button class="quick-button" @click="go('/pages/workorder/create')">报修</button>
    </view>

    <view class="grid">
      <button v-for="item in entries" :key="item.url" class="entry" @click="go(item.url)">
        <text class="entry-icon">{{ item.icon }}</text>
        <text class="entry-title">{{ item.title }}</text>
        <text class="entry-sub">{{ item.sub }}</text>
      </button>
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
const entries = [
  { title: '物业缴费', sub: '账单与收银台', icon: '缴', url: '/pages/bill/list' },
  { title: '报事报修', sub: '提交维修工单', icon: '修', url: '/pages/workorder/create' },
  { title: '工单进度', sub: '处理与评价', icon: '单', url: '/pages/workorder/list' },
  { title: '投诉建议', sub: '服务反馈', icon: '诉', url: '/pages/workorder/complaint' },
  { title: '通知公告', sub: '社区消息', icon: '告', url: '/pages/notice/list' },
  { title: '访客邀请', sub: '门禁通行', icon: '访', url: '/pages/visitor/create' },
  { title: '我的车辆', sub: '车牌月租', icon: '车', url: '/pages/vehicle/list' },
  { title: '我的租赁', sub: '合同到期', icon: '租', url: '/pages/lease/contracts' },
]

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
.page { min-height: 100vh; padding: 28rpx; box-sizing: border-box; background: #eef4f3; }
.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  min-height: 190rpx;
  padding: 34rpx;
  background: #0f766e;
  border-radius: 24rpx;
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(15, 118, 110, .22);
}
.hello { display: block; font-size: 42rpx; font-weight: 800; line-height: 1.2; }
.house { display: block; margin-top: 18rpx; color: #ccfbf1; font-size: 28rpx; }
.switch { width: 132rpx; height: 64rpx; border-radius: 32rpx; color: #0f766e; background: #fff; font-size: 26rpx; font-weight: 700; }
.metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14rpx; margin: 24rpx 0; }
.metric { padding: 22rpx 8rpx; text-align: center; background: #fff; border-radius: 18rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.metric text { display: block; font-size: 34rpx; font-weight: 800; color: #0f766e; }
.metric.hot text { color: #b45309; }
.metric label { color: #64748b; font-size: 23rpx; }
.quick-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
  padding: 28rpx;
  background: #172554;
  border-radius: 22rpx;
  color: #fff;
}
.quick-title { display: block; font-size: 32rpx; font-weight: 800; }
.quick-sub { display: block; margin-top: 8rpx; color: #bfdbfe; font-size: 24rpx; }
.quick-button { width: 132rpx; height: 68rpx; border-radius: 34rpx; background: #f59e0b; color: #111827; font-weight: 800; font-size: 26rpx; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 18rpx; }
.entry { min-height: 150rpx; padding: 22rpx; background: #fff; border-radius: 20rpx; color: #1f2937; text-align: left; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.entry-icon { display: inline-flex; align-items: center; justify-content: center; width: 48rpx; height: 48rpx; margin-bottom: 16rpx; border-radius: 16rpx; background: #e0f2fe; color: #0369a1; font-size: 24rpx; font-weight: 800; text-align: center; }
.entry-title { display: block; font-size: 29rpx; font-weight: 800; }
.entry-sub { display: block; margin-top: 8rpx; color: #64748b; font-size: 23rpx; }
</style>
