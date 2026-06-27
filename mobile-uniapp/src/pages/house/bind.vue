<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">房屋绑定</text>
        <text class="page-sub">按小区、楼栋、单元、房屋提交审核</text>
      </view>
      <text class="pill">待物业审核</text>
    </view>

    <view class="hero">
      <text class="hero-title">绑定我的房屋</text>
      <text class="hero-copy">审核通过后可接收对应房屋通知，查看账单并办理缴费、报修、访客和家属管理。</text>
      <view class="hero-tags">
        <text>业主端提交</text>
        <text>物业端审核</text>
      </view>
    </view>

    <view v-if="!member.token" class="notice-card">
      <text class="notice-title">请先完成微信授权</text>
      <text class="notice-copy">授权后才能提交房屋绑定申请。</text>
      <button class="primary full" @click="initPreviewSession">模拟授权</button>
    </view>

    <view v-else class="form-card">
      <view class="card-title">
        <text>绑定信息</text>
        <text>{{ selectedHouseLabel || '逐级选择' }}</text>
      </view>

      <picker :range="projects" range-key="projectName" @change="selectProject">
        <view class="select-field" :class="{ disabled: loadingProjects }">
          <view>
            <text class="label">小区名称</text>
            <text class="value">{{ selectedProjectLabel || '请选择小区' }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </picker>

      <picker :disabled="!form.projectId" :range="buildings" range-key="buildingName" @change="selectBuilding">
        <view class="select-field" :class="{ disabled: !form.projectId }">
          <view>
            <text class="label">楼栋名称</text>
            <text class="value">{{ selectedBuildingLabel || '请先选择小区' }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </picker>

      <picker :disabled="!form.buildingId" :range="units" range-key="unitName" @change="selectUnit">
        <view class="select-field" :class="{ disabled: !form.buildingId }">
          <view>
            <text class="label">单元</text>
            <text class="value">{{ selectedUnitLabel || '请先选择楼栋' }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </picker>

      <picker :disabled="!form.unitId" :range="houses" range-key="houseNo" @change="selectHouse">
        <view class="select-field" :class="{ disabled: !form.unitId }">
          <view>
            <text class="label">房屋</text>
            <text class="value">{{ selectedHouseLabel || '请先选择单元' }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </picker>

      <picker :range="roleOptions" range-key="label" @change="selectRole">
        <view class="select-field">
          <view>
            <text class="label">住户类型</text>
            <text class="value">{{ roleLabel }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </picker>

      <view class="field">
        <text class="label">姓名</text>
        <input v-model="form.realName" placeholder="请输入真实姓名" />
      </view>

      <view class="field">
        <text class="label">手机号</text>
        <input v-model="form.mobile" maxlength="11" type="number" placeholder="请输入11位手机号" />
      </view>

      <view class="actions">
        <button class="primary" :loading="submitting" @click="submit">提交审核</button>
        <button class="secondary" @click="goList">查看进度</button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>审核进度</text>
        <text @click="loadBindings">刷新</text>
      </view>
      <view v-for="item in bindings" :key="String(item.bindId)" class="binding-card">
        <view>
          <text class="binding-title">{{ item.projectName || '-' }}</text>
          <text class="binding-room">{{ roomText(item) }}</text>
        </view>
        <text class="status" :class="String(item.status).toLowerCase()">{{ statusText(item.status) }}</text>
        <text class="binding-meta">身份：{{ roleText(String(item.bindRole || '')) }} ｜ 来源：{{ item.applySource || '业主端提交' }} ｜ 申请：{{ formatTime(item.createdAt) }}</text>
        <text v-if="item.status === 'APPROVED'" class="audit-result approved">
          审核通过后，可在“我的房屋”中设为当前房屋。
        </text>
        <text v-if="item.status === 'REJECTED'" class="audit-result rejected">
          驳回原因：{{ item.auditRemark || '物业未填写原因' }}
        </text>
      </view>
      <view v-if="!bindings.length" class="empty-card">
        <text>暂无绑定申请</text>
      </view>
    </view>

    <AppTabBar active="service" />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import {
  applyHouseBinding,
  fetchBindBuildings,
  fetchBindHouses,
  fetchBindProjects,
  fetchBindUnits,
  fetchHouses,
  wxLogin,
} from '@/api/app'
import { useMemberStore } from '@/store/member'

type Dict = Record<string, unknown>

const member = useMemberStore()
const roleOptions = [
  { label: '业主', value: 'OWNER' },
  { label: '家属', value: 'FAMILY' },
  { label: '租户', value: 'TENANT' },
  { label: '住户', value: 'RESIDENT' },
]
const roleLabels: Record<string, string> = Object.fromEntries(roleOptions.map((item) => [item.value, item.label]))
const statusLabels: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  UNBOUND: '已解绑',
}

const loadingProjects = ref(false)
const submitting = ref(false)
const projects = ref<Dict[]>([])
const buildings = ref<Dict[]>([])
const units = ref<Dict[]>([])
const houses = ref<Dict[]>([])
const bindings = ref<Dict[]>([])

const form = reactive({
  tenantId: member.currentTenantId ? String(member.currentTenantId) : '1',
  projectId: '',
  buildingId: '',
  unitId: '',
  houseId: '',
  bindRole: 'OWNER',
  realName: member.realName,
  mobile: member.mobile,
})

const selectedProject = computed(() => projects.value.find((item) => String(item.projectId) === form.projectId))
const selectedBuilding = computed(() => buildings.value.find((item) => String(item.buildingId) === form.buildingId))
const selectedUnit = computed(() => units.value.find((item) => String(item.unitId) === form.unitId))
const selectedHouse = computed(() => houses.value.find((item) => String(item.houseId) === form.houseId))
const selectedProjectLabel = computed(() => String(selectedProject.value?.projectName ?? ''))
const selectedBuildingLabel = computed(() => String(selectedBuilding.value?.buildingName ?? ''))
const selectedUnitLabel = computed(() => String(selectedUnit.value?.unitName ?? ''))
const selectedHouseLabel = computed(() => {
  const houseNo = String(selectedHouse.value?.houseNo ?? '')
  return houseNo ? `${selectedBuildingLabel.value}${selectedUnitLabel.value}${houseNo}` : ''
})
const roleLabel = computed(() => roleText(form.bindRole))
const isH5Preview = typeof window !== 'undefined' && Boolean((window as unknown as { __PROPERTY_SAAS_H5_PREVIEW__?: boolean }).__PROPERTY_SAAS_H5_PREVIEW__)

onMounted(async () => {
  if (!member.token && isH5Preview) {
    await initPreviewSession()
  }
  if (member.token) {
    await Promise.all([loadProjects(), loadBindings()])
  }
})

async function initPreviewSession() {
  try {
    const session = await wxLogin({
      tenantId: Number(form.tenantId || 1),
      openid: 'demo-openid-001',
      mobile: '13900000001',
      realName: '业主用户',
    })
    member.setSession(session)
    form.tenantId = String(member.currentTenantId || 1)
    form.realName = member.realName
    form.mobile = member.mobile
    await Promise.all([loadProjects(), loadBindings()])
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '授权失败', icon: 'none' })
  }
}

async function loadProjects() {
  loadingProjects.value = true
  try {
    projects.value = (await fetchBindProjects()).records
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '小区加载失败', icon: 'none' })
  } finally {
    loadingProjects.value = false
  }
}

