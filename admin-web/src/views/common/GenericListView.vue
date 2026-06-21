<template>
  <section class="page-shell">
    <div class="page-head">
      <div>
        <h1>{{ config.title }}</h1>
        <p>{{ config.group }}</p>
      </div>
      <div class="head-actions">
        <el-button
          v-for="action in pageActions"
          :key="action.key"
          :type="action.type ?? 'default'"
          :icon="action.icon"
          @click="runAction(action)"
        >
          {{ action.label }}
        </el-button>
        <el-button
          v-if="config.key === 'buildings' && canCreate"
          type="success"
          :icon="Plus"
          @click="openBulkBuildingDialog"
        >
          批量新增
        </el-button>
        <el-button
          v-if="config.createPath && canCreate"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增
        </el-button>
      </div>
    </div>

    <el-form class="filter-bar" :inline="true" @submit.prevent>
      <el-form-item v-if="config.projectScoped" label="小区名称">
        <el-select v-model="filters.projectId" clearable filterable placeholder="小区名称" class="form-control">
          <el-option
            v-for="option in remoteOptions.projectId"
            :key="String(optionValue(option))"
            :label="optionLabel(option)"
            :value="optionValue(option)"
          />
        </el-select>
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
      >
        <template #default="{ row }">
          {{ displayCell(row, column) }}
        </template>
      </el-table-column>
      <el-table-column v-if="hasOperationColumn" label="操作" :width="operationWidth" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="编辑" placement="top">
            <el-button v-if="config.updatePath && canUpdate" text type="primary" :icon="Edit" @click="openEdit(row)" />
          </el-tooltip>
          <el-button v-if="config.showDetails" text type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            v-for="action in rowActions"
            :key="action.key"
            text
            :type="action.type ?? 'primary'"
            @click="runAction(action, row)"
          >
            {{ action.label }}
          </el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑' : '新增'" width="620px" draggable>
      <section v-if="isParkingSpacePage" class="member-picker">
        <div class="member-picker__head">
          <span>搜索业主/住户</span>
          <small>选择后自动带出小区、楼栋、单元和房屋</small>
        </div>
        <div class="member-picker__search">
          <el-input
            v-model="memberSearchKeyword"
            clearable
            placeholder="输入姓名或手机号"
            @keyup.enter="searchMembers"
          />
          <el-button type="primary" :icon="Search" :loading="memberSearchLoading" @click="searchMembers">搜索</el-button>
        </div>
        <div v-if="memberSearchResults.length" class="member-result-list">
          <button
            v-for="member in memberSearchResults"
            :key="String(member.memberId)"
            class="member-result"
            type="button"
            :disabled="!member.houseId"
            @click="selectParkingMember(member)"
          >
            <span>
              <strong>{{ member.realName || '-' }}</strong>
              <small>{{ member.mobile || '-' }}</small>
            </span>
            <span>
              <strong>{{ member.houseNo || '未绑定房屋' }}</strong>
              <small>{{ bindRoleLabel(member.bindRole) }}</small>
            </span>
          </button>
        </div>
        <div v-if="selectedParkingMember || form.houseId" class="member-card">
          <div>
            <span>业主/住户</span>
            <strong>{{ selectedParkingMember?.realName ?? '-' }}</strong>
          </div>
          <div>
            <span>手机号</span>
            <strong>{{ selectedParkingMember?.mobile ?? '-' }}</strong>
          </div>
          <div>
            <span>房屋</span>
            <strong>{{ selectedParkingMember?.houseNo ?? optionText('house', form.houseId) }}</strong>
          </div>
          <div>
            <span>住户类型</span>
            <strong>{{ bindRoleLabel(selectedParkingMember?.bindRole) }}</strong>
          </div>
        </div>
      </section>
      <el-form label-position="top">
        <el-form-item v-for="field in visibleFormFields" :key="field.prop" :label="field.label" :required="field.required">
          <el-select
            v-if="isSelectField(field)"
            v-model="form[field.prop]"
            clearable
            class="form-control"
            filterable
            :placeholder="field.label"
            @change="handleFieldChange(field)"
          >
            <el-option
              v-for="option in optionsForField(field)"
              :key="String(optionValue(option))"
              :label="optionLabel(option)"
              :value="optionValue(option)"
            />
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
          <el-input
            v-else
            v-model="form[field.prop]"
            :maxlength="isMobileField(field) ? 11 : undefined"
            :inputmode="isMobileField(field) ? 'numeric' : undefined"
            @input="handleTextInput(field, $event)"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="小区详情" width="620px" draggable>
      <el-form v-if="detailRow" label-position="top" class="detail-form">
        <el-form-item v-for="field in detailFields" :key="field.prop" :label="field.label">
          <el-input :model-value="displayDetailCell(detailRow, field)" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bulkBuildingDialogVisible" title="批量新增楼栋/单元" width="720px" draggable>
      <el-form label-position="top">
        <el-form-item label="小区名称" required>
          <el-select v-model="bulkBuildingForm.projectId" clearable filterable class="form-control" placeholder="请选择小区">
            <el-option
              v-for="option in remoteOptions.projectId"
              :key="String(optionValue(option))"
              :label="optionLabel(option)"
              :value="optionValue(option)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋与单元" required>
          <el-input
            v-model="bulkBuildingForm.content"
            type="textarea"
            :rows="10"
            placeholder="每行一个楼栋。示例：&#10;1栋：1单元、2单元&#10;2栋：1单元、2单元&#10;3栋"
          />
          <p class="field-help">冒号前为楼栋名称，冒号后为该楼栋下的单元名称；多个单元可用顿号、逗号、分号或空格分隔。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bulkBuildingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bulkBuildingSaving" @click="submitBulkBuildings">批量保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="actionDialogVisible" :title="currentAction?.label ?? '业务操作'" width="560px" draggable>
      <el-form label-position="top">
        <el-form-item
          v-for="field in currentAction?.fields ?? []"
          :key="field.prop"
          :label="field.label"
          :required="field.required"
        >
          <el-select v-if="field.type === 'select'" v-model="actionForm[field.prop]" clearable class="form-control">
            <el-option
              v-for="option in field.options ?? []"
              :key="String(optionValue(option))"
              :label="optionLabel(option)"
              :value="optionValue(option)"
            />
          </el-select>
          <el-input-number v-else-if="field.type === 'number'" v-model="actionForm[field.prop]" class="form-control" />
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="actionForm[field.prop]"
            type="date"
            value-format="YYYY-MM-DD"
            class="form-control"
          />
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="actionForm[field.prop]"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="form-control"
          />
          <el-input v-else-if="field.type === 'textarea'" v-model="actionForm[field.prop]" type="textarea" :rows="4" />
          <el-input v-else v-model="actionForm[field.prop]" />
          <p v-if="field.help" class="field-help">{{ field.help }}</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionSaving" @click="submitAction">确认执行</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, Download, Edit, Finished, Plus, Refresh, Search, SwitchButton, Tools, Upload } from '@element-plus/icons-vue'
