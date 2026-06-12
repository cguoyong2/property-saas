<template>
  <section class="page-shell">
    <div class="page-head">
      <div>
        <h1>运维监控</h1>
        <p>接口异常、消息失败、支付退款异常和登录失败摘要</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="error" class="page-alert" :title="error" type="error" show-icon :closable="false" />

    <div v-loading="loading" class="metric-grid">
      <div v-for="metric in metrics" :key="metric.label" class="metric-tile">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </div>
    </div>

    <el-table v-loading="loading" class="data-table" :data="alerts" border>
      <el-table-column prop="metricCode" label="指标" min-width="180" />
      <el-table-column prop="level" label="级别" width="120">
        <template #default="{ row }">
          <el-tag :type="tagType(row.level)">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="count" label="数量" width="120" />
      <el-table-column prop="message" label="说明" min-width="260" />
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { fetchPage } from '@/api/admin'

interface MonitorAlert {
  metricCode: string
  level: string
  count: number
  message: string
}

interface PlatformMonitor {
  generatedAt?: string
  interfaceFailureCount?: number
  interfaceRetryPendingCount?: number
  messageFailedCount?: number
  messagePendingCount?: number
  paymentFailedCount?: number
  refundFailedCount?: number
  loginFailedCount?: number
  highRiskOperationCount?: number
  alerts?: MonitorAlert[]
}

const loading = ref(false)
const error = ref('')
const data = ref<PlatformMonitor>({})

const metrics = computed(() => [
  { label: '接口失败', value: number(data.value.interfaceFailureCount) },
  { label: '待重试接口', value: number(data.value.interfaceRetryPendingCount) },
  { label: '消息失败', value: number(data.value.messageFailedCount) },
  { label: '待发消息', value: number(data.value.messagePendingCount) },
  { label: '支付异常', value: number(data.value.paymentFailedCount) },
  { label: '退款异常', value: number(data.value.refundFailedCount) },
  { label: '登录失败', value: number(data.value.loginFailedCount) },
  { label: '高风险操作', value: number(data.value.highRiskOperationCount) },
])

const alerts = computed(() => data.value.alerts ?? [])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchPage('/platform/monitor')
    data.value = response.data.data ?? {}
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function number(value: unknown) {
  return Number(value ?? 0)
}

function tagType(level: string) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

onMounted(load)
</script>
