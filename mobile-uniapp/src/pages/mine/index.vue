<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">我的</text>
        <text class="page-sub">家庭与账户</text>
      </view>
      <text class="pill">{{ member.token ? '已授权' : '未绑定' }}</text>
    </view>

    <view class="profile-card">
      <view class="avatar-row">
        <text class="avatar">{{ avatarText }}</text>
        <view class="avatar-copy">
          <text class="name">{{ mine.realName || member.realName || '访客用户' }}</text>
          <text class="house">{{ member.currentHouseNo || (member.token ? '未选房屋' : '绑定房屋后接收专属通知和账单') }}</text>
        </view>
        <button class="switch" @click="go(member.token ? '/pages/house/switch' : '/pages/house/bind')">
          {{ member.token ? '切换' : '绑定' }}
        </button>
      </view>
      <view class="family-strip">
        <text>已通过 {{ member.token ? approvedCount : '-' }}</text>
        <text>待审核 {{ member.token ? pendingCount : '-' }}</text>
        <text>被驳回 {{ member.token ? rejectedCount : '-' }}</text>
      </view>
    </view>

    <view v-if="!member.token" class="bind-card">
      <text class="bind-title">绑定房屋后开启完整服务</text>
      <text class="bind-copy">可查看本人房屋账单、缴费记录、工单进度、家属房屋关系和物业通知。</text>
      <button @click="go('/pages/house/bind')">绑定房屋</button>
    </view>

    <button v-if="auditSummary.count" class="audit-card" @click="go('/pages/house/bind')">
      <view>
        <text class="audit-title">{{ auditSummary.title }}</text>
        <text class="audit-copy">{{ auditSummary.copy }}</text>
      </view>
      <text class="arrow">›</text>
    </button>

    <view class="section">
      <view class="section-head">
        <text>我的关系</text>
        <text>可管理</text>
      </view>
      <view class="mini-stack">
        <button class="wide-card" @click="goPrivate('/pages/house/list')">
          <text class="wide-title">我的房屋</text>
          <text class="wide-copy">绑定、切换、解绑本人或家属房屋。</text>
        </button>
        <button class="wide-card" @click="goPrivate('/pages/family/list')">
          <text class="wide-title">家属管理</text>
          <text class="wide-copy">添加配偶、父母、子女等同住人，授权接收通知和办理服务。</text>
        </button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>我的服务</text>
        <text>全部</text>
      </view>
      <view class="row-list">
        <button v-for="item in menuItems" :key="item.url" class="row" @click="goPrivate(item.url)">
          <view>
            <text class="row-title">{{ item.title }}</text>
            <text class="row-sub">{{ item.sub }}</text>
          </view>
          <text class="arrow">›</text>
        </button>
      </view>
    </view>

    <button v-if="member.token" class="logout" @click="logout">退出当前身份</button>
    <AppTabBar active="mine" />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '@/components/AppTabBar.vue'
import { fetchHouses, fetchMine } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const mine = reactive<Record<string, unknown>>({})
const bindings = ref<Record<string, unknown>[]>([])
const avatarText = computed(() => String(mine.realName || member.realName || '访').slice(0, 1))
const approvedCount = computed(() => bindings.value.filter((item) => item.status === 'APPROVED').length)
const pendingCount = computed(() => bindings.value.filter((item) => item.status === 'PENDING').length)
const rejectedCount = computed(() => bindings.value.filter((item) => item.status === 'REJECTED').length)
const auditSummary = computed(() => {
  const pending = pendingCount.value
  const rejected = rejectedCount.value
  if (rejected > 0) {
    return { count: rejected, title: `${rejected} 条绑定申请被驳回`, copy: '查看驳回原因，补充资料后重新提交。' }
  }
  if (pending > 0) {
    return { count: pending, title: `${pending} 条房屋绑定待审核`, copy: '物业审核通过后会在消息里通知您。' }
  }
  return { count: 0, title: '', copy: '' }
})
const menuItems = [
  { title: '我的账单', sub: '待缴费用和缴费记录', url: '/pages/bill/list', permission: 'bill' },
  { title: '缴费记录', sub: '收款凭证、退款和支付记录', url: '/pages/payment/history', permission: 'bill' },
  { title: '预存款', sub: '超收余额和账单抵扣明细', url: '/pages/payment/prepayment', permission: 'bill' },
  { title: '我的工单', sub: '报修、投诉、评价记录', url: '/pages/workorder/list', permission: 'workOrder' },
  { title: '我的车辆', sub: '车牌、车位、月租信息', url: '/pages/vehicle/list' },
  { title: '我的租赁', sub: '合同和到期提醒', url: '/pages/lease/contracts' },
  { title: '联系物业', sub: '客服电话、管家、服务时间', url: '/pages/notice/list' },
]

onShow(async () => {
  if (!member.token) {
    Object.keys(mine).forEach((key) => delete mine[key])
    bindings.value = []
    return
  }
  try {
    Object.assign(mine, await fetchMine())
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' })
  }
  try {
    bindings.value = (await fetchHouses()).records
  } catch (error) {
    bindings.value = []
  }
})

