<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">消息</text>
        <text class="page-sub">{{ member.token ? '房屋通知与公共公告' : '公共公告' }}</text>
      </view>
      <text class="pill">{{ member.token ? `${unreadCount} 条未读` : `${records.length} 条` }}</text>
    </view>

    <view class="hero">
      <text class="hero-title">{{ member.token ? '我的通知' : '社区动态' }}</text>
      <text class="hero-copy">{{ member.token ? '查看物业公告、缴费提醒、工单处理和房屋通知。' : '未绑定也可以查看小区公开公告和服务提示。' }}</text>
    </view>

    <view class="section">
      <view class="section-head">
        <text>{{ member.token ? '最新消息' : '公开公告' }}</text>
        <button v-if="member.token && unreadCount" class="read-all" @click="readAll">全部已读</button>
        <text v-else>{{ member.currentHouseNo || (member.token ? '本人消息' : '全部') }}</text>
      </view>
      <view v-if="records.length" class="message-list">
        <view
          v-for="item in records"
          :key="messageKey(item)"
          :class="['message-card', item.readStatus === 'UNREAD' ? 'unread' : '']"
          @click="openDetail(item)"
        >
          <view class="message-head">
            <view class="badge-wrap">
              <text class="badge">{{ typeText(item.noticeType || item.templateCode) }}</text>
              <text v-if="item.readStatus === 'UNREAD'" class="unread-dot">未读</text>
            </view>
            <text class="date">{{ formatTime(item.publishedAt || item.createdAt) }}</text>
          </view>
          <text class="title">{{ item.title }}</text>
          <text class="content">{{ item.content }}</text>
          <view v-if="item.templateCode" class="message-foot">
            <text>{{ templateText(item.templateCode) }}</text>
            <text>查看详情</text>
          </view>
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
import { fetchAppMessages, fetchAppUnreadSummary, fetchPublicNotices, markAllAppMessagesRead } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const records = ref<Record<string, unknown>[]>([])
const unreadCount = ref(0)

async function load() {
  try {
    if (member.token) {
      const params: Record<string, unknown> = {
        pageNo: 1,
        pageSize: 50,
      }
      if (member.currentProjectId) {
        params.projectId = member.currentProjectId
      }
      const [messages, summary] = await Promise.all([
        fetchAppMessages(params),
        fetchAppUnreadSummary(member.currentProjectId ? { projectId: member.currentProjectId } : {}),
      ])
      records.value = messages.records
      unreadCount.value = Number(summary.unreadCount || 0)
      return
    }
    records.value = (await fetchPublicNotices({
      tenantId: member.currentTenantId || 1,
      pageNo: 1,
      pageSize: 50,
    })).records
    unreadCount.value = 0
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '公告加载失败', icon: 'none' })
  }
}

onShow(load)

async function readAll() {
  try {
    await markAllAppMessagesRead(member.currentProjectId ? { projectId: member.currentProjectId } : {})
    uni.showToast({ title: '已全部标记为已读', icon: 'success' })
    await load()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '操作失败', icon: 'none' })
  }
}

function openDetail(item: Record<string, unknown>) {
  uni.setStorageSync('current_message_detail', JSON.stringify(item))
  if (item.messageId) {
    uni.navigateTo({ url: `/pages/notice/detail?messageId=${item.messageId}` })
    return
  }
  uni.navigateTo({ url: `/pages/notice/detail?noticeId=${item.noticeId || ''}` })
}

function messageKey(item: Record<string, unknown>) {
  return String(item.messageId || item.noticeId || item.createdAt || item.title)
}

function formatTime(value: unknown) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function typeText(value: unknown) {
  const map: Record<string, string> = {
    PROPERTY: '公告',
    SYSTEM: '系统',
    PAYMENT: '缴费',
    WORKORDER: '工单',
    EMERGENCY: '紧急',
    HOUSE_BINDING_AUDIT: '审核',
  }
  return map[String(value || '').toUpperCase()] || '消息'
}

function templateText(value: unknown) {
  const map: Record<string, string> = {
    HOUSE_BINDING_AUDIT: '房屋绑定审核结果',
    BILL_DUE: '账单提醒',
    WORKORDER_DISPATCH: '工单进度',
  }
  return map[String(value || '').toUpperCase()] || '站内通知'
}
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
  align-items: center;
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

.read-all {
  margin: 0;
  padding: 0;
  color: #0f766e;
  background: transparent;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.4;
}

.read-all::after {
  display: none;
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
  position: relative;
}

.message-card.unread {
  border-color: #9bded4;
  box-shadow: 0 9px 22px rgba(15, 118, 110, .12);
}

.message-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 9px;
  margin-bottom: 8px;
}

.badge-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.badge {
  padding: 4px 8px;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 499.5px;
  font-size: 10.5px;
  font-weight: 900;
}

.unread-dot {
  padding: 3px 7px;
  color: #fff;
  background: #ef4444;
  border-radius: 499.5px;
  font-size: 10px;
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
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.message-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 0.5px solid #e6efed;
}

.message-foot text {
  color: #718096;
  font-size: 11px;
  font-weight: 800;
}

.message-foot text:last-child {
  color: #0f766e;
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
