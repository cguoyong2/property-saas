import { createApp } from 'vue'
import { createPinia } from 'pinia'
import H5PreviewApp from './h5-preview/App.vue'

type UniRequestOptions = {
  url: string
  method?: string
  data?: Record<string, unknown>
  header?: Record<string, string>
  success?: (response: { statusCode: number, data: unknown }) => void
  fail?: (error: unknown) => void
}

type UniRouteOptions = {
  url: string
}

const API_BASE_URL = ((import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL ?? 'http://localhost:19023/api')

function resolveApiUrl(url: string, data?: Record<string, unknown>, method = 'GET') {
  const baseUrl = API_BASE_URL.startsWith('http') ? API_BASE_URL : `${window.location.origin}${API_BASE_URL}`
  const target = url.startsWith('http') ? new URL(url) : new URL(`${baseUrl}${url}`)
  if (method.toUpperCase() === 'GET' && data) {
    Object.entries(data).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        target.searchParams.set(key, String(value))
      }
    })
  }
  return target.toString()
}

function navigate(url: string) {
  window.location.hash = url
  window.dispatchEvent(new CustomEvent('h5-preview:navigate', { detail: { url } }))
}

Object.assign(window, {
  __PROPERTY_SAAS_H5_PREVIEW__: true,
  uni: {
    getStorageSync: (key: string) => localStorage.getItem(key) ?? '',
    setStorageSync: (key: string, value: unknown) => localStorage.setItem(key, String(value ?? '')),
    removeStorageSync: (key: string) => localStorage.removeItem(key),
    request: (options: UniRequestOptions) => {
      const method = (options.method ?? 'GET').toUpperCase()
      fetch(resolveApiUrl(options.url, options.data, method), {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...(options.header ?? {}),
        },
        body: method === 'GET' ? undefined : JSON.stringify(options.data ?? {}),
      })
        .then(async (response) => {
          const text = await response.text()
          const data = text ? JSON.parse(text) : null
          options.success?.({ statusCode: response.status, data })
        })
        .catch((error) => options.fail?.(error))
    },
    navigateTo: ({ url }: UniRouteOptions) => navigate(url),
    redirectTo: ({ url }: UniRouteOptions) => navigate(url),
    reLaunch: ({ url }: UniRouteOptions) => navigate(url),
    switchTab: ({ url }: UniRouteOptions) => navigate(url),
    navigateBack: () => window.history.back(),
    showToast: ({ title }: { title: string }) => {
      window.dispatchEvent(new CustomEvent('h5-preview:toast', { detail: { title } }))
    },
    showModal: ({ title, content, success }: { title?: string, content?: string, success?: (result: { confirm: boolean }) => void }) => {
      success?.({ confirm: window.confirm([title, content].filter(Boolean).join('\n')) })
    },
    chooseImage: ({ fail }: { fail?: (error: unknown) => void }) => {
      fail?.(new Error('浏览器预览暂不支持选择图片'))
    },
    uploadFile: ({ fail }: { fail?: (error: unknown) => void }) => {
      fail?.(new Error('浏览器预览暂不支持文件上传'))
    },
  },
})

createApp(H5PreviewApp).use(createPinia()).mount('#app')