function go(url: string) {
  uni.navigateTo({ url })
}

function goPrivate(url: string) {
  if (!member.token) {
    uni.showToast({ title: '请先绑定房屋', icon: 'none' })
    uni.navigateTo({ url: '/pages/house/bind' })
    return
  }
  if (url === '/pages/family/list' && member.currentBindRole !== 'OWNER') {
    uni.showToast({ title: '仅业主可管理家属/同住人', icon: 'none' })
    return
  }
  const item = menuItems.find((menu) => menu.url === url)
  if (item?.permission && !featureAllowed(item.permission)) {
    uni.showToast({ title: '当前房屋未开通该授权，请联系业主或物业', icon: 'none' })
    return
  }
  uni.navigateTo({ url })
}

function featureAllowed(permission: string) {
  if (member.currentBindRole === 'OWNER') return true
  if (permission === 'bill') return member.currentAllowBill
  if (permission === 'payment') return member.currentAllowPayment
  if (permission === 'workOrder') return member.currentAllowWorkOrder
  if (permission === 'visitor') return member.currentAllowVisitor
  if (permission === 'notice') return member.currentAllowNotice
  return true
}

function logout() {
  member.clearSession()
  uni.switchTab({ url: '/pages/home/index' })
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

.profile-card {
  padding: 18px;
  color: #fff;
  background: linear-gradient(135deg, #0f766e 0%, #164e63 100%);
  border-radius: 18px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  color: #0b5f59;
  background: #ecfeff;
  border-radius: 14px;
  font-size: 17px;
  font-weight: 900;
}

.avatar-copy {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 17px;
  font-weight: 900;
}

.house {
  display: block;
  margin-top: 5px;
  overflow: hidden;
  color: #cdfcf1;
  font-size: 11.5px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.switch {
  flex: none;
  height: 31px;
  padding: 0 11px;
  color: #0b5f59;
  background: #fff;
  border-radius: 499.5px;
  font-size: 12px;
  font-weight: 900;
}

.family-strip {
  display: flex;
  gap: 8px;
  margin-top: 13px;
}

.family-strip text {
  flex: 1;
  padding: 10px 4px;
  color: #d7f7ef;
  background: rgba(255, 255, 255, .13);
  border: 0.5px solid rgba(255, 255, 255, .16);
  border-radius: 11px;
  text-align: center;
  font-size: 11px;
  font-weight: 900;
}

.bind-card,
.audit-card,
.wide-card,
.row-list {
  background: rgba(255, 255, 255, .96);
  border: 0.5px solid #dfe9e6;
  border-radius: 14px;
  box-shadow: 0 7px 17px rgba(15, 23, 42, .055);
}

.bind-card {
  margin-top: 12px;
  padding: 15px;
}

.bind-title {
  display: block;
  color: #172033;
  font-size: 15.5px;
  font-weight: 900;
}

.bind-copy {
  display: block;
  margin: 6px 0 12px;
  color: #65758a;
  font-size: 12px;
  line-height: 1.65;
}

.bind-card button {
  width: 88px;
  height: 34px;
  color: #fff;
  background: #0f766e;
  border-radius: 499.5px;
  font-size: 13px;
  font-weight: 900;
}

.audit-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  margin-top: 12px;
  padding: 13px 14px;
  border-color: #fed7aa;
  box-shadow: 0 7px 17px rgba(180, 83, 9, .08);
  text-align: left;
}

.audit-title {
  display: block;
  color: #172033;
  font-size: 14px;
  font-weight: 900;
}

.audit-copy {
  display: block;
  margin-top: 4px;
  color: #9a5b00;
  font-size: 11.5px;
  line-height: 1.5;
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

.mini-stack {
  display: grid;
  gap: 9px;
}

.wide-card {
  width: 100%;
  padding: 14px;
  text-align: left;
}

.wide-title {
  display: block;
  color: #172033;
  font-size: 14.5px;
  font-weight: 900;
}

.wide-copy {
  display: block;
  margin-top: 5px;
  color: #65758a;
  font-size: 11.5px;
  line-height: 1.5;
}

.row-list {
  padding: 4px 14px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 58px;
  padding: 0;
  background: transparent;
  border-bottom: 0.5px solid #eef2f3;
  text-align: left;
}

.row:last-child {
  border-bottom: 0;
}

.row-title {
  display: block;
  color: #172033;
  font-size: 14px;
  font-weight: 900;
}

.row-sub {
  display: block;
  margin-top: 4px;
  color: #65758a;
  font-size: 11px;
}

.arrow {
  color: #94a3b8;
  font-size: 21px;
}

.logout {
  width: 100%;
  height: 38px;
  margin-top: 14px;
  color: #991b1b;
  background: #fee2e2;
  border-radius: 499.5px;
  font-size: 13.5px;
  font-weight: 900;
}
</style>
