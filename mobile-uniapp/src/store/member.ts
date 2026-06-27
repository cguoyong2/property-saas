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
    currentAllowNotice: uni.getStorageSync('current_allow_notice') !== 'false',
    currentAllowBill: uni.getStorageSync('current_allow_bill') !== 'false',
    currentAllowPayment: uni.getStorageSync('current_allow_payment') !== 'false',
    currentAllowWorkOrder: uni.getStorageSync('current_allow_work_order') !== 'false',
    currentAllowVisitor: uni.getStorageSync('current_allow_visitor') !== 'false',
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
      allowNotice?: boolean
      allowBill?: boolean
      allowPayment?: boolean
      allowWorkOrder?: boolean
      allowVisitor?: boolean
    }) {
      const isOwner = context.bindRole === 'OWNER'
      this.currentTenantId = context.tenantId
      this.currentProjectId = context.projectId
      this.currentHouseId = context.houseId
      this.currentHouseNo = context.houseNo ?? ''
      this.currentBindRole = context.bindRole
      this.currentAllowNotice = isOwner || context.allowNotice !== false
      this.currentAllowBill = isOwner || context.allowBill !== false
      this.currentAllowPayment = isOwner || context.allowPayment !== false
      this.currentAllowWorkOrder = isOwner || context.allowWorkOrder !== false
      this.currentAllowVisitor = isOwner || context.allowVisitor !== false
      uni.setStorageSync('current_tenant_id', context.tenantId)
      uni.setStorageSync('current_project_id', context.projectId)
      uni.setStorageSync('current_house_id', context.houseId)
      uni.setStorageSync('current_house_no', context.houseNo ?? '')
      uni.setStorageSync('current_bind_role', context.bindRole)
      uni.setStorageSync('current_allow_notice', String(this.currentAllowNotice))
      uni.setStorageSync('current_allow_bill', String(this.currentAllowBill))
      uni.setStorageSync('current_allow_payment', String(this.currentAllowPayment))
      uni.setStorageSync('current_allow_work_order', String(this.currentAllowWorkOrder))
      uni.setStorageSync('current_allow_visitor', String(this.currentAllowVisitor))
    },
    clearCurrentHouse() {
      this.currentProjectId = undefined
      this.currentHouseId = undefined
      this.currentHouseNo = ''
      this.currentBindRole = ''
      this.currentAllowNotice = true
      this.currentAllowBill = true
      this.currentAllowPayment = true
      this.currentAllowWorkOrder = true
      this.currentAllowVisitor = true
      ;[
        'current_project_id',
        'current_house_id',
        'current_house_no',
        'current_bind_role',
        'current_allow_notice',
        'current_allow_bill',
        'current_allow_payment',
        'current_allow_work_order',
        'current_allow_visitor',
      ].forEach((key) => {
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
      this.currentAllowNotice = true
      this.currentAllowBill = true
      this.currentAllowPayment = true
      this.currentAllowWorkOrder = true
      this.currentAllowVisitor = true
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
        'current_allow_notice',
        'current_allow_bill',
        'current_allow_payment',
        'current_allow_work_order',
        'current_allow_visitor',
      ].forEach((key) => uni.removeStorageSync(key))
    },
  },
})
