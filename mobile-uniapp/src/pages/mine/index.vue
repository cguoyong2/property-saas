<template>
  <view class="page">
    <view class="profile">
      <text class="name">{{ mine.realName || member.realName || '业主用户' }}</text>
      <text class="mobile">{{ mine.mobile || member.mobile }}</text>
    </view>
    <view class="menu">
      <button @click="go('/pages/house/list')">我的房屋</button>
      <button @click="go('/pages/vehicle/list')">我的车辆</button>
      <button @click="go('/pages/lease/contracts')">我的租赁</button>
      <button @click="logout">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchMine } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const mine = reactive<Record<string, unknown>>({})

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
.page { padding: 24rpx; }
.profile { padding: 36rpx; background: #0f766e; border-radius: 16rpx; color: #fff; }
.name { display: block; font-size: 38rpx; font-weight: 800; }
.mobile { display: block; margin-top: 12rpx; color: #ccfbf1; }
.menu { margin-top: 24rpx; }
.menu button { margin-bottom: 16rpx; background: #fff; color: #1f2937; text-align: left; }
</style>
