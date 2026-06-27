<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="title">家属管理</text>
        <text class="subtitle">为当前房屋添加家属、租户或同住人</text>
      </view>
      <button class="switch-house" @click="goHouseList">切换房屋</button>
    </view>

    <view class="house-card">
      <view>
        <text class="house-label">当前房屋</text>
        <text class="house-name">{{ member.currentHouseNo || '未选择房屋' }}</text>
      </view>
      <text class="owner-badge">{{ member.currentBindRole === 'OWNER' ? '业主' : roleLabel(member.currentBindRole) }}</text>
    </view>

    <view class="section">
      <view class="section-head">
        <text>已添加人员</text>
        <text>{{ familyMembers.length }} 人</text>
      </view>
      <view v-if="familyMembers.length" class="member-list">
        <view v-for="item in familyMembers" :key="String(item.bindId)" class="member-card">
          <view class="member-main">
            <view>
              <text class="member-name">{{ item.realName || '-' }}</text>
              <text class="member-mobile">{{ item.mobile || '-' }}</text>
            </view>
            <text class="status" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</text>
          </view>
          <view class="member-meta">
            <text>{{ roleLabel(item.bindRole) }}</text>
            <text>{{ relationshipLabel(item.relationship) }}</text>
            <text>{{ item.applySource || '业主邀请' }}</text>
          </view>
          <text v-if="item.status === 'APPROVED'" class="audit-note approved">
            授权已生效{{ item.auditAt ? `：${formatTime(item.auditAt)}` : '' }}
          </text>
          <text v-if="item.status === 'REJECTED'" class="audit-note rejected">
            驳回原因：{{ item.auditRemark || '物业未填写原因' }}
          </text>
          <text v-if="item.status === 'PENDING'" class="audit-note pending">
            待物业审核，通过后授权范围才会生效。
          </text>
          <view class="permission-row">
            <text :class="permissionClass(item, 'allowNotice')">通知</text>
            <text :class="permissionClass(item, 'allowBill')">账单</text>
            <text :class="permissionClass(item, 'allowPayment')">缴费</text>
            <text :class="permissionClass(item, 'allowWorkOrder')">报修</text>
            <text :class="permissionClass(item, 'allowVisitor')">访客</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-card">
        <text>暂无家属/同住人</text>
        <text>提交后由物业审核，通过后对方可按授权使用服务。</text>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>新增人员</text>
        <text>待物业审核</text>
      </view>
      <view class="form-card">
        <label class="field">
          <text>姓名</text>
          <input v-model.trim="form.realName" placeholder="输入姓名" />
        </label>
        <label class="field">
          <text>手机号</text>
          <input
            v-model="form.mobile"
            type="number"
            maxlength="11"
            placeholder="11位手机号"
            @input="onMobileInput"
          />
        </label>
        <picker :range="roleOptions" range-key="label" :value="roleIndex" @change="onRoleChange">
          <view class="select-field">
            <text>身份</text>
            <text>{{ roleOptions[roleIndex].label }}</text>
          </view>
        </picker>
        <picker :range="relationshipOptions" range-key="label" :value="relationshipIndex" @change="onRelationshipChange">
          <view class="select-field">
            <text>关系</text>
            <text>{{ relationshipOptions[relationshipIndex].label }}</text>
          </view>
        </picker>

        <view class="permission-card">
          <text class="permission-title">授权范围</text>
          <view v-for="item in permissionOptions" :key="item.key" class="switch-row">
            <view>
              <text>{{ item.label }}</text>
              <text>{{ item.desc }}</text>
            </view>
            <switch
              color="#0f766e"
              :checked="Boolean(form[item.key])"
              @change="(event) => setPermission(item.key, event.detail.value)"
            />
          </view>
        </view>

        <button class="submit" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中' : '提交审核' }}
        </button>
      </view>
    </view>

    <AppTabBar active="service" />
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '@/components/AppTabBar.vue'
import { fetchFamilyMembers, inviteFamilyMember } from '@/api/app'
import { useMemberStore } from '@/store/member'

type PermissionKey = 'allowNotice' | 'allowBill' | 'allowPayment' | 'allowWorkOrder' | 'allowVisitor'