import { pcaTextArr } from 'element-china-area-data'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { createRecord, downloadFile, fetchPage, postAction, putAction, updateRecord } from '@/api/admin'
import { allPages, type FieldConfig, type PageConfig } from '@/config/pages'
import { useAuthStore } from '@/store/auth'

type ActionScope = 'page' | 'row'
type ActionMethod = 'POST' | 'PUT' | 'DOWNLOAD'

interface BusinessAction {
  key: string
  label: string
  scope: ActionScope
  method: ActionMethod
  path: string | ((row?: Record<string, unknown>, form?: Record<string, unknown>) => string)
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default'
  icon?: unknown
  permission?: string
  confirm?: string
  fields?: Array<FieldConfig & { help?: string }>
  buildPayload?: (row?: Record<string, unknown>, form?: Record<string, unknown>) => Record<string, unknown>
  filename?: (row?: Record<string, unknown>) => string
}

const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const records = ref<Record<string, unknown>[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const filters = reactive<Record<string, string | number>>({})
const form = reactive<Record<string, string | number | undefined | null>>({})
const actionForm = reactive<Record<string, string | number | undefined | null>>({})
const dialogVisible = ref(false)
const actionDialogVisible = ref(false)
const bulkBuildingDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const editingId = ref<string | number | null>(null)
const actionSaving = ref(false)
const bulkBuildingSaving = ref(false)
const memberSearchLoading = ref(false)
const currentAction = ref<BusinessAction | null>(null)
const currentActionRow = ref<Record<string, unknown> | undefined>()
const detailRow = ref<Record<string, unknown> | null>(null)
const chinaAreaOptions = pcaTextArr
const memberSearchKeyword = ref('')
const memberSearchResults = ref<Record<string, unknown>[]>([])
const selectedParkingMember = ref<Record<string, unknown> | null>(null)
const bulkBuildingForm = reactive<{ projectId?: string | number; content: string }>({
  projectId: undefined,
  content: '',
})
const remoteOptions = reactive<Record<string, SelectOption[]>>({
  projectId: [],
  buildingId: [],
  unitId: [],
  houseId: [],
  areaId: [],
})

const businessActions: Record<string, BusinessAction[]> = {
  bills: [
    {
      key: 'bill-generate',
      label: '批量生成',
      scope: 'page',
      method: 'POST',
      path: '/fee/bills/generate',
      type: 'primary',
      icon: Finished,
      permission: 'fee:bill:generate',
      fields: [
        { prop: 'projectId', label: '项目ID', type: 'number', required: true },
        { prop: 'itemId', label: '收费项目ID', type: 'number', required: true },
        { prop: 'billPeriod', label: '账期', required: true, help: '例如：2026-06' },
        { prop: 'objectType', label: '对象类型', type: 'select', options: ['HOUSE', 'VEHICLE', 'SPACE', 'CONTRACT'] },
        { prop: 'objectIds', label: '对象ID列表', help: '多个 ID 用英文逗号分隔；留空表示按收费绑定生成' },
        { prop: 'dueDate', label: '到期日', type: 'date' },
      ],
      buildPayload: (_row, formData = {}) => ({
        ...formData,
        objectIds: parseIdList(formData.objectIds),
      }),
    },
    {
      key: 'bill-remind',
      label: '催缴',
      scope: 'row',
      method: 'POST',
      path: '/fee/bills/remind',
      type: 'warning',
      icon: Bell,
      permission: 'fee:bill:remind',
      fields: [
        { prop: 'channel', label: '发送渠道', type: 'select', options: ['SITE', 'SMS', 'WECHAT'] },
        { prop: 'templateCode', label: '模板编码' },
        { prop: 'content', label: '催缴内容', type: 'textarea' },
      ],
      buildPayload: (row, formData = {}) => ({
        billIds: [Number(row?.billId)],
        channel: formData.channel,
        templateCode: formData.templateCode,
        content: formData.content,
      }),
    },
    {
      key: 'bill-void',
      label: '作废',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/fee/bills/${row?.billId}/void`,
      type: 'danger',
      permission: 'fee:bill:void',
      fields: [{ prop: 'reason', label: '作废原因', type: 'textarea', required: true }],
    },
  ],
  refunds: [
    {
      key: 'refund-approve',
      label: '通过',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/refunds/${row?.refundId}/audit`,
      type: 'success',
      permission: 'payment:refund:audit',
      fields: [{ prop: 'auditRemark', label: '审批备注', type: 'textarea' }],
      buildPayload: (_row, formData = {}) => ({ auditResult: 'APPROVED', auditRemark: formData.auditRemark }),
    },
    {
      key: 'refund-reject',
      label: '拒绝',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/refunds/${row?.refundId}/audit`,
      type: 'danger',
      permission: 'payment:refund:audit',
      fields: [{ prop: 'auditRemark', label: '拒绝原因', type: 'textarea', required: true }],
      buildPayload: (_row, formData = {}) => ({ auditResult: 'REJECTED', auditRemark: formData.auditRemark }),
    },
  ],
  workorders: [
    workOrderAction('accept', '受理', 'accept', 'success', 'service:workorder:accept'),
    workOrderAction('reject', '驳回', 'reject', 'danger', 'service:workorder:reject', true),
    {
      key: 'workorder-dispatch',
      label: '派单',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/service/workorders/${row?.workOrderId}/dispatch`,
      type: 'primary',
      permission: 'service:workorder:dispatch',
      fields: [
        { prop: 'handlerUserId', label: '处理人用户ID', type: 'number', required: true },
        { prop: 'content', label: '派单说明', type: 'textarea' },
      ],
    },
    workOrderAction('start', '开始', 'start', 'primary', 'service:workorder:process'),
    workOrderAction('hang-up', '挂起', 'hang-up', 'warning', 'service:workorder:process', true),
    workOrderAction('resume', '恢复', 'resume', 'success', 'service:workorder:process'),
    workOrderAction('submit-result', '完工', 'submit-result', 'success', 'service:workorder:process', true),
    workOrderAction('rework', '返工', 'rework', 'warning', 'service:workorder:revisit', true),
    workOrderAction('revisit', '回访', 'revisit', 'primary', 'service:workorder:revisit', true),
    workOrderAction('cancel', '取消', 'cancel', 'danger', 'service:workorder:cancel', true),
    {
      key: 'workorder-mark-overdue',
      label: '标记超时',
      scope: 'page',
      method: 'POST',
      path: '/service/workorders/sla/mark-overdue',
      type: 'warning',
      icon: Tools,
      permission: 'service:workorder:sla',
      confirm: '确认扫描并标记超时工单？',
    },
  ],
  'service-messages': [
    {
      key: 'message-dispatch',
      label: '派发待发送',
      scope: 'page',
      method: 'POST',
      path: (_row, formData = {}) => `/service/messages/dispatch-pending${toQuery(formData)}`,
      type: 'primary',
      icon: Upload,
      permission: 'service:message:dispatch',
      fields: [{ prop: 'limit', label: '处理上限', type: 'number' }],
    },
    {
      key: 'message-retry-failed',
      label: '重试失败',
      scope: 'page',
      method: 'POST',
      path: (_row, formData = {}) => `/service/messages/retry-failed${toQuery(formData)}`,
      type: 'warning',
      icon: Refresh,
      permission: 'service:message:retry',
      fields: [{ prop: 'limit', label: '处理上限', type: 'number' }],
    },
    {
      key: 'message-retry-one',
      label: '重试',
      scope: 'row',
      method: 'POST',
      path: (row) => `/service/messages/${row?.messageId}/retry`,
      type: 'primary',
      permission: 'service:message:retry',
      confirm: '确认重试发送这条消息？',
    },
  ],
  'lease-contracts': [
    {
      key: 'contract-activate',
      label: '生效',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/lease/contracts/${row?.contractId}/activate`,
      type: 'success',
      permission: 'lease:contract:activate',
      confirm: '确认将该合同置为生效？',
    },
    {
      key: 'contract-terminate',
      label: '终止',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/lease/contracts/${row?.contractId}/terminate`,
      type: 'danger',
      permission: 'lease:contract:terminate',
      fields: [{ prop: 'reason', label: '终止原因', type: 'textarea', required: true }],
    },
    {
      key: 'contract-expire-remind',
      label: '到期提醒',
      scope: 'page',
      method: 'POST',
      path: (_row, formData = {}) => `/lease/contracts/expire-remind${toQuery(formData)}`,
      type: 'warning',
      icon: Bell,
      permission: 'lease:contract:remind',
      fields: [{ prop: 'days', label: '到期天数', type: 'number', help: '例如：30，表示提醒 30 天内到期的合同' }],
    },
  ],
  'device-access': [
    {
      key: 'access-sync',
      label: '同步权限',
      scope: 'page',
      method: 'POST',
      path: '/device/access/sync',
      type: 'primary',
      icon: SwitchButton,
      permission: 'device:access:sync',
      fields: [
        { prop: 'projectId', label: '项目ID', type: 'number' },
        { prop: 'deviceId', label: '设备ID', type: 'number' },
        { prop: 'limit', label: '处理上限', type: 'number' },
      ],
    },
  ],
  'import-batches': [
    {
      key: 'import-errors-csv',
      label: '下载错误',
      scope: 'row',
      method: 'DOWNLOAD',
      path: (row) => `/import/batches/${row?.batchId}/errors.csv`,
      type: 'warning',
      icon: Download,
      permission: 'import:batch:errors',
      filename: (row) => `import-errors-${row?.batchNo ?? row?.batchId}.csv`,
    },
  ],
}

const config = computed<PageConfig>(() => {
  const page = allPages.find((item) => item.key === route.meta.pageKey)
  if (!page) {
    throw new Error('页面配置不存在')
  }
  return page
})

const filterFields = computed(() => config.value.columns.filter((field) => field.inFilter))
const formFields = computed(() => config.value.fields ?? [])
const isParkingSpacePage = computed(() => config.value.key === 'parking-spaces')
const visibleFormFields = computed(() => {
  if (!isParkingSpacePage.value) return formFields.value
  return formFields.value.filter((field) => !['projectId', 'buildingId', 'unitId', 'houseId'].includes(field.prop))
})
const canCreate = computed(() => !config.value.createPermission || auth.hasPermission(config.value.createPermission))
const canUpdate = computed(() => !config.value.updatePermission || auth.hasPermission(config.value.updatePermission))
const availableActions = computed(() => (businessActions[config.value.key] ?? []).filter(canRunAction))
const pageActions = computed(() => availableActions.value.filter((action) => action.scope === 'page'))
const rowActions = computed(() => availableActions.value.filter((action) => action.scope === 'row'))
const detailFields = computed(() => config.value.detailFields ?? config.value.columns)
const hasOperationColumn = computed(() => Boolean((config.value.updatePath && canUpdate.value) || config.value.showDetails || rowActions.value.length))
const operationWidth = computed(() => {
  if (config.value.showDetails && config.value.updatePath && canUpdate.value) return 180
  return rowActions.value.length > 4 ? 360 : rowActions.value.length > 1 ? 260 : 140
})

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
    await loadVisibleRemoteOptions()
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
  resetParkingMemberPicker()
  Object.keys(form).forEach((key) => delete form[key])
  formFields.value.forEach((field) => {
    form[field.prop] = undefined
  })
  loadFormRemoteOptions()
  dialogVisible.value = true
}

