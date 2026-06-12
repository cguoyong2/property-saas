<template>
  <view class="page">
    <view class="top">
      <view>
        <text class="top-title">我的房屋</text>
        <text class="top-sub">管理已绑定房屋和当前服务地址</text>
      </view>
      <button @click="goBind">新增</button>
    </view>

    <view v-for="item in houses" :key="String(item.bindId)" class="card" :class="{ current: Number(item.houseId) === Number(member.currentHouseId) }">
      <view class="card-head">
        <text class="title">{{ item.projectName }} {{ item.houseNo }}</text>
        <text class="badge" :class="String(item.status).toLowerCase()">{{ item.status }}</text>
      </view>
      <text class="meta">角色：{{ item.bindRole || '-' }}</text>
      <text v-if="Number(item.houseId) === Number(member.currentHouseId)" class="current-text">当前使用中</text>
      <view v-if="item.status === 'APPROVED'" class="actions">
        <button @click="selectHouse(item)">设为当前房屋</button>
        <button class="danger" @click="unbind(item)">解绑</button>
      </view>
    </view>

    <view v-if="!houses.length" class="empty-card">
      <text class="empty-title">还没有绑定房屋</text>
      <text class="empty-text">绑定房屋后可查看账单、工单、车辆和访客服务。</text>
      <button @click="goBind">新增绑定</button>
    </view>
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
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #eef4f3; }
.top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 22rpx; padding: 30rpx; background: #172554; border-radius: 22rpx; color: #fff; }
.top-title { display: block; font-size: 36rpx; font-weight: 900; }
.top-sub { display: block; margin-top: 8rpx; color: #bfdbfe; font-size: 24rpx; }
.top button { width: 120rpx; height: 62rpx; border-radius: 31rpx; background: #f59e0b; color: #111827; font-size: 25rpx; font-weight: 800; }
.card { margin-bottom: 20rpx; padding: 28rpx; background: #fff; border-radius: 18rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, .04); }
.card.current { border: 2rpx solid #0f766e; }
.card-head { display: flex; justify-content: space-between; gap: 16rpx; align-items: flex-start; }
.title { flex: 1; display: block; font-size: 32rpx; font-weight: 800; line-height: 1.35; }
.badge { flex: none; padding: 8rpx 16rpx; border-radius: 999rpx; background: #e2e8f0; color: #334155; font-size: 22rpx; }
.badge.approved { background: #dcfce7; color: #166534; }
.badge.pending { background: #fef3c7; color: #92400e; }
.badge.rejected { background: #fee2e2; color: #991b1b; }
.meta { display: block; margin: 14rpx 0 8rpx; color: #64748b; font-size: 24rpx; }
.current-text { display: block; margin-bottom: 16rpx; color: #0f766e; font-size: 24rpx; font-weight: 800; }
.actions { display: flex; gap: 16rpx; }
.actions button { flex: 1; height: 72rpx; border-radius: 36rpx; background: #0f766e; color: #fff; font-size: 26rpx; }
.actions .danger { background: #fee2e2; color: #b91c1c; }
.empty-card { margin-top: 24rpx; padding: 56rpx 34rpx; text-align: center; background: #fff; border-radius: 20rpx; }
.empty-title { display: block; font-size: 32rpx; font-weight: 800; }
.empty-text { display: block; margin: 14rpx 0 26rpx; color: #64748b; font-size: 25rpx; line-height: 1.6; }
.empty-card button { width: 188rpx; height: 68rpx; border-radius: 34rpx; background: #0f766e; color: #fff; font-size: 26rpx; }
</style>
