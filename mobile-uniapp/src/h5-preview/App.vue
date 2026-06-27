<template>
  <view class="preview-shell">
    <component :is="currentComponent" :key="currentPath" />
    <view v-if="toast" class="toast">{{ toast }}</view>
  </view>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref } from 'vue'

const pages = {
  '/pages/login/index': defineAsyncComponent(() => import('@/pages/login/index.vue')),
  '/pages/home/index': defineAsyncComponent(() => import('@/pages/home/index.vue')),
  '/pages/service/index': defineAsyncComponent(() => import('@/pages/service/index.vue')),
  '/pages/house/list': defineAsyncComponent(() => import('@/pages/house/list.vue')),
  '/pages/house/bind': defineAsyncComponent(() => import('@/pages/house/bind.vue')),
  '/pages/house/switch': defineAsyncComponent(() => import('@/pages/house/switch.vue')),
  '/pages/bill/list': defineAsyncComponent(() => import('@/pages/bill/list.vue')),
  '/pages/bill/detail': defineAsyncComponent(() => import('@/pages/bill/detail.vue')),
  '/pages/payment/history': defineAsyncComponent(() => import('@/pages/payment/history.vue')),
  '/pages/payment/receipt': defineAsyncComponent(() => import('@/pages/payment/receipt.vue')),
  '/pages/payment/prepayment': defineAsyncComponent(() => import('@/pages/payment/prepayment.vue')),
  '/pages/workorder/create': defineAsyncComponent(() => import('@/pages/workorder/create.vue')),
  '/pages/workorder/list': defineAsyncComponent(() => import('@/pages/workorder/list.vue')),
  '/pages/workorder/detail': defineAsyncComponent(() => import('@/pages/workorder/detail.vue')),
  '/pages/workorder/complaint': defineAsyncComponent(() => import('@/pages/workorder/complaint.vue')),
  '/pages/notice/list': defineAsyncComponent(() => import('@/pages/notice/list.vue')),
  '/pages/notice/detail': defineAsyncComponent(() => import('@/pages/notice/detail.vue')),
  '/pages/visitor/create': defineAsyncComponent(() => import('@/pages/visitor/create.vue')),
  '/pages/vehicle/list': defineAsyncComponent(() => import('@/pages/vehicle/list.vue')),
  '/pages/lease/contracts': defineAsyncComponent(() => import('@/pages/lease/contracts.vue')),
  '/pages/mine/index': defineAsyncComponent(() => import('@/pages/mine/index.vue')),
  '/pages-sub/payment/cashier': defineAsyncComponent(() => import('@/pages-sub/payment/cashier.vue')),
}

const defaultPath = '/pages/home/index'
const currentPath = ref(normalizePath(window.location.hash.slice(1) || defaultPath))
const toast = ref('')
let toastTimer: number | undefined

const currentComponent = computed(() => {
  return pages[currentPath.value as keyof typeof pages] ?? pages['/pages/home/index']
})

function normalizePath(url: string) {
  const [path] = url.split('?')
  return path.startsWith('/') ? path : `/${path}`
}

function showToast(title: string) {
  toast.value = title
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value = ''
  }, 2200)
}

onMounted(() => {
  window.addEventListener('h5-preview:navigate', ((event: CustomEvent<{ url: string }>) => {
    currentPath.value = normalizePath(event.detail.url)
  }) as EventListener)
  window.addEventListener('hashchange', () => {
    currentPath.value = normalizePath(window.location.hash.slice(1) || defaultPath)
  })
  window.addEventListener('h5-preview:toast', ((event: CustomEvent<{ title: string }>) => {
    showToast(event.detail.title)
  }) as EventListener)
})
</script>

<style>
html,
body,
#app {
  min-height: 100%;
  margin: 0;
  background: #f5f7fb;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}

view,
text,
label,
uni-view,
uni-text,
uni-label {
  box-sizing: border-box;
  display: block;
}

button,
input,
textarea,
uni-button,
uni-input,
uni-textarea {
  box-sizing: border-box;
  font: inherit;
}

uni-button::after {
  display: none;
}

.preview-shell .page {
  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 16px 14px 28px;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 16% -6%, rgba(20, 184, 166, .18), transparent 34%),
    linear-gradient(180deg, #eef7f4 0%, #f8fafc 56%, #f5f7fb 100%);
  box-sizing: border-box;
}

.preview-shell .card,
.preview-shell .panel,
.preview-shell .section,
.preview-shell .notice,
.preview-shell .profile {
  background: rgba(255, 255, 255, .96);
  border: 1px solid #e4efec;
  border-radius: 16px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, .055);
  box-sizing: border-box;
}

