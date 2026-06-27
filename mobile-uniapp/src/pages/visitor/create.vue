<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">访客邀请</text>
      <text class="hero-sub">生成门禁通行邀请，访客到访时可用于物业核验</text>
    </view>

    <view class="panel">
      <view class="field">
        <text>访客姓名</text>
        <input v-model="form.visitorName" class="input" placeholder="请输入访客姓名" />
      </view>
      <view class="field">
        <text>访客手机号</text>
        <input v-model="form.visitorMobile" class="input" placeholder="请输入访客手机号" type="number" />
      </view>
      <view class="field">
        <text>来访事由</text>
        <input v-model="form.visitReason" class="input" placeholder="例如：亲友来访、装修上门" />
      </view>
      <view class="field">
        <text>开始时间</text>
        <input v-model="form.validStartAt" class="input" placeholder="2026-06-12T09:00:00" />
      </view>
      <view class="field">
        <text>结束时间</text>
        <input v-model="form.validEndAt" class="input" placeholder="2026-06-12T18:00:00" />
      </view>
      <button class="primary" :loading="loading" @click="submit">生成访客邀请</button>
    </view>

    <view v-if="invite" class="result">
      <text class="result-title">邀请已生成</text>
      <text class="result-code">VISITOR-{{ invite.visitorId || invite.inviteId || 'OK' }}</text>
      <text class="result-text">请将以上编号发送给访客或门岗，正式部署后可替换为二维码/小程序分享卡片。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createVisitor } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const loading = ref(false)
const invite = ref<Record<string, unknown> | null>(null)
const form = reactive({
  visitorName: '',
  visitorMobile: '',
  visitReason: '',
  validStartAt: '',
  validEndAt: '',
})

async function submit() {
  if (member.currentBindRole !== 'OWNER' && !member.currentAllowVisitor) {
    uni.showToast({ title: '当前房屋未授权访客通行', icon: 'none' })
    return
  }
  if (!member.currentProjectId || !member.memberId) {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    return
  }
  if (!form.visitorName.trim() || !form.visitorMobile.trim() || !form.validStartAt.trim() || !form.validEndAt.trim()) {
    uni.showToast({ title: '请填写访客和有效期', icon: 'none' })
    return
  }
  loading.value = true
  try {
    invite.value = await createVisitor({ projectId: member.currentProjectId, inviterMemberId: member.memberId, ...form, deviceIds: [] })
    uni.showToast({ title: '已创建' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '创建失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #eef4f3; }
.hero { margin-bottom: 22rpx; padding: 34rpx; border-radius: 22rpx; background: #0f766e; color: #fff; }
.hero-title { display: block; font-size: 40rpx; font-weight: 900; }
.hero-sub { display: block; margin-top: 12rpx; color: #ccfbf1; font-size: 25rpx; line-height: 1.55; }
.panel { padding: 28rpx; background: #fff; border-radius: 20rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.field { margin-bottom: 18rpx; }
.field text { display: block; margin-bottom: 10rpx; color: #334155; font-size: 24rpx; font-weight: 800; }
.input { height: 84rpx; padding: 0 22rpx; background: #f8fafc; border: 2rpx solid #dbe4ea; border-radius: 14rpx; box-sizing: border-box; }
.primary { width: 100%; height: 78rpx; margin-top: 8rpx; border-radius: 39rpx; background: #0f766e; color: #fff; font-size: 28rpx; font-weight: 800; }
.result { margin-top: 22rpx; padding: 30rpx; border-radius: 20rpx; background: #172554; color: #fff; }
.result-title { display: block; color: #bfdbfe; font-size: 24rpx; }
.result-code { display: block; margin: 14rpx 0; font-size: 36rpx; font-weight: 900; letter-spacing: 0; }
.result-text { display: block; color: #dbeafe; font-size: 24rpx; line-height: 1.6; }
</style>
