<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">{{ member.token ? '已绑定首页' : '游客首页' }}</text>
        <text class="page-sub">{{ member.token ? '业主状态' : '公共入口' }}</text>
      </view>
      <text class="pill">{{ member.token ? '业主服务' : '公共服务' }}</text>
    </view>

    <view class="hero">
      <text class="hero-title">{{ member.token ? projectName : '智慧物业' }}</text>
      <text class="hero-copy">{{ heroCopy }}</text>
      <view class="hero-row">
        <text class="hero-meta">{{ member.token ? member.currentHouseNo || '请绑定或选择房屋' : '公共服务入口' }}</text>
        <button class="hero-button" @click="go(member.token ? '/pages/house/switch' : '/pages/house/bind')">
          {{ member.token ? '切换' : '绑定房屋' }}
        </button>
      </view>
    </view>

    <view v-if="member.token" class="metric-grid">
      <view class="metric"><text>{{ summary.houseCount ?? 0 }}</text><label>绑定房屋</label></view>
      <view class="metric hot"><text>¥{{ summary.unpaidAmount ?? summary.unpaidBillAmount ?? summary.unpaidBillCount ?? 0 }}</text><label>待缴</label></view>
      <view class="metric"><text>{{ summary.workOrderCount ?? 0 }}</text><label>工单</label></view>
    </view>

    <view v-else class="public-card">
      <text class="public-title">绑定后可使用完整业主服务</text>
      <text class="public-copy">支持业主、家属、租户、住户提交绑定审核，审核通过后可接收对应房屋的通知、账单和工单消息。</text>
      <view class="public-actions">
        <button class="primary" @click="go('/pages/house/bind')">立即绑定</button>
        <button class="secondary" @click="go('/pages/notice/list')">查看公告</button>
      </view>
    </view>

    <view v-if="member.token" class="section">
      <view class="section-head">
        <text>重点待办</text>
        <text>本周</text>
      </view>
      <view class="card notice-list">
        <button class="notice-line" @click="go('/pages/bill/list')">
          <text class="badge orange">待缴</text>
          <view>
            <text class="line-title">本月物业费待支付</text>
            <text class="line-sub">如有待缴账单，请及时处理</text>
          </view>
          <text class="pay-link">支付</text>
        </button>
        <button class="notice-line" @click="go('/pages/workorder/list')">
          <text class="badge blue">工单</text>
          <view>
            <text class="line-title">查看报修处理进度</text>
            <text class="line-sub">维修、投诉、评价统一查询</text>
          </view>
        </button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>{{ member.token ? '快捷服务' : '常用服务' }}</text>
        <text>全部</text>
      </view>
      <view class="service-grid">
        <button v-for="item in entries" :key="item.url" class="service" @click="openEntry(item)">
          <text class="service-icon">{{ item.icon }}</text>
          <text class="service-title">{{ item.title }}</text>
          <text class="service-sub">{{ item.sub }}</text>
        </button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>{{ member.token ? '最新消息' : '社区动态' }}</text>
        <text @click="go('/pages/notice/list')">更多</text>
      </view>
      <view class="card notice-list">
        <view class="notice-line">
          <text class="badge green">公告</text>
          <view>
            <text class="line-title">端午节物业服务安排</text>
            <text class="line-sub">客服、保洁、秩序值班时间说明</text>
          </view>
        </view>
        <view class="notice-line">
          <text class="badge orange">提醒</text>
          <view>
            <text class="line-title">地下车库清洗通知</text>
            <text class="line-sub">请车主提前移车，避免影响施工</text>
          </view>
        </view>
      </view>
    </view>

    <AppTabBar active="home" />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '@/components/AppTabBar.vue'
import { fetchHome } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const summary = reactive<Record<string, unknown>>({})
const projectName = computed(() => member.currentHouseNo ? '清江花园' : '智慧物业')
const heroCopy = computed(() => {
  if (!member.token) return '先浏览公共公告与服务说明，绑定房屋后开启专属缴费、报修和通知。'
  return `${member.currentHouseNo || '当前未选择房屋'}，欢迎回来。查看待缴、工单和物业通知。`
})
const entries = [
  { title: '通知公告', sub: '公共消息', icon: '告', url: '/pages/notice/list', public: true },
  { title: '房屋绑定', sub: '提交审核', icon: '房', url: '/pages/house/bind', public: true },
  { title: '物业缴费', sub: member.token ? '账单支付' : '需绑定', icon: '缴', url: '/pages/bill/list' },
  { title: '报事报修', sub: member.token ? '一键提交' : '需绑定', icon: '修', url: '/pages/workorder/create' },
  { title: '投诉建议', sub: '服务反馈', icon: '诉', url: '/pages/workorder/complaint' },
  { title: '访客通行', sub: '临时邀约', icon: '访', url: '/pages/visitor/create' },
  { title: '车辆管理', sub: '车位月租', icon: '车', url: '/pages/vehicle/list' },
  { title: '个人中心', sub: '我的资料', icon: '我', url: '/pages/mine/index', public: true },
]

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
  if (!member.token) {
    uni.showToast({ title: '请先绑定房屋', icon: 'none' })
    go('/pages/house/bind')
    return
  }
  if (!member.currentHouseId) {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    go('/pages/house/list')
    return
  }
  go(item.url)
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
  padding: 12rpx 20rpx;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 900;
}

