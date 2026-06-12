<template>
  <view class="page">
    <view v-for="item in records" :key="String(item.vehicleId)" class="card">
      <text class="title">{{ item.plateNo }}</text>
      <text class="meta">{{ item.vehicleType }} ｜ 月租：{{ item.monthlyRentStatus }} ｜ {{ item.status }}</text>
    </view>
    <view v-if="!records.length" class="empty">暂无车辆</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchVehicles } from '@/api/app'

const records = ref<Record<string, unknown>[]>([])
onShow(async () => {
  records.value = (await fetchVehicles({ pageNo: 1, pageSize: 50 })).records
})
</script>

<style scoped>
.page { padding: 24rpx; }
.card { margin-bottom: 18rpx; padding: 28rpx; background: #fff; border-radius: 14rpx; }
.title { display: block; font-size: 32rpx; font-weight: 800; }
.meta { display: block; margin-top: 12rpx; color: #64748b; font-size: 24rpx; }
.empty { padding: 80rpx 0; text-align: center; color: #94a3b8; }
</style>
