<template>
  <view class="page owner-home">
    <view class="owner-screen-title">
      <view>
        <text class="owner-page-title">{{ member.token ? '已绑定首页' : '游客首页' }}</text>
        <text class="owner-page-sub">{{ member.token ? '业主状态' : '公共入口' }}</text>
      </view>
      <text class="owner-pill">{{ member.token ? '业主服务' : '公共入口' }}</text>
    </view>

    <view class="owner-hero">
      <text class="owner-hero-title">{{ member.token ? projectName : '智慧物业' }}</text>
      <text class="owner-hero-copy">{{ heroCopy }}</text>
      <view class="owner-hero-row">
        <text class="owner-hero-meta">{{ member.token ? member.currentHouseNo || '请绑定或选择房屋' : '公共服务入口' }}</text>
        <button class="owner-hero-button" @click="go(member.token ? '/pages/house/switch' : '/pages/house/bind')">
          {{ member.token ? '切换' : '绑定房屋' }}
        </button>
      </view>
    </view>

    <view v-if="member.token" class="metric-grid">
      <view class="metric"><text>{{ summary.houseCount ?? 0 }}</text><label>绑定房屋</label></view>
      <view class="metric hot"><text>¥{{ summary.unpaidAmount ?? summary.unpaidBillAmount ?? summary.unpaidBillCount ?? 0 }}</text><label>待缴</label></view>
      <view class="metric"><text>{{ summary.workOrderCount ?? 0 }}</text><label>工单</label></view>
    </view>

    <button v-if="auditSummary.count" class="audit-alert" @click="go('/pages/house/bind')">
      <view>
        <text class="audit-title">{{ auditSummary.title }}</text>
        <text class="audit-copy">{{ auditSummary.copy }}</text>
      </view>
      <text class="audit-link">查看</text>
    </button>

    <view v-else class="owner-public-card">
      <text class="owner-public-title">绑定后可使用完整业主服务</text>
      <text class="owner-public-copy">支持业主、家属、租户、住户提交绑定审核，审核通过后可接收对应房屋的通知、账单和工单消息。</text>
      <view class="owner-public-actions">
        <button class="owner-primary" @click="go('/pages/house/bind')">立即绑定</button>
        <button class="owner-secondary" @click="go('/pages/notice/list')">查看公告</button>
      </view>
    </view>

    <view v-if="member.token" class="owner-section">
      <view class="owner-section-head">
        <text>重点待办</text>
        <text>本周</text>
      </view>
      <view class="card notice-list">
        <button class="notice-line" @click="go('/pages/bill/list')">
          <text class="badge orange">待缴</text>
          <view>
            <text class="line-title">本月物业费待支付</text>
            <text class="line-sub">如有待缴账单，请及时处理</text>
          </view>
          <text class="pay-link">支付</text>
        </button>
        <button class="notice-line" @click="go('/pages/workorder/list')">
          <text class="badge blue">工单</text>
          <view>
            <text class="line-title">查看报修处理进度</text>
            <text class="line-sub">维修、投诉、评价统一查询</text>
          </view>
        </button>
      </view>
    </view>

    <view class="owner-section">
      <view class="owner-section-head">
        <text>{{ member.token ? '快捷服务' : '常用服务' }}</text>
        <text>{{ member.token ? '全部' : '绑定后解锁更多' }}</text>
      </view>
      <view class="owner-service-grid">
        <view v-for="item in entries" :key="item.url" class="owner-service" @click="openEntry(item)">
          <text class="service-icon">{{ item.icon }}</text>
          <text class="service-title">{{ item.title }}</text>
          <text class="service-sub">{{ item.sub }}</text>
        </view>
      </view>
    </view>

    <view class="owner-section">
      <view class="owner-section-head">
        <text>{{ member.token ? '最新消息' : '社区动态' }}</text>
        <text @click="go('/pages/notice/list')">更多</text>
      </view>
      <view v-if="latestMessages.length" class="card notice-list">
        <view v-for="item in latestMessages" :key="messageKey(item)" class="notice-line" @click="openMessage(item)">
          <text :class="['badge', badgeClass(item)]">{{ messageCategoryText(item) }}</text>
          <view>
            <text class="line-title">{{ item.title || '物业消息' }}</text>
            <text class="line-sub">{{ shortMessageContent(item.content, 34) }}</text>
          </view>
          <text v-if="item.readStatus === 'UNREAD'" class="message-new">未读</text>
        </view>
      </view>
      <view v-else class="card notice-empty" @click="go('/pages/notice/list')">
        <text class="line-title">{{ member.token ? '暂无最新消息' : '暂无社区公告' }}</text>
        <text class="line-sub">{{ member.token ? '新的缴费、工单、房屋审核消息会在这里显示。' : '物业发布公告后会在这里显示。' }}</text>
      </view>
    </view>

    <AppTabBar active="home" />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '@/components/AppTabBar.vue'
