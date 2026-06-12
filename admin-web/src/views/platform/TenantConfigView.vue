<template>
  <section class="page-shell">
    <div class="page-head">
      <div>
        <h1>租户配置</h1>
        <p>平台运营</p>
      </div>
      <el-button type="primary" :icon="Search" :loading="loading" @click="load">读取配置</el-button>
    </div>

    <el-form class="filter-bar" :inline="true" @submit.prevent>
      <el-form-item label="租户ID">
        <el-input v-model="tenantId" clearable placeholder="请输入租户ID" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" :loading="loading" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="error"
      class="page-alert"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />

    <el-card v-if="loaded" shadow="never" class="config-card">
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="Logo URL">
              <el-input v-model="form.logoUrl" placeholder="https://..." />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="客服电话">
              <el-input v-model="form.servicePhone" placeholder="400 或项目服务电话" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="独立域名">
              <el-input v-model="form.domain" placeholder="tenant.example.com" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="微信 AppID">
              <el-input v-model="form.wechatAppid" placeholder="wx..." />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="短信通道编码">
              <el-input v-model="form.smsChannelCode" placeholder="ALIYUN_SMS" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="存储策略">
              <el-select v-model="form.storagePolicy" clearable class="form-control">
                <el-option label="共享桶" value="SHARED_BUCKET" />
                <el-option label="独立桶" value="DEDICATED_BUCKET" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div class="actions">
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存配置</el-button>
      </div>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Check, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { http } from '@/api/http'

const tenantId = ref('')
const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const error = ref('')
const form = reactive({
  logoUrl: '',
  servicePhone: '',
  domain: '',
  wechatAppid: '',
  smsChannelCode: '',
  storagePolicy: 'SHARED_BUCKET',
})

function configPath() {
  const id = tenantId.value.trim()
  if (!id) {
    throw new Error('请先输入租户ID')
  }
  return `/platform/tenants/${id}/config`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.get(configPath())
    Object.assign(form, data.data ?? {})
    loaded.value = true
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await http.put(configPath(), { ...form })
    ElMessage.success('保存成功')
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.config-card { margin-top: 18px; }
.form-control { width: 100%; }
.actions { display: flex; justify-content: flex-end; margin-top: 8px; }
</style>
