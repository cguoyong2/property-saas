<template>
  <view class="page">
    <view v-for="item in records" :key="String(item.contractId)" class="card">
      <text class="title">{{ item.contractNo }}</text>
      <text class="meta">{{ item.lesseeName }} ｜ {{ item.startDate }} 至 {{ item.endDate }}</text>
      <text class="amount">租金：{{ item.rentAmount }} 元 ｜ {{ item.status }}</text>
    </view>
    <view v-if="!records.length" class="empty">暂无租赁合同</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchLeaseContracts } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const records = ref<Record<string, unknown>[]>([])

onShow(async () => {
  if (!member.currentProjectId) return
  records.value = (await fetchLeaseContracts({
    projectId: member.currentProjectId,
    mobile: member.mobile,
    pageNo: 1,
    pageSize: 50,
  })).records
})
</script>

<style scoped>
.page { padding: 24rpx; }
.card { margin-bottom: 18rpx; padding: 28rpx; background: #fff; border-radius: 14rpx; }
.title { display: block; font-size: 30rpx; font-weight: 700; }
.meta, .amount { display: block; margin-top: 12rpx; color: #64748b; font-size: 24rpx; }
.empty { padding: 80rpx 0; text-align: center; color: #94a3b8; }
</style>