function openEdit(row: Record<string, unknown>) {
  editingId.value = row[config.value.idProp ?? 'id'] as string | number
  resetParkingMemberPicker()
  Object.keys(form).forEach((key) => delete form[key])
  formFields.value.forEach((field) => {
    form[field.prop] = row[field.prop] as string | number | undefined | null
  })
  loadFormRemoteOptions()
  dialogVisible.value = true
}

function openDetail(row: Record<string, unknown>) {
  detailRow.value = row
  detailDialogVisible.value = true
}

async function save() {
  if (!config.value.createPath) return
  const invalidMobileField = formFields.value.find((field) => isMobileField(field) && !/^\d{11}$/.test(String(form[field.prop] ?? '')))
  if (invalidMobileField) {
    ElMessage.warning('手机号必须为11位数字')
    return
  }
  if (isParkingSpacePage.value && (!form.projectId || !form.buildingId || !form.unitId || !form.houseId)) {
    ElMessage.warning('请先搜索并选择业主/住户')
    return
  }
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

function openBulkBuildingDialog() {
  bulkBuildingForm.projectId = filters.projectId || undefined
  bulkBuildingForm.content = ''
  loadProjects()
  bulkBuildingDialogVisible.value = true
}

async function submitBulkBuildings() {
  const projectId = Number(bulkBuildingForm.projectId)
  if (!Number.isFinite(projectId) || projectId <= 0) {
    ElMessage.warning('请选择小区名称')
    return
  }
  const rows = parseBulkBuildingContent(bulkBuildingForm.content)
  if (!rows.length) {
    ElMessage.warning('请填写楼栋名称')
    return
  }

  bulkBuildingSaving.value = true
  try {
    let buildingCount = 0
    let unitCount = 0
    for (const row of rows) {
      const buildingResponse = await createRecord('/base/buildings', {
        projectId,
        buildingName: row.buildingName,
        status: 'ACTIVE',
      })
      buildingCount += 1
      const buildingId = buildingResponse.data?.data?.buildingId
      for (const unitName of row.unitNames) {
        await createRecord('/base/units', {
          projectId,
          buildingId,
          unitName,
          status: 'ACTIVE',
        })
        unitCount += 1
      }
    }
    ElMessage.success(`已新增 ${buildingCount} 个楼栋、${unitCount} 个单元`)
    bulkBuildingDialogVisible.value = false
    filters.projectId = projectId
    pageNo.value = 1
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '批量新增失败')
  } finally {
    bulkBuildingSaving.value = false
  }
}

