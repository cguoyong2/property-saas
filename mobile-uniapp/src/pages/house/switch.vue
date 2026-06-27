<template>
  <view class="page">
    <view v-for="item in houses" :key="String(item.bindId)" class="card" @click="selectHouse(item)">
      <text class="title">{{ item.projectName }} {{ roomText(item) }}</text>
      <text class="meta">{{ roleText(item.bindRole) }} ｜ {{ statusText(item.status) }}</text>
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
const statusLabels: Record<string, string> = { APPROVED: '已通过' }
const roleLabels: Record<string, string> = {
  OWNER: '业主',
  FAMILY: '家属',
  TENANT: '租户',
  RESIDENT: '住户',
}

onShow(async () => {
  houses.value = (await fetchHouses()).records.filter((item) => item.status === 'APPROVED')
})

function selectHouse(item: Record<string, unknown>) {
  member.setCurrentHouse({
    tenantId: Number(member.currentTenantId),
    projectId: Number(item.projectId),
    houseId: Number(item.houseId),
    houseNo: roomText(item),
    bindRole: String(item.bindRole ?? ''),
  })
  uni.switchTab({ url: '/pages/home/index' })
}

function roomText(item: Record<string, unknown>) {
  return String(item.roomNo || `${item.buildingName || ''}${item.unitName || ''}${item.houseNo || ''}` || '-')
}

function statusText(value: unknown) {
  return statusLabels[String(value)] ?? String(value || '-')
}

function roleText(value: unknown) {
  return roleLabels[String(value)] ?? String(value || '-')
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { margin-bottom: 20rpx; padding: 30rpx; background: #fff; border-radius: 14rpx; }
.title { display: block; font-size: 32rpx; font-weight: 700; }
.meta { display: block; margin-top: 12rpx; color: #64748b; font-size: 24rpx; }
</style>
