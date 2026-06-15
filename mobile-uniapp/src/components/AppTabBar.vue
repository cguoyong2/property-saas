<template>
  <view class="app-tabbar">
    <view
      v-for="item in tabs"
      :key="item.key"
      class="tab"
      :class="{ active: active === item.key }"
      @click="go(item.url)"
    >
      <text class="tab-text">{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  active: 'home' | 'service' | 'message' | 'mine'
}>()

const tabs = [
  { key: 'home', label: '首页', icon: '', url: '/pages/home/index' },
  { key: 'service', label: '服务', icon: '', url: '/pages/service/index' },
  { key: 'message', label: '消息', icon: '', url: '/pages/notice/list' },
  { key: 'mine', label: '我的', icon: '', url: '/pages/mine/index' },
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
  height: 58px;
  margin-top: 13px;
  padding: 0 5px;
  background: rgba(255, 255, 255, .96);
  border: 0.5px solid #e2ece9;
  border-radius: 20px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, .06);
}

.tab {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-width: 0;
  min-height: 45px;
  padding: 0;
  color: #8795a6;
  background: transparent;
  border: 0;
  font-size: 10px;
  font-weight: 800;
}

.tab.active {
  color: #0f766e;
}

.tab-text {
  display: block;
  line-height: 1;
  white-space: nowrap;
}
</style>