function parseBulkBuildingContent(content: string) {
  return content
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [buildingName = '', unitText = ''] = line.split(/[:：]/, 2)
      return {
        buildingName: buildingName.trim(),
        unitNames: unitText
          .split(/[、,，;；\s]+/)
          .map((item) => item.trim())
          .filter(Boolean),
      }
    })
    .filter((row) => row.buildingName)
}

function isMobileField(field: FieldConfig) {
  return field.prop === 'mobile'
}

function handleTextInput(field: FieldConfig, value: string) {
  if (!isMobileField(field)) return
  form[field.prop] = value.replace(/\D/g, '').slice(0, 11)
}

function resetParkingMemberPicker() {
  memberSearchKeyword.value = ''
  memberSearchResults.value = []
  selectedParkingMember.value = null
}

async function searchMembers() {
  if (!isParkingSpacePage.value) return
  const keyword = memberSearchKeyword.value.trim()
  if (!keyword) {
    ElMessage.warning('请输入业主/住户姓名或手机号')
    return
  }
  memberSearchLoading.value = true
  try {
    const { data } = await fetchPage('/base/members', {
      keyword,
      status: 'ACTIVE',
      pageNo: 1,
      pageSize: 10,
    })
    memberSearchResults.value = toRecords(data.data)
    if (!memberSearchResults.value.length) {
      ElMessage.info('未找到匹配的业主/住户')
    }
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '搜索业主/住户失败')
  } finally {
    memberSearchLoading.value = false
  }
}

