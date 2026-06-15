<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">我的</text>
        <text class="page-sub">家庭与账户</text>
      </view>
      <text class="pill">{{ member.token ? '已授权' : '未绑定' }}</text>
    </view>

    <view class="profile-card">
      <view class="avatar-row">
        <text class="avatar">{{ avatarText }}</text>
        <view class="avatar-copy">
          <text class="name">{{ mine.realName || member.realName || '访客用户' }}</text>
          <text class="house">{{ member.currentHouseNo || (member.token ? '未选房屋' : '绑定房屋后接收专属通知和账单') }}</text>
        </view>
        <button class="switch" @click="go(member.token ? '/pages/house/switch' : '/pages/house/bind')">
          {{ member.token ? '切换' : '绑定' }}
        </button>
      </view>
      <view class="family-strip">
        <text>房屋 {{ member.token ? '2' : '-' }}</text>
        <text>家属 {{ member.token ? '3' : '-' }}</text>
        <text>车辆 {{ member.token ? '1' : '-' }}</text>
      </view>
    </view>

    <view v-if="!member.token" class="bind-card">
      <text class="bind-title">绑定房屋后开启完整服务</text>
      <text class="bind-copy">可查看本人房屋账单、缴费记录、工单进度、家属房屋关系和物业通知。</text>
      <button @click="go('/pages/house/bind')">绑定房屋</button>
    </view>

    <view class="section">
      <view class="section-head">
        <text>我的关系</text>
        <text>可管理</text>
      </view>
      <view class="mini-stack">
        <button class="wide-card" @click="goPrivate('/pages/house/list')">
          <text class="wide-title">我的房屋</text>
          <text class="wide-copy">绑定、切换、解绑本人或家属房屋。</text>
        </button>
        <button class="wide-card" @click="goPrivate('/pages/mine/index')">
          <text class="wide-title">家属管理</text>
          <text class="wide-copy">添加配偶、父母、子女等同住人，授权接收通知和办理服务。</text>
        </button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>我的服务</text>
        <text>全部</text>
      </view>
      <view class="row-list">
        <button v-for="item in menuItems" :key="item.url" class="row" @click="goPrivate(item.url)">
          <view>
            <text class="row-title">{{ item.title }}</text>
            <text class="row-sub">{{ item.sub }}</text>
          </view>
          <text class="arrow">›</text>
        </button>
      </view>
    </view>

    <button v-if="member.token" class="logout" @click="logout">退出当前身份</button>
    <AppTabBar active="mine" />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '@/components/AppTabBar.vue'
import { fetchMine } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const mine = reactive<Record<string, unknown>>({})
const avatarText = computed(() => String(mine.realName || member.realName || '访').slice(0, 1))
const menuItems = [
  { title: '我的账单', sub: '待缴费用和缴费记录', url: '/pages/bill/list' },
  { title: '我的工单', sub: '报修、投诉、评价记录', url: '/pages/workorder/list' },
  { title: '我的车辆', sub: '车牌、车位、月租信息', url: '/pages/vehicle/list' },
  { title: '我的租赁', sub: '合同和到期提醒', url: '/pages/lease/contracts' },
  { title: '联系物业', sub: '客服电话、管家、服务时间', url: '/pages/notice/list' },
]

onShow(async () => {
  if (!member.token) {
    Object.keys(mine).forEach((key) => delete mine[key])
    return
  }
  try {
    Object.assign(mine, await fetchMine())
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' })
  }
})

function go(url: string) {
  uni.navigateTo({ url })
}

function goPrivate(url: string) {
  if (!member.token) {
    uni.showToast({ title: '请先绑定房屋', icon: 'none' })
    uni.navigateTo({ url: '/pages/house/bind' })
    return
  }
  uni.navigateTo({ url })
}

function logout() {
  member.clearSession()
  uni.switchTab({ url: '/pages/home/index' })
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

.profile-card {
  padding: 36rpx;
  color: #fff;
  background: linear-gradient(135deg, #0f766e 0%, #164e63 100%);
  border-radius: 36rpx;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 92rpx;
  height: 92rpx;
  color: #0b5f59;
  background: #ecfeff;
  border-radius: 28rpx;
  font-size: 34rpx;
  font-weight: 900;
}

.avatar-copy {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 34rpx;
  font-weight: 900;
}

.house {
  display: block;
  margin-top: 10rpx;
  overflow: hidden;
  color: #cdfcf1;
  font-size: 23rpx;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.switch {
  flex: none;
  height: 62rpx;
  padding: 0 22rpx;
  color: #0b5f59;
  background: #fff;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 900;
}

.family-strip {
  display: flex;
  gap: 16rpx;
  margin-top: 26rpx;
}

.family-strip text {
  flex: 1;
  padding: 20rpx 8rpx;
  color: #d7f7ef;
  background: rgba(255, 255, 255, .13);
  border: 1rpx solid rgba(255, 255, 255, .16);
  border-radius: 22rpx;
  text-align: center;
  font-size: 22rpx;
  font-weight: 900;
}

.bind-card,
.wide-card,
.row-list {
  background: rgba(255, 255, 255, .96);
  border: 1rpx solid #dfe9e6;
  border-radius: 28rpx;
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, .055);
}

.bind-card {
  margin-top: 24rpx;
  padding: 30rpx;
}

.bind-title {
  display: block;
  color: #172033;
  font-size: 31rpx;
  font-weight: 900;
}

.bind-copy {
  display: block;
  margin: 12rpx 0 24rpx;
  color: #65758a;
  font-size: 24rpx;
  line-height: 1.65;
}

.bind-card button {
  width: 176rpx;
  height: 68rpx;
  color: #fff;
  background: #0f766e;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 900;
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

.mini-stack {
  display: grid;
  gap: 18rpx;
}

.wide-card {
  width: 100%;
  padding: 28rpx;
  text-align: left;
}

.wide-title {
  display: block;
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
}

.wide-copy {
  display: block;
  margin-top: 10rpx;
  color: #65758a;
  font-size: 23rpx;
  line-height: 1.5;
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
}

.logout {
  width: 100%;
  height: 76rpx;
  margin-top: 28rpx;
  color: #991b1b;
  background: #fee2e2;
  border-radius: 999rpx;
  font-size: 27rpx;
  font-weight: 900;
}
</style>
