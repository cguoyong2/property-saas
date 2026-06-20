<template>
  <el-container class="layout">
    <el-aside width="232px" class="layout__aside">
      <div class="layout__brand">智慧物业 SaaS</div>
      <el-menu router :default-active="route.path">
        <el-menu-item v-if="auth.userType !== 'PLATFORM'" index="/dashboard">
          <el-icon><House /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item
          v-for="link in visibleReportLinks"
          :key="link.route"
          :index="link.route"
        >
          <el-icon><component :is="link.icon" /></el-icon>
          <span>{{ link.label }}</span>
        </el-menu-item>
        <el-sub-menu v-for="group in menuGroups" :key="group.name" :index="group.name">
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.name }}</span>
          </template>
          <el-menu-item v-for="item in group.items" :key="item.route" :index="item.route">
            {{ item.title }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout__header">
        <span>{{ auth.userType === 'PLATFORM' ? '平台运营端' : '物业管理端' }}</span>
        <el-button text :icon="SwitchButton" @click="logout">退出</el-button>
      </el-header>
      <el-main ref="mainRef" class="layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House, OfficeBuilding, SwitchButton } from '@element-plus/icons-vue'
import { allPages, reportLinks } from '@/config/pages'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mainRef = ref<HTMLElement>()

const visibleReportLinks = computed(() => reportLinks.filter((item) => auth.hasPermission(item.permission)))

const menuGroups = computed(() => {
  const groups = new Map<string, { name: string; icon: unknown; items: typeof allPages }>()
  allPages
    .filter((item) => auth.hasPermission(item.permission))
    .forEach((item) => {
      if (!groups.has(item.group)) {
        groups.set(item.group, { name: item.group, icon: item.icon ?? OfficeBuilding, items: [] })
      }
      groups.get(item.group)?.items.push(item)
    })
  groups.forEach((group) => {
    group.items.sort((a, b) => (a.menuOrder ?? 9999) - (b.menuOrder ?? 9999))
  })
  return Array.from(groups.values())
})

function logout() {
  auth.clearSession()
  router.push('/login')
}

watch(
  () => route.fullPath,
  () => {
    requestAnimationFrame(() => {
      mainRef.value?.scrollTo({ top: 0, left: 0 })
      window.scrollTo({ top: 0, left: 0 })
    })
  },
)
</script>
