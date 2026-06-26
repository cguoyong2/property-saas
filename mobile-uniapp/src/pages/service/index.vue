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
        <button class="row" @click="openPrivate('/pages/payment/history')">
          <view>
            <text class="row-title">缴费记录</text>
            <text class="row-sub">收款凭证、退款和支付记录</text>
          </view>
          <text class="arrow">›</text>
        </button>
        <button class="row" @click="openPrivate('/pages/payment/prepayment')">
          <view>
            <text class="row-title">预存款</text>
            <text class="row-sub">余额、超收转入和账单抵扣</text>
          </view>
          <text class="arrow">›</text>
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
  padding: 14px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% -2%, rgba(15, 118, 110, .18), transparent 35%),
    linear-gradient(180deg, #eef8f5 0%, #f8fafc 45%, #f3f6f8 100%);
}

.screen-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.title {
  display: block;
  color: #172033;
  font-size: 19px;
  font-weight: 900;
}

.subtitle {
  display: block;
  margin-top: 4px;
  color: #65758a;
  font-size: 12px;
}

.pill {
  flex: none;
  padding: 6px 10px;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 499.5px;
  font-size: 11.5px;
  font-weight: 900;
}

.intro-card,
.row-list {
  background: rgba(255, 255, 255, .96);
  border: 0.5px solid #dfe9e6;
  border-radius: 14px;
  box-shadow: 0 7px 17px rgba(15, 23, 42, .055);
}

.intro-card {
  padding: 15px;
}

.intro-title {
  display: block;
  color: #172033;
  font-size: 16px;
  font-weight: 900;
}

.intro-copy {
  display: block;
  margin-top: 6px;
  color: #65758a;
  font-size: 12px;
  line-height: 1.6;
}

.section {
  margin-top: 14px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  margin: 0 2px 9px;
}

.section-head text:first-child {
  color: #172033;
  font-size: 15.5px;
  font-weight: 900;
}

.section-head text:last-child {
  color: #65758a;
  font-size: 11.5px;
  font-weight: 800;
}

.row-list {
  padding: 4px 14px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 58px;
  padding: 0;
  background: transparent;
  border-bottom: 0.5px solid #eef2f3;
  text-align: left;
}

.row:last-child {
  border-bottom: 0;
}

.row-title {
  display: block;
  color: #172033;
  font-size: 14px;
  font-weight: 900;
}

.row-sub {
  display: block;
  margin-top: 4px;
  color: #65758a;
  font-size: 11px;
}

.arrow {
  color: #94a3b8;
  font-size: 21px;
  line-height: 1;
}

.badge {
  padding: 4px 8px;
  color: #9a5b00;
  background: #fff4dd;
  border-radius: 499.5px;
  font-size: 11px;
  font-weight: 900;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 7px;
}

.service {
  display: block;
  width: 100%;
  min-height: 71px;
  padding: 7px 4px;
  background: #fff;
  border: 0.5px solid #e2ece9;
  border-radius: 12px;
  text-align: center;
}

.service-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 31px;
  height: 31px;
  margin: 0 auto 5px;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 11px;
  font-size: 11.5px;
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
  font-size: 11.5px;
  font-weight: 900;
  white-space: nowrap;
}

.service-sub {
  display: block;
  margin-top: 3px;
  color: #65758a;
  font-size: 9.5px;
  line-height: 1.25;
  white-space: nowrap;
}
</style>
