<template>
  <view class="page">
    <view v-for="item in houses" :key="String(item.bindId)" class="card">
      <text class="title">{{ item.projectName }} {{ item.houseNo }}</text>
      <text class="meta">角色：{{ item.bindRole }} ｜ 状态：{{ item.status }}</text>
      <view v-if="item.status === 'APPROVED'" class="actions">
        <button @click="selectHouse(item)">设为当前房屋</button>
        <button class="danger" @click="unbind(item)">解绑</button>
      </view>
    </view>
    <button class="primary" @click="goBind">新增绑定</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchHouses, unbindHouse } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const houses = ref<Record<string, unknown>[]>([])

onShow(load)

async function load() {
  const page = await fetchHouses()
  houses.value = page.records
}

function selectHouse(item: Record<string, unknown>) {
  member.setCurrentHouse({
    tenantId: Number(member.currentTenantId),
    projectId: Number(item.projectId),
    houseId: Number(item.houseId),
    houseNo: String(item.houseNo ?? ''),
    bindRole: String(item.bindRole ?? ''),
  })
  uni.showToast({ title: '已切换' })
}

function goBind() {
  uni.navigateTo({ url: '/pages/house/bind' })
}

function unbind(item: Record<string, unknown>) {
  uni.showModal({
    title: '确认解绑',
    content: `确认解绑 ${item.houseNo ?? ''}？`,
    async success(result) {
      if (!result.confirm) return
      try {
        await unbindHouse(String(item.bindId), '业主小程序自助解绑')
        if (Number(item.houseId) === Number(member.currentHouseId)) {
          member.clearCurrentHouse()
        }
        uni.showToast({ title: '已解绑' })
        await load()
      } catch (error) {
        uni.showToast({ title: error instanceof Error ? error.message : '解绑失败', icon: 'none' })
      }
    },
  })
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { margin-bottom: 20rpx; padding: 28rpx; background: #fff; border-radius: 14rpx; }
.title { display: block; font-size: 32rpx; font-weight: 700; }
.meta { display: block; margin: 12rpx 0 20rpx; color: #64748b; font-size: 24rpx; }
.actions { display: flex; gap: 16rpx; }
button { flex: 1; background: #0f766e; color: #fff; }
.danger { background: #fee2e2; color: #b91c1c; }
.primary { margin-top: 16rpx; }
</style>