import { fetchAppMessages, fetchHome, fetchHouses, fetchPublicNotices } from '@/api/app'
import { useMemberStore } from '@/store/member'
import { messageCategory, messageCategoryText, shortMessageContent } from '@/utils/message'

const member = useMemberStore()
const summary = reactive<Record<string, unknown>>({})
const bindings = ref<Record<string, unknown>[]>([])
const latestMessages = ref<Record<string, unknown>[]>([])
const projectName = computed(() => member.currentProjectName || '智慧物业')
const heroCopy = computed(() => {
  if (!member.token) return '先浏览公共公告与服务说明，绑定房屋后开启专属缴费、报修和通知。'
  return `${member.currentHouseNo || '当前未选择房屋'}，欢迎回来。查看待缴、工单和物业通知。`
})
const auditSummary = computed(() => {
  const pending = bindings.value.filter((item) => item.status === 'PENDING').length
  const rejected = bindings.value.filter((item) => item.status === 'REJECTED').length
  if (rejected > 0) {
    return { count: rejected, title: `${rejected} 条绑定申请被驳回`, copy: '请查看驳回说明后重新提交资料。' }
  }
  if (pending > 0) {
    return { count: pending, title: `${pending} 条房屋绑定待审核`, copy: '物业审核通过后将开启专属房屋服务。' }
  }
  return { count: 0, title: '', copy: '' }
})
const entries = [
  { title: '通知公告', sub: '公共消息', icon: '告', url: '/pages/notice/list', public: true },
  { title: '房屋绑定', sub: '提交审核', icon: '房', url: '/pages/house/bind', public: true },
  { title: '物业缴费', sub: member.token ? '账单支付' : '需绑定', icon: '缴', url: '/pages/bill/list' },
  { title: '报事报修', sub: member.token ? '一键提交' : '需绑定', icon: '修', url: '/pages/workorder/create' },
  { title: '投诉建议', sub: '服务反馈', icon: '诉', url: '/pages/workorder/complaint' },
  { title: '访客通行', sub: '临时邀约', icon: '访', url: '/pages/visitor/create' },
  { title: '车辆管理', sub: '车位月租', icon: '车', url: '/pages/vehicle/list' },
  { title: '个人中心', sub: '我的资料', icon: '我', url: '/pages/mine/index', public: true },
]

onShow(async () => {
  if (!member.token) {
    Object.keys(summary).forEach((key) => delete summary[key])
    bindings.value = []
    await loadPublicMessages()
    return
  }
  try {
    Object.assign(summary, await fetchHome())
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '首页加载失败', icon: 'none' })
  }
  try {
    bindings.value = (await fetchHouses()).records
  } catch (error) {
    bindings.value = []
  }
  await loadLatestMessages()
})

function go(url: string) {
  uni.navigateTo({ url })
}

function openEntry(item: { url: string, public?: boolean }) {
  if (item.public) {
    go(item.url)
    return
  }
  if (!member.token) {
    uni.showToast({ title: '请先绑定房屋', icon: 'none' })
    go('/pages/house/bind')
    return
  }
  if (!member.currentHouseId) {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    go('/pages/house/list')
    return
  }
  go(item.url)
}

