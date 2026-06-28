<template>
  <section class="page-shell">
    <div class="page-head">
      <div>
        <h1>报表中心</h1>
        <p>收费、工单、巡检、租赁经营汇总</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-form class="filter-bar" :inline="true">
      <el-form-item label="小区名称">
        <el-select v-model="filters.projectId" clearable filterable placeholder="小区名称">
          <el-option v-for="option in projectOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker v-model="filters.startDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker v-model="filters.endDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
    </el-form>

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
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { fetchPage } from '@/api/admin'

const loading = ref(false)
const error = ref('')
const data = ref<Record<string, any> | null>(null)
const filters = reactive({ projectId: '', startDate: '', endDate: '' })
const projectOptions = ref<Array<{ label: string; value: string }>>([])

const metrics = computed(() => {
  const value = data.value ?? {}
  return [
    { label: '应收金额', value: money(value.fee?.receivableAmount) },
    { label: '净入账', value: money(value.fee?.netPaidAmount) },
    { label: '欠费金额', value: money(value.fee?.arrearsAmount) },
    { label: '工单完成率', value: percent(value.workOrder?.completionRate) },
    { label: '巡检完成率', value: percent(value.patrol?.completionRate) },
    { label: '出租率', value: percent(value.lease?.occupancyRate) },
    { label: '活跃合同', value: value.lease?.activeContractCount ?? 0 },
    { label: '到期合同', value: value.lease?.expiringContractCount ?? 0 },
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''))
    const response = await fetchPage('/reports', params)
    data.value = response.data.data
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadProjects() {
  const response = await fetchPage('/base/projects', { pageNo: 1, pageSize: 200 })
  const payload = response.data.data
  const records = Array.isArray(payload) ? payload : payload?.records ?? []
  projectOptions.value = records.map((item: Record<string, any>) => ({
    label: item.projectName ?? item.name ?? String(item.projectId ?? ''),
    value: String(item.projectId),
  }))
}

function money(value: unknown) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function percent(value: unknown) {
  return `${(Number(value ?? 0) * 100).toFixed(2)}%`
}

onMounted(() => {
  loadProjects()
  load()
})
</script>