const member = useMemberStore()
const familyMembers = ref<Record<string, unknown>[]>([])
const submitting = ref(false)
const roleOptions = [
  { label: '家属', value: 'FAMILY' },
  { label: '租户', value: 'TENANT' },
  { label: '住户', value: 'RESIDENT' },
]
const relationshipOptions = [
  { label: '配偶', value: 'SPOUSE' },
  { label: '父母', value: 'PARENT' },
  { label: '子女', value: 'CHILD' },
  { label: '亲属', value: 'RELATIVE' },
  { label: '租户', value: 'TENANT' },
  { label: '其他', value: 'OTHER' },
]
const permissionOptions: Array<{ key: PermissionKey, label: string, desc: string }> = [
  { key: 'allowNotice', label: '接收通知', desc: '接收物业公告和房屋消息' },
  { key: 'allowBill', label: '查看账单', desc: '查看当前房屋费用账单' },
  { key: 'allowPayment', label: '缴纳费用', desc: '可在手机端发起缴费' },
  { key: 'allowWorkOrder', label: '提交报修', desc: '可提交报事报修和投诉建议' },
  { key: 'allowVisitor', label: '访客通行', desc: '可提交访客邀请' },
]
const roleIndex = ref(0)
const relationshipIndex = ref(0)
const form = reactive<Record<PermissionKey | 'realName' | 'mobile', string | boolean>>({
  realName: '',
  mobile: '',
  allowNotice: true,
  allowBill: false,
  allowPayment: false,
  allowWorkOrder: true,
  allowVisitor: true,
})

onShow(loadFamilyMembers)

async function loadFamilyMembers() {
  if (!member.token) {
    uni.navigateTo({ url: '/pages/house/bind' })
    return
  }
  if (!member.currentHouseId) {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    uni.navigateTo({ url: '/pages/house/list' })
    return
  }
  if (member.currentBindRole !== 'OWNER') {
    uni.showToast({ title: '仅业主可管理家属', icon: 'none' })
  }
  try {
    familyMembers.value = (await fetchFamilyMembers({ houseId: member.currentHouseId })).records
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' })
  }
}

function onMobileInput(event: { detail: { value: string } }) {
  form.mobile = event.detail.value.replace(/\D/g, '').slice(0, 11)
}

function onRoleChange(event: { detail: { value: string | number } }) {
  roleIndex.value = Number(event.detail.value)
}

function onRelationshipChange(event: { detail: { value: string | number } }) {
  relationshipIndex.value = Number(event.detail.value)
}

function setPermission(key: PermissionKey, value: boolean) {
  form[key] = value
}

