<template>
  <view class="page">
    <view v-for="item in houses" :key="String(item.bindId)" class="card" @click="selectHouse(item)">
      <text class="title">{{ item.projectName }} {{ item.houseNo }}</text>
      <text class="meta">{{ item.bindRole }} ｜ {{ item.status }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchHouses } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const houses = ref<Record<string, unknown>[]>([])

onShow(async () => {
  houses.value = (await fetchHouses()).records.filter((item) => item.status === 'APPROVED')
})

function selectHouse(item: Record<string, unknown>) {
  member.setCurrentHouse({
    tenantId: Number(member.currentTenantId),
    projectId: Number(item.projectId),
    houseId: Number(item.houseId),
    houseNo: String(item.houseNo ?? ''),
    bindRole: String(item.bindRole ?? ''),
  })
  uni.switchTab({ url: '/pages/home/index' })
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { margin-bottom: 20rpx; padding: 30rpx; background: #fff; border-radius: 14rpx; }
.title { display: block; font-size: 32rpx; font-weight: 700; }
.meta { display: block; margin-top: 12rpx; color: #64748b; font-size: 24rpx; }
</style>
