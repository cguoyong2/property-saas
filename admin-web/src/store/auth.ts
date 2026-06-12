import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('admin_token') ?? '',
    userType: localStorage.getItem('admin_user_type') ?? '',
    realName: localStorage.getItem('admin_real_name') ?? '',
    permissions: JSON.parse(localStorage.getItem('admin_permissions') ?? '[]') as string[],
  }),
  actions: {
    setSession(token: string, permissions: string[], userType: string, realName: string) {
      this.token = token
      this.permissions = permissions
      this.userType = userType
      this.realName = realName
      localStorage.setItem('admin_token', token)
      localStorage.setItem('admin_permissions', JSON.stringify(permissions))
      localStorage.setItem('admin_user_type', userType)
      localStorage.setItem('admin_real_name', realName)
    },
    clearSession() {
      this.token = ''
      this.permissions = []
      this.userType = ''
      this.realName = ''
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_permissions')
      localStorage.removeItem('admin_user_type')
      localStorage.removeItem('admin_real_name')
    },
    hasPermission(permission: string) {
      return this.permissions.includes(permission)
    },
  },
})
