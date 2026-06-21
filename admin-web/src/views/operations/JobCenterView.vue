<template>
  <section class="page-shell">
    <div class="page-head">
      <div>
        <h1>任务中心</h1>
        <p>运维工具</p>
      </div>
    </div>

    <el-form class="filter-bar" :inline="true" @submit.prevent>
      <el-form-item label="单次处理上限">
        <el-input-number v-model="params.limit" :min="1" :max="500" />
      </el-form-item>
      <el-form-item label="合同到期天数">
        <el-input-number v-model="params.days" :min="1" :max="365" />
      </el-form-item>
    </el-form>

    <el-row :gutter="16">
      <el-col v-for="job in jobs" :key="job.path" :xs="24" :md="12" :lg="8">
        <el-card shadow="never" class="job-card">
          <h2>{{ job.title }}</h2>
          <p>{{ job.description }}</p>
          <el-button type="primary" :icon="VideoPlay" :loading="running === job.path" @click="run(job)">
            执行
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="result" shadow="never" class="result-card">
      <template #header>最近执行结果</template>
      <pre>{{ result }}</pre>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { http } from '@/api/http'

interface JobItem {
  title: string
  description: string
  path: string
  params?: () => Record<string, number>
}

const params = reactive({ limit: 100, days: 30 })
const running = ref('')
const result = ref('')

const jobs: JobItem[] = [
  { title: '一键执行', description: '按后端预设顺序触发所有可运行定时任务。', path: '/jobs/run-all' },
  { title: '账单自动生成', description: '按收费标准和收费绑定补齐当前账期应收账单。', path: '/jobs/fee-bill-generate', params: () => ({ limit: params.limit }) },
  { title: '工单 SLA 扫描', description: '扫描超时工单并写入超时事件与提醒。', path: '/jobs/workorder-sla', params: () => ({ limit: params.limit }) },
  { title: '巡检漏检扫描', description: '扫描漏检巡检任务并输出处理数量。', path: '/jobs/patrol-missed', params: () => ({ limit: params.limit }) },
  { title: '合同到期提醒', description: '为临近到期合同生成提醒消息。', path: '/jobs/lease-expire-remind', params: () => ({ days: params.days }) },
  { title: '站内信派发', description: '派发待发送站内信并记录成功/失败数量。', path: '/jobs/message-dispatch', params: () => ({ limit: params.limit }) },
]

async function run(job: JobItem) {
  running.value = job.path
  try {
    const { data } = await http.post(job.path, {}, { params: job.params?.() })
    result.value = JSON.stringify(data.data ?? data, null, 2)
    ElMessage.success('任务已执行')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '执行失败')
  } finally {
    running.value = ''
  }
}
</script>

<style scoped>
.job-card { min-height: 168px; margin-bottom: 16px; }
.job-card h2 { margin: 0 0 10px; font-size: 18px; }
.job-card p { min-height: 44px; margin: 0 0 18px; color: #64748b; line-height: 1.6; }
.result-card { margin-top: 4px; }
pre { max-height: 360px; overflow: auto; margin: 0; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 13px; }
</style>