async function loadBindings() {
  if (!member.token) return
  try {
    bindings.value = (await fetchHouses()).records
  } catch {
    bindings.value = []
  }
}

async function selectProject(event: { detail: { value: number | string } }) {
  const item = projects.value[Number(event.detail.value)]
  form.projectId = String(item?.projectId ?? '')
  form.buildingId = ''
  form.unitId = ''
  form.houseId = ''
  buildings.value = []
  units.value = []
  houses.value = []
  if (form.projectId) {
    buildings.value = (await fetchBindBuildings(form.projectId)).records
  }
}

async function selectBuilding(event: { detail: { value: number | string } }) {
  const item = buildings.value[Number(event.detail.value)]
  form.buildingId = String(item?.buildingId ?? '')
  form.unitId = ''
  form.houseId = ''
  units.value = []
  houses.value = []
  if (form.projectId && form.buildingId) {
    units.value = (await fetchBindUnits(form.projectId, form.buildingId)).records
  }
}

async function selectUnit(event: { detail: { value: number | string } }) {
  const item = units.value[Number(event.detail.value)]
  form.unitId = String(item?.unitId ?? '')
  form.houseId = ''
  houses.value = []
  if (form.projectId && form.buildingId && form.unitId) {
    houses.value = (await fetchBindHouses(form.projectId, form.buildingId, form.unitId)).records
  }
}

function selectHouse(event: { detail: { value: number | string } }) {
  const item = houses.value[Number(event.detail.value)]
  form.houseId = String(item?.houseId ?? '')
}

function selectRole(event: { detail: { value: number | string } }) {
  form.bindRole = String(roleOptions[Number(event.detail.value)]?.value ?? 'OWNER')
}