async function selectParkingMember(member: Record<string, unknown>) {
  if (!member.projectId || !member.buildingId || !member.unitId || !member.houseId) {
    ElMessage.warning('该业主/住户没有已绑定房屋，不能用于新增车位')
    return
  }
  selectedParkingMember.value = member
  form.projectId = Number(member.projectId)
  form.buildingId = Number(member.buildingId)
  form.unitId = Number(member.unitId)
  form.houseId = Number(member.houseId)
  await loadBuildings()
  await loadUnits()
  await loadHouses()
  await loadParkingAreas()
  ElMessage.success('已带出业主/住户房屋信息')
}

function bindRoleLabel(value: unknown) {
  const labels: Record<string, string> = {
    OWNER: '业主',
    FAMILY: '家属',
    TENANT: '租户',
    RESIDENT: '住户',
  }
  return typeof value === 'string' ? labels[value] ?? value : '-'
}

function optionText(type: 'project' | 'building' | 'unit' | 'house' | 'parkingArea', value: unknown) {
  const keyMap = {
    project: 'projectId',
    building: 'buildingId',
    unit: 'unitId',
    house: 'houseId',
    parkingArea: 'areaId',
  } as const
  const option = remoteOptions[keyMap[type]].find((item) => optionValue(item) === value)
  return option ? optionLabel(option) : value ?? '-'
}

