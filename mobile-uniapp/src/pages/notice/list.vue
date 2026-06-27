<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">消息</text>
        <text class="page-sub">{{ member.token ? '房屋通知与公共公告' : '公共公告' }}</text>
      </view>
      <text class="pill">{{ records.length }} 条</text>
    </view>

    <view class="hero">
      <text class="hero-title">{{ member.token ? '我的通知' : '社区动态' }}</text>
      <text class="hero-copy">{{ member.token ? '查看物业公告、缴费提醒、工单处理和房屋通知。' : '未绑定也可以查看小区公开公告和服务提示。' }}</text>
    </view>

    <view class="section">
      <view class="section-head">
        <text>{{ member.token ? '最新消息' : '公开公告' }}</text>
        <text>{{ member.currentHouseNo || (member.token ? '本人消息' : '全部') }}</text>
      </view>
      <view v-if="records.length" class="message-list">
        <view v-for="item in records" :key="String(item.noticeId)" class="message-card">
          <view class="message-head">
            <text class="badge">{{ item.noticeType || '公告' }}</text>
            <text class="date">{{ item.publishedAt || item.createdAt }}</text>
          </view>
          <text class="title">{{ item.title }}</text>
          <text class="content">{{ item.content }}</text>
        </view>
      </view>
      <view v-else class="empty">
        <text class="empty-title">{{ member.token ? '暂无公告' : '暂无公共公告' }}</text>
        <text class="empty-text">物业发布公告后会在这里展示。</text>
      </view>
    </view>

    <AppTabBar active="message" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '@/components/AppTabBar.vue'
import { fetchNotices, fetchPublicNotices } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const records = ref<Record<string, unknown>[]>([])

onShow(async () => {
  try {
    if (member.token) {
      const params: Record<string, unknown> = {
        memberId: member.memberId,
        pageNo: 1,
        pageSize: 50,
      }
      if (member.currentProjectId) {
        params.projectId = member.currentProjectId
      }
      records.value = (await fetchNotices(params)).records
      return
    }
    records.value = (await fetchPublicNotices({
      tenantId: member.currentTenantId || 1,
      pageNo: 1,
      pageSize: 50,
    })).records
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '公告加载失败', icon: 'none' })
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 14px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% -2%, rgba(15, 118, 110, .18), transparent 35%),
    linear-gradient(180deg, #eef8f5 0%, #f8fafc 45%, #f3f6f8 100%);
}

.screen-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.page-title {
  display: block;
  color: #172033;
  font-size: 19px;
  font-weight: 900;
}

.page-sub {
  display: block;
  margin-top: 4px;
  color: #65758a;
  font-size: 12px;
}

.pill {
  padding: 6px 10px;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 499.5px;
  font-size: 11.5px;
  font-weight: 900;
}

.hero {
  padding: 18px;
  color: #fff;
  background:
    radial-gradient(circle at 82% 12%, rgba(255, 255, 255, .24), transparent 24%),
    linear-gradient(135deg, #0f766e 0%, #124e61 100%);
  border-radius: 18px;
}

.hero-title {
  display: block;
  font-size: 21px;
  font-weight: 900;
}

.hero-copy {
  display: block;
  margin-top: 7px;
  color: #cdfcf1;
  font-size: 12.5px;
  line-height: 1.55;
}

.section {
  margin-top: 14px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  margin: 0 2px 9px;
}

.section-head text:first-child {
  color: #172033;
  font-size: 15.5px;
  font-weight: 900;
}

.section-head text:last-child {
  color: #65758a;
  font-size: 11.5px;
  font-weight: 800;
}

.message-list {
  display: grid;
  gap: 9px;
}

.message-card,
.empty {
  background: rgba(255, 255, 255, .96);
  border: 0.5px solid #dfe9e6;
  border-radius: 14px;
  box-shadow: 0 7px 17px rgba(15, 23, 42, .055);
}

.message-card {
  padding: 14px;
}

.message-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 9px;
  margin-bottom: 8px;
}

.badge {
  padding: 4px 8px;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 499.5px;
  font-size: 10.5px;
  font-weight: 900;
}

.date {
  color: #94a3b8;
  font-size: 10.5px;
}

.title {
  display: block;
  color: #172033;
  font-size: 15px;
  font-weight: 900;
}

.content {
  display: block;
  margin-top: 6px;
  color: #65758a;
  font-size: 12px;
  line-height: 1.58;
}

.empty {
  padding: 39px 16px;
  text-align: center;
}

.empty-title {
  display: block;
  color: #172033;
  font-size: 16px;
  font-weight: 900;
}

.empty-text {
  display: block;
  margin-top: 7px;
  color: #65758a;
  font-size: 12px;
}
</style>
