<template>
  <section class="page-shell">
    <div class="page-head">
      <div>
        <h1>平台运营报表</h1>
        <p>租户、项目、账号和接口健康汇总</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="error" class="page-alert" :title="error" type="error" show-icon :closable="false" />

    <div v-loading="loading" class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-tile">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { fetchPage } from '@/api/admin'

const loading = ref(false)
const error = ref('')
const data = ref<Record<string, number> | null>(null)

const metrics = computed(() => [
  { label: '租户总数', value: data.value?.tenantCount ?? 0 },
  { label: '活跃租户', value: data.value?.activeTenantCount ?? 0 },
  { label: '即将到期', value: data.value?.expiringTenantCount ?? 0 },
  { label: '项目总数', value: data.value?.projectCount ?? 0 },
  { label: '房屋总数', value: data.value?.houseCount ?? 0 },
  { label: '后台账号', value: data.value?.userCount ?? 0 },
  { label: '小程序用户', value: data.value?.memberCount ?? 0 },
  { label: '接口失败', value: data.value?.interfaceFailureCount ?? 0 },
])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchPage('/report/platform/summary')
    data.value = response.data.data
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