type SelectOption = string | { label: string; value: string | number }
type AreaOption = { label: string; value: string; children?: AreaOption[] }

function isSelectField(field: FieldConfig) {
  return ['select', 'province', 'city', 'district', 'project', 'building', 'unit', 'house', 'parkingArea'].includes(field.type ?? '')
}

function optionsForField(field: FieldConfig): SelectOption[] {
  if (field.type === 'project') {
    return remoteOptions.projectId
  }
  if (field.type === 'building') {
    return remoteOptions.buildingId
  }
  if (field.type === 'unit') {
    return remoteOptions.unitId
  }
  if (field.type === 'house') {
    return remoteOptions.houseId
  }
  if (field.type === 'parkingArea') {
    return remoteOptions.areaId
  }
  if (field.type === 'province') {
    return chinaAreaOptions.map(toSelectOption)
  }
  if (field.type === 'city') {
    const province = findAreaOption(chinaAreaOptions, form.province)
    return (province?.children ?? []).map(toSelectOption)
  }
  if (field.type === 'district') {
    const province = findAreaOption(chinaAreaOptions, form.province)
    const city = findAreaOption(province?.children ?? [], form.city)
    return (city?.children ?? []).map(toSelectOption)
  }
  return field.options ?? []
}

function handleFieldChange(field: FieldConfig) {
  if (field.type === 'province') {
    form.city = undefined
    form.district = undefined
  }
  if (field.type === 'city') {
    form.district = undefined
  }
  if (field.type === 'project') {
    form.buildingId = undefined
    form.unitId = undefined
    form.houseId = undefined
    form.areaId = undefined
    loadBuildings()
    loadParkingAreas()
    remoteOptions.unitId = []
    remoteOptions.houseId = []
  }
  if (field.type === 'building') {
    form.unitId = undefined
    form.houseId = undefined
    loadUnits()
    remoteOptions.houseId = []
  }
  if (field.type === 'unit') {
    form.houseId = undefined
    loadHouses()
  }
}

function displayCell(row: Record<string, unknown>, field: FieldConfig) {
  const value = row[field.prop]
  if (field.type === 'house' && row.houseNo) {
    return row.houseNo
  }
  if (field.type === 'parkingArea' && row.areaName) {
    return row.areaName
  }
  if (!isSelectField(field)) {
    return value ?? ''
  }
  const option = optionsForDisplay(field).find((item) => optionValue(item) === value)
  return option ? optionLabel(option) : value ?? ''
}

