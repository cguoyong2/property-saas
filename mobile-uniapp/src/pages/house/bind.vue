<template>
  <view class="page">
    <view class="screen-title">
      <view>
        <text class="page-title">房屋绑定</text>
        <text class="page-sub">审核通过后开启房屋服务</text>
      </view>
      <text class="pill">流程式</text>
    </view>

    <view class="hero">
      <text class="hero-title">绑定我的房屋</text>
      <text class="hero-copy">绑定成功后可接收对应房屋通知，查看账单并办理缴费、报修、访客和家属管理。</text>
      <text class="hero-meta">审核通过后生效</text>
    </view>

    <view class="steps">
      <view class="step">
        <text class="step-num">1</text>
        <view>
          <text class="step-title">选择小区</text>
          <text class="step-copy">支持按小区名称搜索，默认展示当前物业服务项目。</text>
        </view>
      </view>
      <view class="step">
        <text class="step-num">2</text>
        <view>
          <text class="step-title">填写房屋</text>
          <text class="step-copy">按楼栋、单元、房号逐步选择，减少手输错误。</text>
        </view>
      </view>
      <view class="step">
        <text class="step-num">3</text>
        <view>
          <text class="step-title">选择身份</text>
          <text class="step-copy">业主、家属、租户、住户，不同身份可配置不同权限。</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text>绑定信息</text>
        <text>示例项目</text>
      </view>
      <view class="form-card">
        <view class="field">
          <text>租户/物业公司</text>
          <input v-model="form.tenantId" type="number" placeholder="租户ID" />
        </view>
        <view class="field">
          <text>小区/项目</text>
          <input v-model="form.projectId" type="number" placeholder="项目ID" />
        </view>
        <view class="field">
          <text>房屋</text>
          <input v-model="form.houseId" type="number" placeholder="房屋ID" />
        </view>
        <picker :range="roles" @change="form.bindRole = roles[Number($event.detail.value)]">
          <view class="select-field">
            <text>身份</text>
            <text>{{ roleLabel }}</text>
          </view>
        </picker>
        <view class="field">
          <text>姓名</text>
          <input v-model="form.realName" placeholder="请输入姓名" />
        </view>
        <view class="field">
          <text>手机号</text>
          <input v-model="form.mobile" placeholder="请输入手机号" />
        </view>
        <view class="actions">
          <button class="primary" :loading="loading" @click="submit">提交审核</button>
          <button class="secondary" @click="goHome">稍后再说</button>
        </view>
      </view>
    </view>
    <AppTabBar active="service" />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import { applyHouseBinding, wxLogin } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const roles = ['OWNER', 'FAMILY', 'TENANT', 'RESIDENT']
const roleText: Record<string, string> = {
  OWNER: '业主',
  FAMILY: '家属',
  TENANT: '租户',
  RESIDENT: '住户',
}
const loading = ref(false)
const form = reactive({
  tenantId: member.currentTenantId ? String(member.currentTenantId) : '1',
  projectId: '',
  houseId: '',
  bindRole: 'OWNER',
  realName: member.realName,
  mobile: member.mobile,
})
const roleLabel = computed(() => roleText[form.bindRole] ?? form.bindRole)
const isH5Preview = typeof window !== 'undefined' && Boolean((window as unknown as { __PROPERTY_SAAS_H5_PREVIEW__?: boolean }).__PROPERTY_SAAS_H5_PREVIEW__)

onMounted(async () => {
  if (!member.token && isH5Preview) {
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
    } catch (error) {
      uni.showToast({ title: error instanceof Error ? error.message : '身份初始化失败', icon: 'none' })
    }
  }
})

async function submit() {
  if (!member.token) {
    uni.showToast({ title: '请先完成微信授权', icon: 'none' })
    return
  }
  if (!form.tenantId || !form.projectId || !form.houseId || !form.realName || !form.mobile) {
    uni.showToast({ title: '请补全绑定信息', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await applyHouseBinding({
      tenantId: Number(form.tenantId),
      memberId: member.memberId,
      projectId: Number(form.projectId),
      houseId: Number(form.houseId),
      bindRole: form.bindRole,
      realName: form.realName,
      mobile: form.mobile,
    })
    uni.showToast({ title: '已提交' })
    uni.navigateTo({ url: '/pages/house/list' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goHome() {
  uni.switchTab({ url: '/pages/home/index' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% -2%, rgba(15, 118, 110, .18), transparent 35%),
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

.hero-meta {
  display: inline-flex;
  height: 58rpx;
  margin-top: 24rpx;
  padding: 0 20rpx;
  color: #ecfeff;
  background: rgba(255, 255, 255, .14);
  border: 1rpx solid rgba(255, 255, 255, .18);
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 58rpx;
}

.steps {
  display: grid;
  gap: 18rpx;
  margin-top: 28rpx;
}

.step {
  display: grid;
  grid-template-columns: 56rpx 1fr;
  gap: 18rpx;
  padding: 24rpx;
  background: #fff;
  border: 1rpx solid #e2ece9;
  border-radius: 26rpx;
}

.step-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  color: #fff;
  background: #0f766e;
  border-radius: 50%;
  font-size: 25rpx;
  font-weight: 900;
}

.step-title {
  display: block;
  color: #172033;
  font-size: 27rpx;
  font-weight: 900;
}

.step-copy {
  display: block;
  margin-top: 7rpx;
  color: #65758a;
  font-size: 22rpx;
  line-height: 1.45;
}

.section {
  margin-top: 28rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  margin: 0 4rpx 18rpx;
}

.section-head text:first-child {
  color: #172033;
  font-size: 31rpx;
  font-weight: 900;
}

.section-head text:last-child {
  color: #65758a;
  font-size: 23rpx;
  font-weight: 800;
}

.form-card {
  display: grid;
  gap: 18rpx;
  padding: 28rpx;
  background: rgba(255, 255, 255, .96);
  border: 1rpx solid #dfe9e6;
  border-radius: 28rpx;
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, .055);
}

.field,
.select-field {
  min-height: 82rpx;
  padding: 0 22rpx;
  background: #f8fafc;
  border: 1rpx solid #dbe6e3;
  border-radius: 18rpx;
}

.field {
  display: grid;
  grid-template-columns: 150rpx 1fr;
  align-items: center;
  gap: 16rpx;
}

.field text,
.select-field text:first-child {
  color: #65758a;
  font-size: 23rpx;
  font-weight: 800;
}

.field input {
  color: #172033;
  font-size: 27rpx;
  font-weight: 800;
}

.select-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.select-field text:last-child {
  color: #172033;
  font-size: 27rpx;
  font-weight: 900;
}

.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 8rpx;
}

.actions button {
  flex: 1;
  height: 76rpx;
  border-radius: 999rpx;
  font-size: 27rpx;
  font-weight: 900;
}

.primary {
  color: #fff;
  background: #0f766e;
}

.secondary {
  color: #334155;
  background: #e8f0f2;
}
</style>
