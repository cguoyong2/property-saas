<template>
  <view class="page auth-page">
    <view class="brand">
      <text class="brand-title">智慧物业</text>
      <text class="brand-sub">业主服务小程序</text>
    </view>
    <view class="panel">
      <input v-model="form.tenantId" class="input" type="number" placeholder="租户ID" />
      <input v-model="form.openid" class="input" placeholder="微信 openid" />
      <input v-model="form.mobile" class="input" placeholder="手机号" />
      <input v-model="form.realName" class="input" placeholder="姓名" />
      <button class="primary" :loading="loading" @click="submit">登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { wxLogin } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const loading = ref(false)
const isDev = ((import.meta as unknown as { env?: { DEV?: boolean | string } }).env?.DEV ?? false) === true
const isH5Preview = typeof window !== 'undefined' && Boolean((window as unknown as { __PROPERTY_SAAS_H5_PREVIEW__?: boolean }).__PROPERTY_SAAS_H5_PREVIEW__)
const form = reactive({
  tenantId: isDev || isH5Preview ? '1' : '',
  openid: isDev || isH5Preview ? 'demo-openid-001' : '',
  mobile: isDev || isH5Preview ? '13900000001' : '',
  realName: isDev || isH5Preview ? '业主用户' : '',
})

async function submit() {
  if (!form.tenantId || !form.openid) {
    uni.showToast({ title: '请填写租户ID和openid', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const session = await wxLogin({ ...form, tenantId: Number(form.tenantId) })
    member.setSession(session)
    uni.switchTab({ url: '/pages/home/index' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; padding: 80rpx 32rpx; background: #eef7f4; }
.brand { margin: 48rpx 0; }
.brand-title { display: block; font-size: 52rpx; font-weight: 800; color: #0f766e; }
.brand-sub { display: block; margin-top: 12rpx; color: #475569; }
.panel { padding: 32rpx; background: #fff; border-radius: 16rpx; box-shadow: 0 16rpx 40rpx rgba(15, 118, 110, .1); }
.input { height: 88rpx; margin-bottom: 20rpx; padding: 0 24rpx; background: #f8fafc; border-radius: 10rpx; font-size: 28rpx; }
.primary { height: 88rpx; margin-top: 16rpx; background: #0f766e; color: #fff; border-radius: 10rpx; font-size: 30rpx; }
</style>
