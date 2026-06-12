<template>
  <section class="page-shell">
    <div class="page-head">
      <div>
        <h1>工作台</h1>
        <p>经营概览、收费、工单和租赁摘要</p>
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
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { fetchPage } from '@/api/admin'

const loading = ref(false)
const error = ref('')
const data = ref<Record<string, any> | null>(null)

const metrics = computed(() => {
  const value = data.value ?? {}
  return [
    { label: '应收金额', value: money(value.fee?.receivableAmount) },
    { label: '欠费金额', value: money(value.fee?.arrearsAmount) },
    { label: '工单总数', value: value.workOrder?.totalCount ?? 0 },
    { label: '工单完成率', value: percent(value.workOrder?.completionRate) },
    { label: '巡检完成率', value: percent(value.patrol?.completionRate) },
    { label: '租赁出租率', value: percent(value.lease?.occupancyRate) },
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchPage('/reports')
    data.value = response.data.data
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function money(value: unknown) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function percent(value: unknown) {
  return `${(Number(value ?? 0) * 100).toFixed(2)}%`
}

onMounted(load)
</script>