async function submit() {
  const mobile = String(form.mobile || '')
  if (!String(form.realName || '').trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }
  if (!/^\d{11}$/.test(mobile)) {
    uni.showToast({ title: '手机号必须为11位数字', icon: 'none' })
    return
  }
  if (!member.currentHouseId) {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await inviteFamilyMember({
      houseId: member.currentHouseId,
      realName: String(form.realName).trim(),
      mobile,
      bindRole: roleOptions[roleIndex.value].value,
      relationship: relationshipOptions[relationshipIndex.value].value,
      allowNotice: form.allowNotice,
      allowBill: form.allowBill,
      allowPayment: form.allowPayment,
      allowWorkOrder: form.allowWorkOrder,
      allowVisitor: form.allowVisitor,
    })
    form.realName = ''
    form.mobile = ''
    uni.showToast({ title: '已提交审核', icon: 'success' })
    await loadFamilyMembers()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function goHouseList() {
  uni.navigateTo({ url: '/pages/house/list' })
}

function roleLabel(value: unknown) {
  const map: Record<string, string> = { OWNER: '业主', FAMILY: '家属', TENANT: '租户', RESIDENT: '住户' }
  return map[String(value || '')] || '-'
}

function relationshipLabel(value: unknown) {
  const map: Record<string, string> = {
    SPOUSE: '配偶',
    PARENT: '父母',
    CHILD: '子女',
    RELATIVE: '亲属',
    TENANT: '租户',
    OTHER: '其他',
  }
  return map[String(value || '')] || '其他'
}

function statusLabel(value: unknown) {
  const map: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', UNBOUND: '已解绑' }
  return map[String(value || '')] || '-'
}

function statusClass(value: unknown) {
  return {
    pending: value === 'PENDING',
    approved: value === 'APPROVED',
    rejected: value === 'REJECTED',
  }
}

function permissionClass(item: Record<string, unknown>, key: PermissionKey) {
  const enabled = item[key] === true || item[key] === 1 || item[key] === '1' || item[key] === 'true'
  return {
    on: item.status === 'APPROVED' && enabled,
    pending: item.status === 'PENDING' && enabled,
  }
}

function formatTime(value: unknown) {
  return String(value || '').replace('T', ' ').slice(0, 16)
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

.screen-title,
.house-card,
.section-head,
.member-main,
.member-meta,
.permission-row,
.select-field,
.switch-row {
  display: flex;
  align-items: center;
}

.screen-title,
.section-head,
.member-main,
.select-field,
.switch-row {
  justify-content: space-between;
}

.screen-title {
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.title {
  display: block;
  color: #172033;
  font-size: 19px;
  font-weight: 900;
}

.subtitle {
  display: block;
  margin-top: 4px;
  color: #65758a;
  font-size: 12px;
}

.switch-house {
  flex: none;
  height: 32px;
  padding: 0 12px;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.house-card,
.form-card,
.member-card,
.empty-card {
  background: rgba(255, 255, 255, .96);
  border: 0.5px solid #dfe9e6;
  box-shadow: 0 7px 17px rgba(15, 23, 42, .055);
}

.house-card {
  justify-content: space-between;
  padding: 15px;
  border-radius: 16px;
}

.house-label {
  display: block;
  color: #65758a;
  font-size: 11.5px;
}

.house-name {
  display: block;
  margin-top: 5px;
  color: #172033;
  font-size: 17px;
  font-weight: 900;
}

.owner-badge {
  padding: 7px 11px;
  color: #0b5f59;
  background: #e8f7f3;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.section {
  margin-top: 15px;
}

.section-head {
  margin: 0 2px 9px;
}

.section-head text:first-child {
  color: #172033;
  font-size: 16px;
  font-weight: 900;
}

.section-head text:last-child {
  color: #65758a;
  font-size: 12px;
  font-weight: 800;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.member-card,
.form-card,
.empty-card {
  border-radius: 16px;
}

.member-card {
  padding: 14px;
}

.member-name {
  display: block;
  color: #172033;
  font-size: 15.5px;
  font-weight: 900;
}

.member-mobile {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.status {
  padding: 5px 9px;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
}

.status.pending {
  color: #b45309;
  background: #fef3c7;
}

.status.approved {
  color: #0f766e;
  background: #dff5ef;
}

.status.rejected {
  color: #b91c1c;
  background: #fee2e2;
}

.member-meta,
.permission-row {
  gap: 7px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.audit-note {
  display: block;
  margin-top: 9px;
  font-size: 11.5px;
  line-height: 1.5;
}

.audit-note.approved {
  color: #0f766e;
}

.audit-note.pending {
  color: #b45309;
}

.audit-note.rejected {
  color: #b91c1c;
}

.member-meta text,
.permission-row text {
  padding: 5px 8px;
  color: #64748b;
  background: #f8fafc;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 800;
}

.permission-row text.on {
  color: #0b5f59;
  background: #e0f2f1;
}

.permission-row text.pending {
  color: #b45309;
  background: #fef3c7;
}

.empty-card {
  padding: 22px 14px;
  text-align: center;
}

.empty-card text:first-child {
  display: block;
  color: #172033;
  font-size: 15px;
  font-weight: 900;
}

.empty-card text:last-child {
  display: block;
  margin-top: 7px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.form-card {
  padding: 15px;
}

.field {
  display: block;
  margin-bottom: 12px;
}

.field text,
.permission-title {
  display: block;
  margin-bottom: 7px;
  color: #172033;
  font-size: 12px;
  font-weight: 900;
}

.field input,
.select-field {
  height: 44px;
  padding: 0 12px;
  box-sizing: border-box;
  color: #172033;
  background: #f8fafc;
  border: 0.5px solid #d6e1df;
  border-radius: 12px;
  font-size: 14px;
}

.select-field {
  margin-bottom: 12px;
}

.select-field text:first-child {
  color: #64748b;
  font-size: 12px;
}

.select-field text:last-child {
  color: #172033;
  font-size: 14px;
  font-weight: 900;
}

.permission-card {
  margin-top: 3px;
  padding: 12px;
  background: #f8fcfb;
  border: 0.5px solid #dfe9e6;
  border-radius: 14px;
}

.switch-row {
  min-height: 48px;
  border-top: 0.5px solid #e5eeeb;
}

.switch-row:first-of-type {
  border-top: 0;
}

.switch-row view text:first-child {
  display: block;
  color: #172033;
  font-size: 13px;
  font-weight: 900;
}

.switch-row view text:last-child {
  display: block;
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
}

.submit {
  height: 44px;
  margin-top: 15px;
  color: #fff;
  background: #0f766e;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 900;
}

.submit[disabled] {
  color: #fff;
  background: #94a3b8;
}
</style>