async function loadLatestMessages() {
  try {
    const params: Record<string, unknown> = { pageNo: 1, pageSize: 3 }
    if (member.currentProjectId) {
      params.projectId = member.currentProjectId
    }
    latestMessages.value = (await fetchAppMessages(params)).records
  } catch {
    latestMessages.value = []
  }
}

async function loadPublicMessages() {
  try {
    latestMessages.value = (await fetchPublicNotices({
      tenantId: member.currentTenantId || 1,
      pageNo: 1,
      pageSize: 3,
    })).records
  } catch {
    latestMessages.value = []
  }
}

function openMessage(item: Record<string, unknown>) {
  uni.setStorageSync('current_message_detail', JSON.stringify(item))
  if (item.messageId) {
    go(`/pages/notice/detail?messageId=${item.messageId}`)
    return
  }
  go(`/pages/notice/detail?noticeId=${item.noticeId || ''}`)
}

function messageKey(item: Record<string, unknown>) {
  return String(item.messageId || item.noticeId || item.createdAt || item.title)
}

function badgeClass(item: Record<string, unknown>) {
  const category = messageCategory(item)
  if (category === 'payment' || category === 'refund') return 'orange'
  if (category === 'workorder') return 'blue'
  return 'green'
}
</script>

<style scoped>
.owner-home {
  min-height: 100vh;
  padding: 16px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% -2%, rgba(15, 118, 110, .18), transparent 35%),
    linear-gradient(180deg, #eef8f5 0%, #f8fafc 45%, #f3f6f8 100%);
}

.owner-screen-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.owner-page-title {
  display: block;
  color: #172033;
  font-size: 18px;
  line-height: 1.25;
  font-weight: 900;
}

.owner-page-sub {
  display: block;
  margin-top: 2px;
  color: #65758a;
  font-size: 12px;
  font-weight: 700;
}

.owner-pill {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 11px;
  color: #0b5f59;
  background: #dff5ef;
  border: 0.5px solid rgba(15, 118, 110, .13);
  border-radius: 499.5px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.owner-hero {
  position: relative;
  overflow: hidden;
  min-height: 146px;
  padding: 20px;
  color: #fff;
  background:
    radial-gradient(circle at 82% 12%, rgba(255, 255, 255, .24), transparent 24%),
    linear-gradient(135deg, #0f766e 0%, #124e61 100%);
  border-radius: 18px;
  box-sizing: border-box;
}

.owner-hero-title {
  display: block;
  font-size: 25px;
  font-weight: 900;
  line-height: 1.25;
}

.owner-hero-copy {
  display: block;
  margin-top: 9px;
  color: #cdfcf1;
  font-size: 12.5px;
  line-height: 1.55;
}

.owner-hero-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
}

.owner-hero-meta {
  flex: 1;
  min-width: 0;
  max-width: 210px;
  min-height: 34px;
  padding: 0 11px;
  overflow: hidden;
  color: #ecfeff;
  background: rgba(255, 255, 255, .14);
  border: 0.5px solid rgba(255, 255, 255, .18);
  border-radius: 499.5px;
  font-size: 12px;
  font-weight: 900;
  line-height: 34px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.owner-hero-button {
  flex: none;
  height: 38px;
  min-height: 38px;
  padding: 0 14px;
  color: #0b5f59;
  background: #fff;
  border-radius: 499.5px;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.owner-public-card,
.card {
  background: rgba(255, 255, 255, .96);
  border: 0.5px solid #dfe9e6;
  border-radius: 18px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, .055);
}

.owner-public-card {
  margin-top: 14px;
  padding: 15px;
}

.owner-public-title {
  display: block;
  color: #172033;
  font-size: 16px;
  font-weight: 900;
  line-height: 1.35;
}

.owner-public-copy {
  display: block;
  margin-top: 7px;
  color: #65758a;
  font-size: 12px;
  line-height: 1.6;
}

.owner-public-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.owner-public-actions button {
  flex: none;
  height: 38px;
  min-height: 38px;
  padding: 0 15px;
  border-radius: 499.5px;
  font-size: 12.5px;
  font-weight: 900;
}

.owner-primary {
  color: #fff;
  background: #0f766e;
  box-shadow: 0 5px 9px rgba(15, 118, 110, .2);
}

.owner-secondary {
  color: #334155;
  background: #e8f0f2;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 7px;
  margin-top: 11px;
}

.metric {
  padding: 12px 5px;
  background: rgba(255, 255, 255, .96);
  border: 0.5px solid #e2ece9;
  border-radius: 12px;
  text-align: center;
}

.metric text {
  display: block;
  color: #0b5f59;
  font-size: 18px;
  font-weight: 950;
}

.metric.hot text {
  color: #b45309;
}

.metric label {
  display: block;
  margin-top: 4px;
  color: #65758a;
  font-size: 11px;
}

.audit-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  margin-top: 11px;
  padding: 13px 14px;
  background: #fff;
  border: 0.5px solid #fed7aa;
  border-radius: 15px;
  box-shadow: 0 7px 17px rgba(180, 83, 9, .08);
  text-align: left;
}

.audit-title {
  display: block;
  color: #172033;
  font-size: 13.5px;
  font-weight: 900;
}

.audit-copy {
  display: block;
  margin-top: 4px;
  color: #9a5b00;
  font-size: 11px;
  line-height: 1.45;
}

.audit-link {
  flex: none;
  height: 30px;
  padding: 0 12px;
  color: #fff;
  background: #0f766e;
  border-radius: 499.5px;
  font-size: 12px;
  font-weight: 900;
  line-height: 30px;
}

.owner-section {
  margin-top: 14px;
}

.owner-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 0 2px 10px;
}

