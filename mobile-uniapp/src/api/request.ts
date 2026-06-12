export const API_BASE_URL = ((import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL ?? 'http://localhost:8080/api')

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export function request<T>(options: UniApp.RequestOptions) {
  return new Promise<T>((resolve, reject) => {
    const token = uni.getStorageSync('member_token')
    const projectId = uni.getStorageSync('current_project_id')
    uni.request({
      ...options,
      header: {
        ...(options.header ?? {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(projectId ? { 'X-Project-Id': String(projectId) } : {}),
      },
      url: `${API_BASE_URL}${options.url}`,
      success: (response) => {
        const body = response.data as ApiEnvelope<T>
        if (response.statusCode === 401) {
          uni.removeStorageSync('member_token')
          uni.navigateTo({ url: '/pages/login/index' })
          reject(new Error('登录已失效'))
          return
        }
        if (body && typeof body.code === 'number' && body.code !== 0) {
          reject(new Error(body.message || '请求失败'))
          return
        }
        resolve((body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : response.data) as T)
      },
      fail: reject,
    })
  })
}

export function authHeaders(extra: Record<string, string> = {}) {
  const token = uni.getStorageSync('member_token')
  const projectId = uni.getStorageSync('current_project_id')
  return {
    ...extra,
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(projectId ? { 'X-Project-Id': String(projectId) } : {}),
  }
}
