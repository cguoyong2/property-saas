import { createRouter, createWebHistory } from 'vue-router'
import { allPages } from '@/config/pages'
import { useAuthStore } from '@/store/auth'

const BasicLayout = () => import('@/views/layout/BasicLayout.vue')
const LoginView = () => import('@/views/auth/LoginView.vue')
const DashboardView = () => import('@/views/dashboard/DashboardView.vue')
const GenericListView = () => import('@/views/common/GenericListView.vue')
const ReportCenterView = () => import('@/views/reports/ReportCenterView.vue')
const PlatformReportView = () => import('@/views/platform/PlatformReportView.vue')
const PlatformMonitorView = () => import('@/views/platform/PlatformMonitorView.vue')
const TenantConfigView = () => import('@/views/platform/TenantConfigView.vue')
const JobCenterView = () => import('@/views/operations/JobCenterView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: BasicLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: DashboardView },
        { path: 'reports', name: 'reports', component: ReportCenterView, meta: { permission: 'report:center:view' } },
        {
          path: 'report/platform',
          name: 'platform-report',
          component: PlatformReportView,
          meta: { permission: 'report:platform:view' },
        },
        {
          path: 'platform/monitor',
          name: 'platform-monitor',
          component: PlatformMonitorView,
          meta: { permission: 'platform:monitor:view' },
        },
        {
          path: 'platform/tenant-config',
          name: 'platform-tenant-config',
          component: TenantConfigView,
          meta: { permission: 'platform:tenant:config:view' },
        },
        {
          path: 'jobs',
          name: 'job-center',
          component: JobCenterView,
          meta: { permission: 'job:run' },
        },
        ...allPages.map((page) => ({
          path: page.route.replace(/^\//, ''),
          name: page.key,
          component: GenericListView,
          meta: { pageKey: page.key, permission: page.permission },
          props: { pageKey: page.key },
        })),
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) {
    return '/login'
  }
  const permission = to.meta.permission as string | undefined
  if (permission && !auth.hasPermission(permission)) {
    return auth.userType === 'PLATFORM' ? '/platform/tenants' : '/dashboard'
  }
  return true
})

export default router
