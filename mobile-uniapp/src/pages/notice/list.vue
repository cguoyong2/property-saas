<template>
  <view class="page">
    <view v-for="item in records" :key="String(item.noticeId)" class="card">
      <text class="title">{{ item.title }}</text>
      <text class="meta">{{ item.noticeType }} ｜ {{ item.publishedAt || item.createdAt }}</text>
      <text class="content">{{ item.content }}</text>
    </view>
    <view v-if="!records.length" class="empty">暂无公告</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchNotices } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const records = ref<Record<string, unknown>[]>([])

onShow(async () => {
  if (!member.currentProjectId) return
  records.value = (await fetchNotices({
    projectId: member.currentProjectId,
    memberId: member.memberId,
    pageNo: 1,
    pageSize: 50,
  })).records
})
</script>

<style scoped>
.page { padding: 24rpx; }
.card { margin-bottom: 18rpx; padding: 28rpx; background: #fff; border-radius: 14rpx; }
.title { display: block; font-size: 30rpx; font-weight: 700; }
.meta, .content { display: block; margin-top: 12rpx; color: #64748b; font-size: 24rpx; }
.empty { padding: 80rpx 0; text-align: center; color: #94a3b8; }
</style>
