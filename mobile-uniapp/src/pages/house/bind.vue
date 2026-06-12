<template>
  <view class="page">
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
import { reactive, ref } from 'vue'
import { applyHouseBinding } from '@/api/app'
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

async function submit() {
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
    uni.navigateBack()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.panel { padding: 28rpx; background: #fff; border-radius: 14rpx; }
.input { height: 84rpx; margin-bottom: 18rpx; padding: 0 22rpx; background: #f8fafc; border-radius: 10rpx; line-height: 84rpx; }
.primary { background: #0f766e; color: #fff; }
</style>