.preview-shell .card {
  position: relative;
  margin-bottom: 12px;
  padding: 16px;
  overflow: hidden;
}

.preview-shell .card::before {
  content: "";
  position: absolute;
  top: 16px;
  left: 0;
  width: 3px;
  height: 34px;
  background: #0f766e;
  border-radius: 0 999px 999px 0;
}

.preview-shell .title,
.preview-shell .section-title {
  color: #102a43;
  font-size: 17px;
  font-weight: 800;
  line-height: 1.35;
}

.preview-shell .meta,
.preview-shell .desc,
.preview-shell .content,
.preview-shell .line,
.preview-shell .event,
.preview-shell .current {
  color: #64748b;
  font-size: 13px;
  line-height: 1.65;
}

.preview-shell .status {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  color: #0f766e;
  background: #e6f6f2;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.preview-shell .amount {
  color: #dc2626;
  font-weight: 900;
}

.preview-shell .empty {
  margin-top: 18px;
  padding: 40px 18px;
  color: #94a3b8;
  text-align: center;
  background: rgba(255, 255, 255, .78);
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
}

.preview-shell .input,
.preview-shell .textarea {
  width: 100%;
  color: #1f2937;
  background: #f8fafc;
  border: 1px solid #d9e2e7;
  border-radius: 10px;
  box-sizing: border-box;
  font-size: 15px;
  outline: none;
}

.preview-shell .input {
  min-height: 46px;
  padding: 0 12px;
}

.preview-shell .textarea {
  min-height: 128px;
  padding: 12px;
  line-height: 1.55;
}

.preview-shell .input:focus,
.preview-shell .textarea:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, .12);
}

.preview-shell button,
.preview-shell uni-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: auto;
  margin: 0;
  padding: 0 14px;
  min-height: 40px;
  border: 0;
  border-radius: 999px;
  line-height: 1.2;
  cursor: pointer;
}

.preview-shell .primary,
.preview-shell .actions button:not(.ghost):not(.secondary):not(.danger),
.preview-shell .actions uni-button:not(.ghost):not(.secondary):not(.danger),
.preview-shell .toolbar button:not(.ghost):not(.secondary):not(.danger),
.preview-shell .toolbar uni-button:not(.ghost):not(.secondary):not(.danger),
.preview-shell .panel > button:not(.ghost):not(.secondary):not(.danger),
.preview-shell .panel > uni-button:not(.ghost):not(.secondary):not(.danger) {
  color: #fff;
  background: #0f766e;
  font-weight: 800;
  box-shadow: 0 10px 18px rgba(15, 118, 110, .18);
}

.preview-shell .secondary,
.preview-shell .ghost {
  color: #334155;
  background: #e8f0f2;
  box-shadow: none;
}

.preview-shell .danger {
  color: #b91c1c;
  background: #fee2e2;
  box-shadow: none;
}

.preview-shell .service {
  display: block !important;
  width: 100% !important;
  min-height: 78px !important;
  padding: 10px 5px 8px !important;
  line-height: normal !important;
}

.preview-shell .app-tabbar .tab {
  width: 100% !important;
  min-height: 44px !important;
  padding: 0 !important;
  line-height: normal !important;
}

.preview-shell .hero-button,
.preview-shell .public-actions button,
.preview-shell .owner-hero-button,
.preview-shell .owner-public-actions button,
.preview-shell .total-actions button,
.preview-shell .actions button,
.preview-shell .bind-card button,
.preview-shell .empty-card button,
.preview-shell .switch {
  padding-left: 14px !important;
  padding-right: 14px !important;
  line-height: normal !important;
}

.preview-shell .owner-home .card {
  margin-bottom: 0;
}

.preview-shell .owner-home .card::before {
  display: none;
}