function displayDetailCell(row: Record<string, unknown>, field: FieldConfig) {
  const value = displayCell(row, field)
  return value === '' ? '-' : value
}

function optionsForDisplay(field: FieldConfig) {
  if (field.type === 'project') return remoteOptions.projectId
  if (field.type === 'building') return remoteOptions.buildingId
  if (field.type === 'unit') return remoteOptions.unitId
  if (field.type === 'house') return remoteOptions.houseId
  if (field.type === 'parkingArea') return remoteOptions.areaId
  return field.options ?? []
}

function toSelectOption(option: AreaOption): SelectOption {
  return { label: option.label, value: option.value }
}

function findAreaOption(options: AreaOption[], value: unknown) {
  return options.find((option) => option.value === value || option.label === value)
}

function optionLabel(option: SelectOption) {
  return typeof option === 'string' ? option : option.label
}

function optionValue(option: SelectOption) {
  return typeof option === 'string' ? option : option.value
}

function needsRemoteOptions(fields: FieldConfig[]) {
  return fields.some((field) => ['project', 'building', 'unit', 'house', 'parkingArea'].includes(field.type ?? ''))
}

async function loadVisibleRemoteOptions() {
  const fields = [...config.value.columns, ...formFields.value]
  if (!needsRemoteOptions(fields)) return
  await loadProjects()
  await loadBuildings()
  await loadUnits()
  await loadHouses()
  await loadParkingAreas()
}

async function loadFormRemoteOptions() {
  if (!needsRemoteOptions(formFields.value)) return
  await loadProjects()
  await loadBuildings()
  await loadUnits()
  await loadHouses()
  await loadParkingAreas()
}

async function loadProjects() {
  const { data } = await fetchPage('/base/projects', { pageNo: 1, pageSize: 200 })
  remoteOptions.projectId = toRecords(data.data).map((item) => ({
    label: String(item.projectName ?? item.projectCode ?? item.projectId),
    value: Number(item.projectId),
  }))
}

async function loadBuildings() {
  const projectId = Number(form.projectId ?? filters.projectId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200 }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  const { data } = await fetchPage('/base/buildings', params)
  remoteOptions.buildingId = toRecords(data.data).map((item) => ({
    label: String(item.buildingName ?? item.buildingCode ?? item.buildingId),
    value: Number(item.buildingId),
  }))
}

async function loadUnits() {
  const projectId = Number(form.projectId ?? filters.projectId)
  const buildingId = Number(form.buildingId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200 }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  if (Number.isFinite(buildingId) && buildingId > 0) params.buildingId = buildingId
  const { data } = await fetchPage('/base/units', params)
  remoteOptions.unitId = toRecords(data.data).map((item) => ({
    label: String(item.unitName ?? item.unitCode ?? item.unitId),
    value: Number(item.unitId),
  }))
}

async function loadHouses() {
  const projectId = Number(form.projectId ?? filters.projectId)
  const buildingId = Number(form.buildingId)
  const unitId = Number(form.unitId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200 }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  if (Number.isFinite(buildingId) && buildingId > 0) params.buildingId = buildingId
  if (Number.isFinite(unitId) && unitId > 0) params.unitId = unitId
  const { data } = await fetchPage('/base/houses', params)
  remoteOptions.houseId = toRecords(data.data).map((item) => ({
    label: String(item.houseNo ?? item.houseId),
    value: Number(item.houseId),
  }))
}

async function loadParkingAreas() {
  const projectId = Number(form.projectId ?? filters.projectId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200 }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  const { data } = await fetchPage('/base/parking-areas', params)
  remoteOptions.areaId = toRecords(data.data).map((item) => ({
    label: String(item.areaName ?? item.areaId),
    value: Number(item.areaId),
  }))
}

function toRecords(payload: unknown): Record<string, unknown>[] {
  if (Array.isArray(payload)) return payload as Record<string, unknown>[]
  if (payload && typeof payload === 'object' && Array.isArray((payload as { records?: unknown }).records)) {
    return (payload as { records: Record<string, unknown>[] }).records
  }
  if (payload && typeof payload === 'object' && Array.isArray((payload as { items?: unknown }).items)) {
    return (payload as { items: Record<string, unknown>[] }).items
  }
  return []
}

function canRunAction(action: BusinessAction) {
  return !action.permission || auth.hasPermission(action.permission)
}

