<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="hello">{{ member.token ? `您好，${member.realName || '业主'}` : '智慧物业' }}</text>
        <text class="house">{{ currentHouseText }}</text>
      </view>
      <button class="switch" @click="go(member.token ? '/pages/house/switch' : '/pages/house/bind')">
        {{ member.token ? '切换' : '绑定' }}
      </button>
    </view>

    <view v-if="!member.token" class="public-card">
      <text class="public-title">先浏览公共服务</text>
      <text class="public-copy">公告、报修入口、访客说明和物业服务指南可直接查看；绑定房屋后自动解锁账单、缴费、家庭成员和房屋通知。</text>
      <view class="public-actions">
        <button class="primary" @click="go('/pages/house/bind')">绑定我的房屋</button>
        <button class="secondary" @click="go('/pages/notice/list')">查看公告</button>
      </view>
    </view>

    <view class="metrics">
      <view class="metric"><text>{{ member.token ? summary.houseCount ?? 0 : '-' }}</text><label>房屋</label></view>
      <view class="metric hot"><text>{{ member.token ? summary.unpaidBillCount ?? 0 : '-' }}</text><label>待缴</label></view>
      <view class="metric"><text>{{ member.token ? summary.workOrderCount ?? 0 : '-' }}</text><label>工单</label></view>
      <view class="metric"><text>{{ member.token ? summary.vehicleCount ?? 0 : '-' }}</text><label>车辆</label></view>
    </view>

    <view class="quick-card">
      <view class="quick-copy">
        <text class="quick-title">物业服务</text>
        <text class="quick-sub">{{ member.token ? '报修、投诉、访客和缴费统一入口' : '公共信息可浏览，房屋服务需先绑定' }}</text>
      </view>
      <button class="quick-button" @click="openPrivate('/pages/workorder/create')">报修</button>
    </view>

    <view class="grid">
      <button v-for="item in entries" :key="item.url" class="entry" @click="openEntry(item)">
        <text class="entry-icon">{{ item.icon }}</text>
        <text class="entry-title">{{ item.title }}</text>
        <text class="entry-sub">{{ item.sub }}</text>
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchHome } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const summary = reactive<Record<string, unknown>>({})
const entries = [
  { title: '通知公告', sub: '社区消息与公共通知', icon: '告', url: '/pages/notice/list', public: true },
  { title: '房屋绑定', sub: '绑定本人或家属房屋', icon: '房', url: '/pages/house/bind', public: true },
  { title: '物业缴费', sub: '账单与收银台', icon: '缴', url: '/pages/bill/list' },
  { title: '报事报修', sub: '提交维修工单', icon: '修', url: '/pages/workorder/create' },
  { title: '工单进度', sub: '处理与评价', icon: '单', url: '/pages/workorder/list' },
  { title: '投诉建议', sub: '服务反馈', icon: '诉', url: '/pages/workorder/complaint' },
  { title: '访客邀请', sub: '门禁通行', icon: '访', url: '/pages/visitor/create' },
  { title: '我的车辆', sub: '车牌月租', icon: '车', url: '/pages/vehicle/list' },
  { title: '我的租赁', sub: '合同到期', icon: '租', url: '/pages/lease/contracts' },
  { title: '个人中心', sub: '房屋、家属和消息', icon: '我', url: '/pages/mine/index', public: true },
]

const currentHouseText = computed(() => {
  if (!member.token) return '公共服务入口'
  return member.currentHouseNo || '请绑定或选择房屋'
})

onShow(async () => {
  if (!member.token) {
    Object.keys(summary).forEach((key) => delete summary[key])
    return
  }
  try {
    Object.assign(summary, await fetchHome())
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '首页加载失败', icon: 'none' })
  }
})

function go(url: string) {
  uni.navigateTo({ url })
}

function openEntry(item: { url: string, public?: boolean }) {
  if (item.public) {
    go(item.url)
    return
  }
  openPrivate(item.url)
}

function openPrivate(url: string) {
  if (!member.token) {
    uni.showToast({ title: '请先绑定房屋', icon: 'none' })
    go('/pages/house/bind')
    return
  }
  if (!member.currentHouseId && url !== '/pages/house/list') {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    go('/pages/house/list')
    return
  }
  go(url)
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
.public-card { margin: 22rpx 0; padding: 28rpx; background: #fff; border-radius: 22rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.public-title { display: block; color: #102a43; font-size: 32rpx; font-weight: 900; }
.public-copy { display: block; margin-top: 12rpx; color: #64748b; font-size: 25rpx; line-height: 1.65; }
.public-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.public-actions button { flex: 1; height: 72rpx; border-radius: 36rpx; font-size: 26rpx; font-weight: 800; }
.primary { color: #fff; background: #0f766e; }
.secondary { color: #334155; background: #e8f0f2; }
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
.quick-copy { flex: 1; min-width: 0; padding-right: 18rpx; }
.quick-title { display: block; font-size: 32rpx; font-weight: 800; }
.quick-sub { display: block; margin-top: 8rpx; color: #bfdbfe; font-size: 24rpx; line-height: 1.45; }
.quick-button { width: 132rpx; height: 68rpx; border-radius: 34rpx; background: #f59e0b; color: #111827; font-weight: 800; font-size: 26rpx; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 18rpx; }
.entry { min-height: 150rpx; padding: 22rpx; background: #fff; border-radius: 20rpx; color: #1f2937; text-align: left; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.entry-icon { display: inline-flex; align-items: center; justify-content: center; width: 48rpx; height: 48rpx; margin-bottom: 16rpx; border-radius: 16rpx; background: #e0f2fe; color: #0369a1; font-size: 24rpx; font-weight: 800; text-align: center; }
.entry-title { display: block; font-size: 29rpx; font-weight: 800; }
.entry-sub { display: block; margin-top: 8rpx; color: #64748b; font-size: 23rpx; line-height: 1.35; }
</style>
