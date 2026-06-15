<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="title">服务中心</text>
        <text class="subtitle">按场景组织物业服务</text>
      </view>
      <text class="pill">清江花园</text>
    </view>

    <view class="intro-card">
      <text class="intro-title">清江花园服务大厅</text>
      <text class="intro-copy">把高频事项按房屋、缴费、物业、通行、公共服务分组，减少寻找成本。</text>
    </view>

    <view class="section">
      <view class="section-head">
        <text>房屋服务</text>
        <text>关系与资料</text>
      </view>
      <view class="row-list">
        <button class="row" @click="openPublic('/pages/house/list')">
          <view>
            <text class="row-title">我的房屋</text>
            <text class="row-sub">绑定、切换、解绑房屋</text>
          </view>
          <text class="arrow">›</text>
        </button>
        <button class="row" @click="openPrivate('/pages/mine/index')">
          <view>
            <text class="row-title">家属管理</text>
            <text class="row-sub">添加家属、租户、同住人</text>
          </view>
          <text class="arrow">›</text>
        </button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>物业服务</text>
        <text>工单与反馈</text>
      </view>
      <view class="service-grid">
        <button v-for="item in propertyServices" :key="item.url" class="service" @click="openPrivate(item.url)">
          <text class="service-icon">{{ item.icon }}</text>
          <text class="service-title">{{ item.title }}</text>
          <text class="service-sub">{{ item.sub }}</text>
        </button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>缴费与通行</text>
        <text>常用</text>
      </view>
      <view class="row-list">
        <button class="row" @click="openPrivate('/pages/bill/list')">
          <view>
            <text class="row-title">物业缴费</text>
            <text class="row-sub">待缴账单、历史账单、收银台</text>
          </view>
          <text class="badge">缴费</text>
        </button>
        <button class="row" @click="openPrivate('/pages/visitor/create')">
          <view>
            <text class="row-title">访客邀请</text>
            <text class="row-sub">生成访客通行凭证</text>
          </view>
          <text class="arrow">›</text>
        </button>
        <button class="row" @click="openPrivate('/pages/vehicle/list')">
          <view>
            <text class="row-title">车辆车位</text>
            <text class="row-sub">车牌、月租、车位信息</text>
          </view>
          <text class="arrow">›</text>
        </button>
      </view>
    </view>

    <AppTabBar active="service" />
  </view>
</template>

<script setup lang="ts">
import AppTabBar from '@/components/AppTabBar.vue'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const propertyServices = [
  { title: '报事报修', sub: '维修提交', icon: '修', url: '/pages/workorder/create' },
  { title: '投诉建议', sub: '意见反馈', icon: '诉', url: '/pages/workorder/complaint' },
  { title: '工单进度', sub: '处理评价', icon: '单', url: '/pages/workorder/list' },
  { title: '通知公告', sub: '社区消息', icon: '告', url: '/pages/notice/list' },
]

function openPublic(url: string) {
  uni.navigateTo({ url })
}

function openPrivate(url: string) {
  if (!member.token) {
    uni.showToast({ title: '请先绑定房屋', icon: 'none' })
    uni.navigateTo({ url: '/pages/house/bind' })
    return
  }
  if (!member.currentHouseId && url !== '/pages/house/list') {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    uni.navigateTo({ url: '/pages/house/list' })
    return
  }
  uni.navigateTo({ url })
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

.screen-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 28rpx;
}

.title {
  display: block;
  color: #172033;
  font-size: 38rpx;
  font-weight: 900;
}

.subtitle {
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

.intro-card,
.row-list {
  background: rgba(255, 255, 255, .96);
  border: 1rpx solid #dfe9e6;
  border-radius: 28rpx;
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, .055);
}

.intro-card {
  padding: 30rpx;
}

.intro-title {
  display: block;
  color: #172033;
  font-size: 32rpx;
  font-weight: 900;
}

.intro-copy {
  display: block;
  margin-top: 12rpx;
  color: #65758a;
  font-size: 24rpx;
  line-height: 1.6;
}

.section {
  margin-top: 28rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  margin: 0 4rpx 18rpx;
}

.section-head text:first-child {
  color: #172033;
  font-size: 31rpx;
  font-weight: 900;
}

.section-head text:last-child {
  color: #65758a;
  font-size: 23rpx;
  font-weight: 800;
}

.row-list {
  padding: 8rpx 28rpx;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 116rpx;
  padding: 0;
  background: transparent;
  border-bottom: 1rpx solid #eef2f3;
  text-align: left;
}

.row:last-child {
  border-bottom: 0;
}

.row-title {
  display: block;
  color: #172033;
  font-size: 28rpx;
  font-weight: 900;
}

.row-sub {
  display: block;
  margin-top: 8rpx;
  color: #65758a;
  font-size: 22rpx;
}

.arrow {
  color: #94a3b8;
  font-size: 42rpx;
  line-height: 1;
}

.badge {
  padding: 8rpx 16rpx;
  color: #9a5b00;
  background: #fff4dd;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx;
}

.service {
  min-height: 142rpx;
  padding: 14rpx 8rpx;
  background: #fff;
  border: 1rpx solid #e2ece9;
  border-radius: 24rpx;
  text-align: center;
}

.service-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62rpx;
  height: 62rpx;
  margin: 0 auto 10rpx;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 22rpx;
  font-size: 23rpx;
  font-weight: 900;
}

.service:nth-child(2n) .service-icon {
  color: #1d4ed8;
  background: #e8f0ff;
}

.service:nth-child(3n) .service-icon {
  color: #b45309;
  background: #fff4dd;
}

.service-title {
  display: block;
  color: #172033;
  font-size: 23rpx;
  font-weight: 900;
}

.service-sub {
  display: block;
  margin-top: 6rpx;
  color: #65758a;
  font-size: 19rpx;
}
</style>
