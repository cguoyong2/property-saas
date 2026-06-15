<template>
  <view class="page">
    <view class="intro">
      <text class="intro-title">绑定房屋</text>
      <text class="intro-copy">本人、家属、租户都可以提交绑定审核。审核通过后即可接收对应房屋通知、查看账单并办理缴费和报修。</text>
    </view>
    <view class="panel">
      <input v-model="form.projectId" class="input" type="number" placeholder="项目ID" />
      <input v-model="form.houseId" class="input" type="number" placeholder="房屋ID" />
      <picker :range="roles" @change="form.bindRole = roles[Number($event.detail.value)]">
        <view class="input">绑定角色：{{ form.bindRole }}</view>
      </picker>
      <input v-model="form.realName" class="input" placeholder="姓名" />
      <input v-model="form.mobile" class="input" placeholder="手机号" />
      <button class="primary" :loading="loading" @click="submit">提交审核</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { applyHouseBinding, wxLogin } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const roles = ['OWNER', 'FAMILY', 'TENANT', 'RESIDENT']
const loading = ref(false)
const form = reactive({
  projectId: '',
  houseId: '',
  bindRole: 'OWNER',
  realName: member.realName,
  mobile: member.mobile,
})

const isH5Preview = typeof window !== 'undefined' && Boolean((window as unknown as { __PROPERTY_SAAS_H5_PREVIEW__?: boolean }).__PROPERTY_SAAS_H5_PREVIEW__)

onMounted(async () => {
  if (!member.token && isH5Preview) {
    try {
      const session = await wxLogin({
        tenantId: 1,
        openid: 'demo-openid-001',
        mobile: '13900000001',
        realName: '业主用户',
      })
      member.setSession(session)
      form.realName = member.realName
      form.mobile = member.mobile
    } catch (error) {
      uni.showToast({ title: error instanceof Error ? error.message : '身份初始化失败', icon: 'none' })
    }
  }
})

async function submit() {
  if (!member.token) {
    uni.showToast({ title: '请先完成微信授权', icon: 'none' })
    return
  }
  if (!form.projectId || !form.houseId || !form.realName || !form.mobile) {
    uni.showToast({ title: '请补全绑定信息', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await applyHouseBinding({
      tenantId: member.currentTenantId,
      memberId: member.memberId,
      projectId: Number(form.projectId),
      houseId: Number(form.houseId),
      bindRole: form.bindRole,
      realName: form.realName,
      mobile: form.mobile,
    })
    uni.showToast({ title: '已提交' })
    uni.navigateTo({ url: '/pages/house/list' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #eef4f3; }
.intro { margin-bottom: 22rpx; padding: 34rpx; background: #0f766e; border-radius: 22rpx; color: #fff; }
.intro-title { display: block; font-size: 38rpx; font-weight: 900; }
.intro-copy { display: block; margin-top: 14rpx; color: #ccfbf1; font-size: 25rpx; line-height: 1.6; }
.panel { padding: 28rpx; background: #fff; border-radius: 20rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.input { height: 84rpx; margin-bottom: 18rpx; padding: 0 22rpx; background: #f8fafc; border: 1rpx solid #d9e2e7; border-radius: 12rpx; color: #1f2937; line-height: 84rpx; }
.primary { width: 100%; height: 78rpx; border-radius: 39rpx; background: #0f766e; color: #fff; font-size: 28rpx; font-weight: 800; }
</style>