function workOrderAction(
  key: string,
  label: string,
  endpoint: string,
  type: BusinessAction['type'],
  permission: string,
  requiredContent = false,
): BusinessAction {
  return {
    key: `workorder-${key}`,
    label,
    scope: 'row',
    method: 'PUT',
    path: (row) => `/service/workorders/${row?.workOrderId}/${endpoint}`,
    type,
    permission,
    fields: [
      { prop: 'content', label: '处理说明', type: 'textarea', required: requiredContent },
      { prop: 'imageFileIds', label: '附件文件ID', help: '多个文件 ID 用英文逗号分隔' },
    ],
  }
}

function parseIdList(value: unknown) {
  if (typeof value !== 'string' || !value.trim()) return undefined
  return value.split(',').map((item) => Number(item.trim())).filter((item) => Number.isFinite(item))
}

function compactPayload(payload: Record<string, unknown>) {
  return Object.fromEntries(Object.entries(payload).filter(([, value]) => value !== undefined && value !== '' && value !== null))
}

function toQuery(data: Record<string, unknown>) {
  const params = new URLSearchParams()
  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined && value !== '' && value !== null) params.set(key, String(value))
  })
  const query = params.toString()
  return query ? `?${query}` : ''
}

function resolvePath(action: BusinessAction, row?: Record<string, unknown>, formData: Record<string, unknown> = {}) {
  return typeof action.path === 'function' ? action.path(row, formData) : action.path
}

async function runAction(action: BusinessAction, row?: Record<string, unknown>) {
  currentAction.value = action
  currentActionRow.value = row
  Object.keys(actionForm).forEach((key) => delete actionForm[key])
  ;(action.fields ?? []).forEach((field) => {
    actionForm[field.prop] = undefined
  })

  if (action.fields?.length) {
    actionDialogVisible.value = true
    return
  }

  if (action.confirm) {
    await ElMessageBox.confirm(action.confirm, action.label, { type: action.type === 'danger' ? 'warning' : 'info' })
  }
  await executeAction(action, row)
}

async function submitAction() {
  if (!currentAction.value) return
  const missing = (currentAction.value.fields ?? []).find((field) => field.required && !actionForm[field.prop])
  if (missing) {
    ElMessage.warning(`请填写${missing.label}`)
    return
  }
  await executeAction(currentAction.value, currentActionRow.value, compactPayload(actionForm))
  actionDialogVisible.value = false
}

async function executeAction(action: BusinessAction, row?: Record<string, unknown>, formData: Record<string, unknown> = {}) {
  actionSaving.value = true
  try {
    const path = resolvePath(action, row, formData)
    if (action.method === 'DOWNLOAD') {
      const response = await downloadFile(path)
      saveBlob(response.data as Blob, action.filename?.(row) ?? 'download.csv')
      ElMessage.success('下载已开始')
      return
    }

    const payload = compactPayload(action.buildPayload ? action.buildPayload(row, formData) : formData)
    if (action.method === 'PUT') {
      await putAction(path, payload)
    } else {
      await postAction(path, payload)
    }
    ElMessage.success('操作成功')
    await load()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err instanceof Error ? err.message : '操作失败')
    }
  } finally {
    actionSaving.value = false
  }
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

watch(
  () => route.meta.pageKey,
  () => {
    pageNo.value = 1
    reset()
  },
)

onMounted(load)
onMounted(loadProjects)
</script>

<style scoped>
.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.field-help {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.member-picker {
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid #dce8e5;
  border-radius: 8px;
  background: #f7fbfa;
}

.member-picker__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.member-picker__head span {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.member-picker__head small {
  color: #64748b;
  font-size: 12px;
}

.member-picker__search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.member-result-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.member-result {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(120px, auto);
  gap: 12px;
  align-items: center;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d7e4e1;
  border-radius: 8px;
  background: #fff;
  color: #111827;
  cursor: pointer;
  text-align: left;
}

.member-result:hover:not(:disabled) {
  border-color: #168276;
  background: #f0faf8;
}

.member-result:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.member-result span,
.member-card div {
  display: grid;
  gap: 2px;
}

.member-result strong,
.member-card strong {
  color: #111827;
  font-size: 14px;
}

.member-result small,
.member-card span {
  color: #64748b;
  font-size: 12px;
}

.member-card {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #d7e4e1 inset;
}

.detail-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: #f8fafc;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.detail-form :deep(.el-input.is-disabled .el-input__inner) {
  color: #303133;
  -webkit-text-fill-color: #303133;
}
</style>
