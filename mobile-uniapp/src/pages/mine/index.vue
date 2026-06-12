<template>
  <view class="page">
    <view class="profile">
      <view>
        <text class="name">{{ mine.realName || member.realName || '业主用户' }}</text>
        <text class="mobile">{{ mine.mobile || member.mobile }}</text>
      </view>
      <text class="tag">{{ member.currentHouseNo || '未选房屋' }}</text>
    </view>
    <view class="menu">
      <button v-for="item in menuItems" :key="item.url" @click="go(item.url)">
        <text class="menu-title">{{ item.title }}</text>
        <text class="menu-sub">{{ item.sub }}</text>
      </button>
    </view>
    <button class="logout" @click="logout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchMine } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const mine = reactive<Record<string, unknown>>({})
const menuItems = [
  { title: '我的房屋', sub: '绑定、切换、解绑', url: '/pages/house/list' },
  { title: '我的账单', sub: '待缴费用和缴费记录', url: '/pages/bill/list' },
  { title: '我的工单', sub: '报修进度和评价', url: '/pages/workorder/list' },
  { title: '我的车辆', sub: '车牌和月租信息', url: '/pages/vehicle/list' },
  { title: '我的租赁', sub: '合同和到期提醒', url: '/pages/lease/contracts' },
  { title: '通知公告', sub: '社区和物业消息', url: '/pages/notice/list' },
]

onShow(async () => {
  Object.assign(mine, await fetchMine())
})

function go(url: string) {
  uni.navigateTo({ url })
}

function logout() {
  member.clearSession()
  uni.navigateTo({ url: '/pages/login/index' })
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #eef4f3; }
.profile { display: flex; justify-content: space-between; gap: 20rpx; align-items: flex-end; padding: 36rpx; background: #0f766e; border-radius: 22rpx; color: #fff; }
.name { display: block; font-size: 38rpx; font-weight: 900; }
.mobile { display: block; margin-top: 12rpx; color: #ccfbf1; font-size: 25rpx; }
.tag { flex: none; max-width: 240rpx; padding: 10rpx 18rpx; overflow: hidden; border-radius: 999rpx; background: rgba(255, 255, 255, .16); color: #fff; font-size: 23rpx; text-overflow: ellipsis; white-space: nowrap; }
.menu { margin-top: 24rpx; }
.menu button { display: block; width: 100%; min-height: 92rpx; margin-bottom: 16rpx; padding: 18rpx 24rpx; border-radius: 18rpx; background: #fff; color: #1f2937; text-align: left; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.menu-title { display: block; font-size: 29rpx; font-weight: 800; }
.menu-sub { display: block; margin-top: 6rpx; color: #64748b; font-size: 23rpx; }
.logout { width: 100%; height: 78rpx; margin-top: 10rpx; border-radius: 39rpx; background: #fee2e2; color: #991b1b; font-size: 27rpx; font-weight: 800; }
</style>