.owner-section-head text:first-child {
  color: #172033;
  font-size: 16px;
  line-height: 1.25;
  font-weight: 900;
}

.owner-section-head text:last-child {
  color: #65758a;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.owner-service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.owner-service {
  width: 100%;
  min-height: 78px;
  padding: 10px 5px 8px;
  background: #fff;
  border: 0.5px solid #e2ece9;
  border-radius: 16px;
  box-shadow: 0 4px 9px rgba(15, 23, 42, .035);
  text-align: center;
}

.service-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin: 0 auto 7px;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 12px;
  font-size: 11.5px;
  font-weight: 900;
}

.owner-service:nth-child(2n) .service-icon {
  color: #1d4ed8;
  background: #e8f0ff;
}

.owner-service:nth-child(3n) .service-icon {
  color: #b45309;
  background: #fff4dd;
}

.service-title {
  display: block;
  color: #172033;
  font-size: 11.5px;
  font-weight: 900;
  white-space: nowrap;
}

.service-sub {
  display: block;
  margin-top: 3px;
  color: #65758a;
  font-size: 9.5px;
  line-height: 1.25;
  white-space: nowrap;
}

.notice-list {
  padding: 4px 15px;
}

.notice-empty {
  padding: 17px 15px;
}

.notice-line {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  min-height: 55px;
  padding: 0;
  background: transparent;
  border-bottom: 0.5px solid #eef2f3;
  text-align: left;
}

.notice-line:last-child {
  border-bottom: 0;
}

.badge {
  flex: none;
  min-width: 38px;
  height: 22px;
  padding: 0 7px;
  border-radius: 499.5px;
  font-size: 10.5px;
  font-weight: 900;
  line-height: 22px;
  text-align: center;
}

.badge.green {
  color: #0b5f59;
  background: #dff5ef;
}

.badge.orange {
  color: #9a5b00;
  background: #fff4dd;
}

.badge.blue {
  color: #1d4ed8;
  background: #e8f0ff;
}

.line-title {
  display: block;
  color: #172033;
  font-size: 13.5px;
  font-weight: 900;
}

.line-sub {
  display: block;
  margin-top: 3.5px;
  color: #65758a;
  font-size: 11px;
}

.pay-link {
  margin-left: auto;
  color: #dc2626;
  font-size: 14px;
  font-weight: 950;
}

.message-new {
  flex: none;
  margin-left: auto;
  padding: 3px 7px;
  color: #fff;
  background: #ef4444;
  border-radius: 499.5px;
  font-size: 10px;
  font-weight: 900;
}
</style>
