import axios from 'axios'
import { useAuthStore } from '@/store/auth'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const auth = useAuthStore()
    const status = error.response?.status
    const message = error.response?.data?.message ?? error.message ?? '请求失败'
    if (status === 401) {
      auth.clearSession()
      window.location.href = '/login'
    }
    return Promise.reject(new Error(message))
  },
)