async function submit() {
  if (!member.token) {
    uni.showToast({ title: '请先完成微信授权', icon: 'none' })
    return
  }
  if (!form.projectId || !form.buildingId || !form.unitId || !form.houseId) {
    uni.showToast({ title: '请完整选择小区、楼栋、单元和房屋', icon: 'none' })
    return
  }
  if (!form.realName.trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }
  if (!/^\\d{11}$/.test(form.mobile.trim())) {
    uni.showToast({ title: '手机号必须为11位数字', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await applyHouseBinding({
      tenantId: Number(form.tenantId),
      memberId: member.memberId,
      projectId: Number(form.projectId),
      houseId: Number(form.houseId),
      bindRole: form.bindRole,
      realName: form.realName.trim(),
      mobile: form.mobile.trim(),
    })
    uni.showToast({ title: '已提交审核' })
    await loadBindings()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function goList() {
  uni.navigateTo({ url: '/pages/house/list' })
}

function roleText(value: string) {
  return roleLabels[value] ?? (value || '-')
}

function statusText(value: unknown) {
  return statusLabels[String(value)] ?? String(value || '-')
}

function roomText(item: Dict) {
  return String(item.roomNo || `${item.buildingName || ''}${item.unitName || ''}${item.houseNo || ''}` || '-')
}

function formatTime(value: unknown) {
  return String(value || '').replace('T', ' ').slice(0, 16) || '-'
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx 28rpx 150rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% -2%, rgba(15, 118, 110, .16), transparent 35%),
    linear-gradient(180deg, #eef8f5 0%, #f8fafc 45%, #f3f6f8 100%);
}

.screen-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28rpx;
}

.page-title {
  display: block;
  color: #172033;
  font-size: 38rpx;
  font-weight: 900;
}

.page-sub {
  display: block;
  margin-top: 8rpx;
  color: #65758a;
  font-size: 24rpx;
}

.pill {
  padding: 12rpx 20rpx;
  color: #0b5f59;
  background: #dff5ef;
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 900;
}

.hero {
  padding: 40rpx;
  color: #fff;
  background:
    radial-gradient(circle at 82% 12%, rgba(255, 255, 255, .24), transparent 24%),
    linear-gradient(135deg, #0f766e 0%, #124e61 100%);
  border-radius: 36rpx;
}

.hero-title {
  display: block;
  font-size: 44rpx;
  font-weight: 900;
}

.hero-copy {
  display: block;
  margin-top: 16rpx;
  color: #cdfcf1;
  font-size: 25rpx;
  line-height: 1.55;
}

.hero-tags {
  display: flex;
  gap: 14rpx;
  margin-top: 24rpx;
}

.hero-tags text {
  padding: 14rpx 18rpx;
  color: #ecfeff;
  background: rgba(255, 255, 255, .14);
  border: 1rpx solid rgba(255, 255, 255, .18);
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 900;
}

.notice-card,
.form-card,
.binding-card,
.empty-card {
  margin-top: 26rpx;
  padding: 28rpx;
  background: #fff;
  border: 1rpx solid #dfeae7;
  border-radius: 28rpx;
  box-shadow: 0 18rpx 44rpx rgba(15, 23, 42, .05);
}

.notice-title,
.binding-title {
  display: block;
  color: #172033;
  font-size: 30rpx;
  font-weight: 900;
}

.notice-copy,
.binding-room,
.binding-meta {
  display: block;
  margin-top: 8rpx;
  color: #64748b;
  font-size: 24rpx;
  line-height: 1.45;
}

.audit-result {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.55;
}

.audit-result.approved {
  color: #0f766e;
}

.audit-result.rejected {
  color: #b91c1c;
}

.card-title,
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
  color: #172033;
  font-size: 30rpx;
  font-weight: 900;
}

.card-title text:last-child,
.section-head text:last-child {
  color: #64748b;
  font-size: 23rpx;
  font-weight: 800;
}

.select-field,
.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 92rpx;
  margin-bottom: 18rpx;
  padding: 0 24rpx;
  background: #f8fafc;
  border: 1rpx solid #d7e1df;
  border-radius: 20rpx;
  box-sizing: border-box;
}

.select-field.disabled {
  opacity: .55;
}

.label {
  display: block;
  color: #64748b;
  font-size: 23rpx;
  font-weight: 800;
}

.value {
  display: block;
  margin-top: 6rpx;
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
}

.arrow {
  color: #94a3b8;
  font-size: 48rpx;
}

.field {
  display: block;
  padding: 16rpx 24rpx;
}

.field input {
  height: 48rpx;
  margin-top: 8rpx;
  color: #172033;
  font-size: 29rpx;
}

.actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 26rpx;
}

button {
  height: 78rpx;
  border-radius: 999rpx;
  font-size: 27rpx;
  font-weight: 900;
}

.primary {
  color: #fff;
  background: #0f766e;
}

.primary.full {
  width: 100%;
  margin-top: 24rpx;
}

.secondary {
  color: #172033;
  background: #e8f0f2;
}

.section {
  margin-top: 30rpx;
}

.binding-card {
  position: relative;
  padding-right: 150rpx;
}

.status {
  position: absolute;
  top: 28rpx;
  right: 28rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  color: #475569;
  background: #e2e8f0;
  font-size: 22rpx;
  font-weight: 900;
}

.status.approved {
  color: #166534;
  background: #dcfce7;
}

.status.pending {
  color: #92400e;
  background: #fef3c7;
}

.status.rejected {
  color: #991b1b;
  background: #fee2e2;
}

.empty-card {
  color: #64748b;
  text-align: center;
  font-size: 26rpx;
}
</style>
