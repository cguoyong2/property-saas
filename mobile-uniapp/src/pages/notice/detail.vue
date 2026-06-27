<template>
  <view class="page">
    <view class="detail-card">
      <view class="detail-head">
        <text class="badge">{{ typeText(message.noticeType || message.templateCode) }}</text>
        <text class="date">{{ formatTime(message.createdAt || message.publishedAt) }}</text>
      </view>
      <text class="title">{{ message.title || '消息详情' }}</text>
      <text v-if="message.readStatus" class="read-state">{{ message.readStatus === 'UNREAD' ? '未读' : '已读' }}</text>
      <view class="content">
        <text>{{ message.content || '暂无内容' }}</text>
      </view>
    </view>

    <view v-if="message.templateCode" class="meta-card">
      <view class="meta-row">
        <text>消息类型</text>
        <text>{{ templateText(message.templateCode) }}</text>
      </view>
      <view v-if="message.sentAt" class="meta-row">
        <text>发送时间</text>
        <text>{{ formatTime(message.sentAt) }}</text>
      </view>
      <view v-if="message.readAt" class="meta-row">
        <text>阅读时间</text>
        <text>{{ formatTime(message.readAt) }}</text>
      </view>
    </view>

    <button class="primary" @click="back">返回消息列表</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchAppMessageDetail, markAppMessageRead } from '@/api/app'

const message = ref<Record<string, unknown>>({})

onLoad(async (query) => {
  const cached = uni.getStorageSync('current_message_detail')
  if (cached) {
    try {
      message.value = JSON.parse(cached)
    } catch {
      message.value = {}
    }
  }
  const messageId = query?.messageId || message.value.messageId
  if (!messageId) return
  try {
    const detail = await fetchAppMessageDetail(String(messageId))
    message.value = detail
    if (detail.readStatus === 'UNREAD') {
      await markAppMessageRead(String(messageId))
      message.value = { ...detail, readStatus: 'READ', readAt: new Date().toISOString() }
    }
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '消息加载失败', icon: 'none' })
  }
})

function back() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: '/pages/notice/list' }),
  })
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
  padding: 16px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% -2%, rgba(15, 118, 110, .18), transparent 35%),
    linear-gradient(180deg, #eef8f5 0%, #f8fafc 45%, #f3f6f8 100%);
}

.detail-card,
.meta-card {
  background: rgba(255, 255, 255, .97);
  border: 0.5px solid #dfe9e6;
  border-radius: 16px;
  box-shadow: 0 9px 22px rgba(15, 23, 42, .06);
}

.detail-card {
  padding: 17px;
}

.detail-head,
.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.badge {
  padding: 5px 9px;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 499.5px;
  font-size: 11px;
  font-weight: 900;
}

.date,
.read-state,
.meta-row text:first-child {
  color: #718096;
  font-size: 11.5px;
  font-weight: 800;
}

.title {
  display: block;
  margin-top: 14px;
  color: #172033;
  font-size: 19px;
  font-weight: 900;
  line-height: 1.35;
}

.read-state {
  display: block;
  margin-top: 6px;
}

.content {
  margin-top: 16px;
  padding-top: 15px;
  border-top: 0.5px solid #e6efed;
}

.content text {
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-line;
}

.meta-card {
  margin-top: 12px;
  padding: 4px 15px;
}

.meta-row {
  padding: 12px 0;
  border-bottom: 0.5px solid #eef3f2;
}

.meta-row:last-child {
  border-bottom: 0;
}

.meta-row text:last-child {
  color: #172033;
  font-size: 12px;
  font-weight: 900;
}

.primary {
  margin: 18px 0 0;
  height: 46px;
  color: #fff;
  background: #0f766e;
  border-radius: 499.5px;
  font-size: 14px;
  font-weight: 900;
  line-height: 46px;
  box-shadow: 0 10px 22px rgba(15, 118, 110, .18);
}

.primary::after {
  display: none;
}
</style>
