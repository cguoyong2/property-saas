<template>
  <view class="page">
    <view class="panel">
      <picker :range="types" @change="form.orderType = types[Number($event.detail.value)]">
        <view class="input">类型：{{ form.orderType }}</view>
      </picker>
      <input v-model="form.title" class="input" placeholder="标题" />
      <textarea v-model="form.description" class="textarea" placeholder="投诉或建议内容" />
      <input v-model="form.location" class="input" placeholder="位置" />
      <button class="primary" :loading="loading" @click="submit">提交</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createComplaint } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const loading = ref(false)
const types = ['COMPLAINT', 'SUGGESTION']
const form = reactive({ orderType: 'COMPLAINT', title: '', description: '', location: '' })

async function submit() {
  if (!member.currentProjectId || !member.currentHouseId) {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    return
  }
  if (!form.title.trim() || !form.description.trim()) {
    uni.showToast({ title: '请填写标题和内容', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await createComplaint({
      projectId: member.currentProjectId,
      memberId: member.memberId,
      houseId: member.currentHouseId,
      ...form,
      imageFileIds: '',
      priority: 'NORMAL',
    })
    uni.showToast({ title: '已提交' })
    uni.navigateTo({ url: '/pages/workorder/list' })
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
.textarea { width: 100%; min-height: 220rpx; margin-bottom: 18rpx; padding: 22rpx; background: #f8fafc; border-radius: 10rpx; box-sizing: border-box; }
.primary { background: #0f766e; color: #fff; }
</style>
