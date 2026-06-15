<template>
  <view class="app-tabbar">
    <button
      v-for="item in tabs"
      :key="item.key"
      class="tab"
      :class="{ active: active === item.key }"
      @click="go(item.url)"
    >
      <text class="tab-icon">{{ item.icon }}</text>
      <text class="tab-text">{{ item.label }}</text>
    </button>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  active: 'home' | 'service' | 'message' | 'mine'
}>()

const tabs = [
  { key: 'home', label: '首页', icon: '首', url: '/pages/home/index' },
  { key: 'service', label: '服务', icon: '服', url: '/pages/service/index' },
  { key: 'message', label: '消息', icon: '信', url: '/pages/notice/list' },
  { key: 'mine', label: '我的', icon: '我', url: '/pages/mine/index' },
] as const

function go(url: string) {
  uni.switchTab({ url })
}
</script>

<style scoped>
.app-tabbar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-items: center;
  height: 116rpx;
  margin-top: 24rpx;
  padding: 8rpx 10rpx;
  background: rgba(255, 255, 255, .96);
  border: 1rpx solid #e2ece9;
  border-radius: 32rpx;
  box-shadow: 0 18rpx 42rpx rgba(15, 23, 42, .06);
}

.tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  width: 100%;
  min-width: 0;
  min-height: 88rpx;
  padding: 0;
  color: #8795a6;
  background: transparent;
  border: 0;
  font-size: 21rpx;
  font-weight: 800;
}

.tab.active {
  color: #0f766e;
}

.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38rpx;
  height: 38rpx;
  border-radius: 14rpx;
  background: #eef3f5;
  font-size: 20rpx;
  font-weight: 900;
}

.tab.active .tab-icon {
  color: #0b5f59;
  background: #dff5ef;
}

.tab-text {
  display: block;
  line-height: 1.1;
  white-space: nowrap;
}
</style>
