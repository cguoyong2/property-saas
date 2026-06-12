<template>
  <section class="page-shell">
    <div class="page-head">
      <div>
        <h1>{{ config.title }}</h1>
        <p>{{ config.group }}</p>
      </div>
      <el-button
        v-if="config.createPath && canCreate"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        新增
      </el-button>
    </div>

    <el-form class="filter-bar" :inline="true" @submit.prevent>
      <el-form-item v-if="config.projectScoped" label="项目ID">
        <el-input v-model="filters.projectId" clearable placeholder="项目ID" />
      </el-form-item>
      <el-form-item v-for="field in filterFields" :key="field.prop" :label="field.label">
        <el-input v-model="filters[field.prop]" clearable :placeholder="field.label" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
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

    <el-table v-loading="loading" :data="records" border class="data-table">
      <el-table-column
        v-for="column in config.columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        min-width="132"
        show-overflow-tooltip
      />
      <el-table-column v-if="config.updatePath && canUpdate" label="操作" width="112" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="编辑" placement="top">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)" />
          </el-tooltip>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="load"
        @size-change="load"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑' : '新增'" width="620px">
      <el-form label-position="top">
        <el-form-item v-for="field in formFields" :key="field.prop" :label="field.label" :required="field.required">
          <el-select v-if="field.type === 'select'" v-model="form[field.prop]" clearable class="form-control">
            <el-option v-for="option in field.options ?? []" :key="option" :label="option" :value="option" />
          </el-select>
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" class="form-control" />
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form[field.prop]"
            type="date"
            value-format="YYYY-MM-DD"
            class="form-control"
          />
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="form[field.prop]"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="form-control"
          />
          <el-input v-else-if="field.type === 'textarea'" v-model="form[field.prop]" type="textarea" :rows="4" />
          <el-input v-else v-model="form[field.prop]" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { createRecord, fetchPage, updateRecord } from '@/api/admin'
import { allPages, type PageConfig } from '@/config/pages'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const records = ref<Record<string, unknown>[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const filters = reactive<Record<string, string>>({})
const form = reactive<Record<string, string | number | undefined | null>>({})
const dialogVisible = ref(false)
const editingId = ref<string | number | null>(null)

const config = computed<PageConfig>(() => {
  const page = allPages.find((item) => item.key === route.meta.pageKey)
  if (!page) {
    throw new Error('页面配置不存在')
  }
  return page
})

const filterFields = computed(() => config.value.columns.filter((field) => field.inFilter))
const formFields = computed(() => config.value.fields ?? [])
const canCreate = computed(() => !config.value.createPermission || auth.hasPermission(config.value.createPermission))
const canUpdate = computed(() => !config.value.updatePermission || auth.hasPermission(config.value.updatePermission))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params: Record<string, string | number> = { pageNo: pageNo.value, pageSize: pageSize.value }
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== '') params[key] = value
    })
    const { data } = await fetchPage(config.value.listPath, params)
    const payload = data.data
    if (Array.isArray(payload)) {
      records.value = payload
      total.value = payload.length
    } else if (Array.isArray(payload?.records)) {
      records.value = payload.records
      total.value = payload.total ?? payload.records.length
    } else if (Array.isArray(payload?.items)) {
      records.value = payload.items
      total.value = payload.total ?? payload.items.length
    } else if (payload && typeof payload === 'object') {
      records.value = [payload]
      total.value = 1
    } else {
      records.value = []
      total.value = 0
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.keys(filters).forEach((key) => {
    filters[key] = ''
  })
  pageNo.value = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.keys(form).forEach((key) => delete form[key])
  formFields.value.forEach((field) => {
    form[field.prop] = undefined
  })
  dialogVisible.value = true
}

function openEdit(row: Record<string, unknown>) {
  editingId.value = row[config.value.idProp ?? 'id'] as string | number
  Object.keys(form).forEach((key) => delete form[key])
  formFields.value.forEach((field) => {
    form[field.prop] = row[field.prop] as string | number | undefined | null
  })
  dialogVisible.value = true
}

async function save() {
  if (!config.value.createPath) return
  saving.value = true
  try {
    const payload = Object.fromEntries(Object.entries(form).filter(([, value]) => value !== undefined && value !== ''))
    if (editingId.value && config.value.updatePath && config.value.idProp) {
      await updateRecord(config.value.updatePath, editingId.value, config.value.idProp, payload)
    } else {
      await createRecord(config.value.createPath, payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

watch(
  () => route.meta.pageKey,
  () => {
    pageNo.value = 1
    reset()
  },
)

onMounted(load)
</script>
