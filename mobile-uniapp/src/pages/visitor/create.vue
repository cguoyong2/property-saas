<template>
  <view class="page">
    <view class="panel">
      <input v-model="form.visitorName" class="input" placeholder="访客姓名" />
      <input v-model="form.visitorMobile" class="input" placeholder="访客手机号" />
      <input v-model="form.visitReason" class="input" placeholder="来访事由" />
      <input v-model="form.validStartAt" class="input" placeholder="开始时间 2026-06-10T09:00:00" />
      <input v-model="form.validEndAt" class="input" placeholder="结束时间 2026-06-10T18:00:00" />
      <button class="primary" :loading="loading" @click="submit">生成访客邀请</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createVisitor } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const loading = ref(false)
const form = reactive({
  visitorName: '',
  visitorMobile: '',
  visitReason: '',
  validStartAt: '',
  validEndAt: '',
})

async function submit() {
  loading.value = true
  try {
    await createVisitor({ projectId: member.currentProjectId, inviterMemberId: member.memberId, ...form, deviceIds: [] })
    uni.showToast({ title: '已创建' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '创建失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.panel { padding: 28rpx; background: #fff; border-radius: 14rpx; }
.input { height: 84rpx; margin-bottom: 18rpx; padding: 0 22rpx; background: #f8fafc; border-radius: 10rpx; }
.primary { background: #0f766e; color: #fff; }
</style>