.hero {
  min-height: 292rpx;
  padding: 40rpx;
  color: #fff;
  background:
    radial-gradient(circle at 82% 12%, rgba(255, 255, 255, .24), transparent 24%),
    linear-gradient(135deg, #0f766e 0%, #124e61 100%);
  border-radius: 36rpx;
  box-sizing: border-box;
}

.hero-title {
  display: block;
  font-size: 50rpx;
  font-weight: 900;
  line-height: 1.25;
}

.hero-copy {
  display: block;
  margin-top: 18rpx;
  color: #cdfcf1;
  font-size: 25rpx;
  line-height: 1.55;
}

.hero-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-top: 30rpx;
}

.hero-meta {
  flex: 1;
  min-width: 0;
  height: 62rpx;
  padding: 0 20rpx;
  overflow: hidden;
  color: #ecfeff;
  background: rgba(255, 255, 255, .14);
  border: 1rpx solid rgba(255, 255, 255, .18);
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 62rpx;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.hero-button {
  flex: none;
  height: 70rpx;
  padding: 0 26rpx;
  color: #0b5f59;
  background: #fff;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 900;
}

.public-card,
.card {
  background: rgba(255, 255, 255, .96);
  border: 1rpx solid #dfe9e6;
  border-radius: 28rpx;
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, .055);
}

.public-card {
  margin-top: 24rpx;
  padding: 30rpx;
}

.public-title {
  display: block;
  color: #172033;
  font-size: 32rpx;
  font-weight: 900;
}

.public-copy {
  display: block;
  margin-top: 14rpx;
  color: #65758a;
  font-size: 24rpx;
  line-height: 1.6;
}

.public-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 26rpx;
}

.public-actions button {
  flex: 1;
  height: 72rpx;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 900;
}

.primary {
  color: #fff;
  background: #0f766e;
}

.secondary {
  color: #334155;
  background: #e8f0f2;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 22rpx;
}

.metric {
  padding: 24rpx 10rpx;
  background: rgba(255, 255, 255, .96);
  border: 1rpx solid #e2ece9;
  border-radius: 24rpx;
  text-align: center;
}

.metric text {
  display: block;
  color: #0b5f59;
  font-size: 36rpx;
  font-weight: 950;
}

.metric.hot text {
  color: #b45309;
}

.metric label {
  display: block;
  margin-top: 8rpx;
  color: #65758a;
  font-size: 22rpx;
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

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx;
}

.service {
  display: block;
  width: 100%;
  min-height: 150rpx;
  padding: 16rpx 8rpx;
  background: #fff;
  border: 1rpx solid #e2ece9;
  border-radius: 24rpx;
  text-align: center;
}

.service-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
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
  white-space: nowrap;
}

.service-sub {
  display: block;
  margin-top: 6rpx;
  color: #65758a;
  font-size: 19rpx;
  line-height: 1.25;
  white-space: nowrap;
}

.notice-list {
  padding: 8rpx 28rpx;
}

.notice-line {
  display: flex;
  align-items: center;
  gap: 18rpx;
  width: 100%;
  min-height: 110rpx;
  padding: 0;
  background: transparent;
  border-bottom: 1rpx solid #eef2f3;
  text-align: left;
}

.notice-line:last-child {
  border-bottom: 0;
}

.badge {
  flex: none;
  min-width: 76rpx;
  height: 44rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 900;
  line-height: 44rpx;
  text-align: center;
}

.badge.green {
  color: #0b5f59;
  background: #dff5ef;
}

.badge.orange {
  color: #9a5b00;
  background: #fff4dd;
}

.badge.blue {
  color: #1d4ed8;
  background: #e8f0ff;
}

.line-title {
  display: block;
  color: #172033;
  font-size: 27rpx;
  font-weight: 900;
}

.line-sub {
  display: block;
  margin-top: 7rpx;
  color: #65758a;
  font-size: 22rpx;
}

.pay-link {
  margin-left: auto;
  color: #dc2626;
  font-size: 28rpx;
  font-weight: 950;
}
</style>
