import { defineStore } from 'pinia'

export const useMemberStore = defineStore('member', {
  state: () => ({
    token: uni.getStorageSync('member_token') || '',
    memberId: Number(uni.getStorageSync('member_id')) || undefined,
    mobile: uni.getStorageSync('member_mobile') || '',
    realName: uni.getStorageSync('member_real_name') || '',
    currentTenantId: Number(uni.getStorageSync('current_tenant_id')) || undefined,
    currentProjectId: Number(uni.getStorageSync('current_project_id')) || undefined,
    currentHouseId: Number(uni.getStorageSync('current_house_id')) || undefined,
    currentHouseNo: uni.getStorageSync('current_house_no') || '',
    currentBindRole: uni.getStorageSync('current_bind_role') || '',
  }),
  actions: {
    setSession(session: Record<string, unknown>) {
      this.token = String(session.token ?? '')
      this.memberId = Number(session.memberId)
      this.currentTenantId = Number(session.tenantId)
      this.mobile = String(session.mobile ?? '')
      this.realName = String(session.realName ?? '')
      uni.setStorageSync('member_token', this.token)
      uni.setStorageSync('member_id', this.memberId)
      uni.setStorageSync('current_tenant_id', this.currentTenantId)
      uni.setStorageSync('member_mobile', this.mobile)
      uni.setStorageSync('member_real_name', this.realName)
    },
    setCurrentHouse(context: {
      tenantId: number
      projectId: number
      houseId: number
      houseNo?: string
      bindRole: string
    }) {
      this.currentTenantId = context.tenantId
      this.currentProjectId = context.projectId
      this.currentHouseId = context.houseId
      this.currentHouseNo = context.houseNo ?? ''
      this.currentBindRole = context.bindRole
      uni.setStorageSync('current_tenant_id', context.tenantId)
      uni.setStorageSync('current_project_id', context.projectId)
      uni.setStorageSync('current_house_id', context.houseId)
      uni.setStorageSync('current_house_no', context.houseNo ?? '')
      uni.setStorageSync('current_bind_role', context.bindRole)
    },
    clearCurrentHouse() {
      this.currentProjectId = undefined
      this.currentHouseId = undefined
      this.currentHouseNo = ''
      this.currentBindRole = ''
      ;['current_project_id', 'current_house_id', 'current_house_no', 'current_bind_role'].forEach((key) => {
        uni.removeStorageSync(key)
      })
    },
    clearSession() {
      this.token = ''
      this.memberId = undefined
      this.mobile = ''
      this.realName = ''
      this.currentTenantId = undefined
      this.currentProjectId = undefined
      this.currentHouseId = undefined
      this.currentHouseNo = ''
      this.currentBindRole = ''
      ;[
        'member_token',
        'member_id',
        'member_mobile',
        'member_real_name',
        'current_tenant_id',
        'current_project_id',
        'current_house_id',
        'current_house_no',
        'current_bind_role',
      ].forEach((key) => uni.removeStorageSync(key))
    },
  },
})