.preview-shell .page > .section,
.preview-shell .page > .owner-section {
  background: transparent;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.preview-shell .page > .section::before,
.preview-shell .page > .owner-section::before {
  display: none;
}

.preview-shell .actions,
.preview-shell .toolbar {
  display: flex;
  gap: 10px;
}

.preview-shell .actions button,
.preview-shell .actions uni-button,
.preview-shell .toolbar button,
.preview-shell .toolbar uni-button {
  min-width: 0;
  flex: 1;
}

.preview-shell .row,
.preview-shell .line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.preview-shell .line {
  padding: 11px 0;
  border-top: 1px solid #edf2f7;
}

.preview-shell .hero {
  position: relative;
  min-height: 150px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #0f766e 0%, #0b8a72 52%, #155e75 100%);
  border-radius: 18px;
  box-shadow: 0 16px 28px rgba(15, 118, 110, .18);
}

.preview-shell .hero::after,
.preview-shell .profile::after {
  content: "";
  position: absolute;
  right: -26px;
  bottom: -34px;
  width: 138px;
  height: 138px;
  border: 1px solid rgba(255, 255, 255, .24);
  border-radius: 50%;
}

.preview-shell .hello,
.preview-shell .name {
  color: #fff;
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 0;
}

.preview-shell .house,
.preview-shell .mobile {
  margin-top: 8px;
  color: #d9fffa;
  font-size: 14px;
}

.preview-shell .switch {
  position: static;
  width: auto;
  min-height: 34px;
  margin-top: 18px;
  padding: 0 14px;
  color: #0f766e;
  background: #fff;
  box-shadow: none;
}

.preview-shell .metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin: 12px 0;
}

.preview-shell .metric {
  min-width: 0;
  padding: 13px 6px;
  background: rgba(255, 255, 255, .96);
  border: 1px solid #e4efec;
  border-radius: 14px;
  text-align: center;
  box-shadow: 0 8px 18px rgba(15, 23, 42, .045);
}

.preview-shell .metric text {
  color: #0f766e;
  font-size: 20px;
  font-weight: 900;
}

.preview-shell .metric label {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.preview-shell .grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.preview-shell .grid button,
.preview-shell .grid uni-button {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  height: 76px;
  padding: 16px 12px;
  color: #102a43;
  background: #fff;
  border: 1px solid #e4efec;
  border-radius: 15px;
  text-align: left;
  box-shadow: 0 8px 18px rgba(15, 23, 42, .045);
}

.preview-shell .grid button::after,
.preview-shell .grid uni-button::after {
  display: block;
  content: "›";
  position: absolute;
  right: 14px;
  top: 50%;
  left: auto;
  width: auto;
  height: auto;
  border: 0;
  color: #0f766e;
  font-size: 22px;
  transform: translateY(-50%);
  transform-origin: center;
}

.preview-shell .profile {
  position: relative;
  padding: 22px 20px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #0f766e 0%, #155e75 100%);
}

.preview-shell .menu {
  margin-top: 14px;
}

.preview-shell .menu button,
.preview-shell .menu uni-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
  min-height: 54px;
  margin-bottom: 10px;
  padding: 0 16px;
  color: #102a43;
  background: #fff;
  border: 1px solid #e4efec;
  border-radius: 14px;
  text-align: left;
  box-shadow: 0 8px 18px rgba(15, 23, 42, .045);
}

.preview-shell .menu button::after,
.preview-shell .menu uni-button::after {
  display: block;
  content: "›";
  position: static;
  width: auto;
  height: auto;
  border: 0;
  color: #0f766e;
  font-size: 22px;
  transform: none;
}

.preview-shell .auth-page {
  background:
    radial-gradient(circle at 18% -8%, rgba(20, 184, 166, .22), transparent 34%),
    #eef7f4;
}

.preview-shell .auth-page {
  min-height: 100vh;
  padding: 40px 16px;
  background: #eef7f4;
}

.preview-shell .brand {
  margin: 24px 0;
}

.preview-shell .brand-title {
  display: block;
  font-size: 26px;
  font-weight: 800;
  color: #0f766e;
}

.preview-shell .brand-sub {
  display: block;
  margin-top: 6px;
  color: #475569;
  font-size: 17px;
}

.preview-shell .panel {
  max-width: 420px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 20px rgba(15, 118, 110, .1);
}

.preview-shell .input {
  display: block;
  width: 100%;
  height: 44px;
  margin-bottom: 10px;
  padding: 0 12px;
  color: #1f2937;
  background: #f8fafc;
  border: 1px solid #d9e2e7;
  border-radius: 6px;
  font-size: 15px;
  outline: none;
}

.preview-shell .input:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, .12);
}

.preview-shell .primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
  height: 44px;
  margin-top: 8px;
  padding: 0 18px;
  color: #fff;
  background: #0f766e;
  border: 0;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}
</style>

<style scoped>
.preview-shell {
  min-height: 100vh;
  background: #f5f7fb;
}

.toast {
  position: fixed;
  left: 50%;
  bottom: 48px;
  z-index: 20;
  transform: translateX(-50%);
  max-width: calc(100vw - 48px);
  padding: 10px 16px;
  color: #fff;
  background: rgba(15, 23, 42, .86);
  border-radius: 8px;
  font-size: 14px;
}
</style>
