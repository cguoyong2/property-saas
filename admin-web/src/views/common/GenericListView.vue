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
        <el-select
          v-model="filters.projectId"
          clearable
          filterable
          placeholder="小区名称"
          class="form-control project-filter-control"
        >
          <el-option
            v-for="option in remoteOptions.projectId"
            :key="String(optionValue(option))"
            :label="optionLabel(option)"
            :value="optionValue(option)"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-for="field in filterFields" :key="field.prop" :label="field.label">
        <el-select
          v-if="isSelectField(field)"
          v-model="filters[field.prop]"
          clearable
          filterable
          :placeholder="field.label"
          class="form-control"
        >
          <el-option
            v-for="option in optionsForField(field)"
            :key="String(optionValue(option))"
            :label="optionLabel(option)"
            :value="optionValue(option)"
          />
        </el-select>
        <el-input v-else v-model="filters[field.prop]" clearable :placeholder="field.label" />
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
          <template v-for="action in rowActions" :key="action.key">
            <el-button
              v-if="isActionVisible(action, row)"
              text
              :type="action.type ?? 'primary'"
              @click="runAction(action, row)"
            >
              {{ action.label }}
            </el-button>
          </template>
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
      <section v-if="needsMemberPicker" class="member-picker">
        <div class="member-picker__head">
          <span>搜索业主/住户</span>
          <small>选择后自动带出业主/住户和房屋信息</small>
        </div>
        <div v-if="isRefundPage" class="member-picker__project">
          <el-select
            v-model="form.projectId"
            clearable
            filterable
            class="form-control"
            placeholder="先选择小区名称"
            @change="handleRefundProjectChange"
          >
            <el-option
              v-for="option in remoteOptions.projectId"
              :key="String(optionValue(option))"
              :label="optionLabel(option)"
              :value="optionValue(option)"
            />
          </el-select>
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
            :disabled="!isRefundPage && !member.houseId"
            @click="selectParkingMember(member)"
          >
            <span>
              <strong>{{ member.realName || '-' }}</strong>
              <small>{{ member.mobile || '-' }}</small>
            </span>
            <span>
              <strong>{{ projectName(member.projectId) }}</strong>
              <small>小区名称</small>
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
            <span>小区名称</span>
            <strong>{{ projectName(selectedParkingMember?.projectId ?? form.projectId) }}</strong>
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
        <div v-if="isRefundPage && selectedParkingMember" class="refund-order-picker">
          <div class="refund-order-picker__head">
            <span>选择已收款账单</span>
            <small>只显示已收款且还有可退金额的账单</small>
          </div>
          <el-select
            v-model="form.orderId"
            clearable
            filterable
            class="form-control"
            placeholder="请选择已收款账单"
            @change="selectRefundOrder"
          >
            <el-option
              v-for="order in refundableOrders"
              :key="String(order.orderId)"
              :label="refundOrderLabel(order)"
              :value="Number(order.orderId)"
            />
          </el-select>
          <div v-if="selectedRefundOrder" class="refund-order-card">
            <div>
              <span>订单号</span>
              <strong>{{ selectedRefundOrder.orderNo ?? '-' }}</strong>
            </div>
            <div>
              <span>订单金额</span>
              <strong>{{ moneyText(selectedRefundOrder.amount) }}</strong>
            </div>
            <div>
              <span>已退金额</span>
              <strong>{{ moneyText(selectedRefundOrder.refundedAmount) }}</strong>
            </div>
            <div>
              <span>可退金额</span>
              <strong>{{ moneyText(selectedRefundOrder.refundableAmount) }}</strong>
            </div>
            <div class="refund-order-card__summary">
              <span>账单明细</span>
              <strong>{{ selectedRefundOrder.billSummary ?? '-' }}</strong>
            </div>
          </div>
        </div>
      </section>
      <el-form label-position="top">
        <el-form-item v-for="field in visibleFormFields" :key="field.prop" :label="formFieldLabel(field)" :required="field.required">
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
          <template v-else-if="field.type === 'number'">
            <el-input-number v-model="form[field.prop]" class="form-control" />
            <p v-if="field.prop === 'unitPrice' && config.key === 'fee-standards'" class="field-help">
              {{ unitPriceHelpText }}
            </p>
          </template>
          <div v-else-if="field.type === 'plateNo'" class="plate-input">
            <el-select v-model="plateProvince" filterable placeholder="省" class="plate-input__province" @change="syncPlateNo">
              <el-option v-for="item in plateProvinceOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-select v-model="plateLetter" filterable placeholder="字母" class="plate-input__letter" @change="syncPlateNo">
              <el-option v-for="item in plateLetterOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-input
              v-model="plateSuffix"
              class="plate-input__suffix"
              maxlength="6"
              placeholder="后5/6位号码或字母"
              @input="syncPlateNo"
            />
          </div>
          <el-select
            v-else-if="field.type === 'vehicleBrand'"
            v-model="form[field.prop]"
            allow-create
            clearable
            default-first-option
            filterable
            class="form-control"
            placeholder="输入品牌首字搜索，可新增"
            @change="handleVehicleBrandChange"
          >
            <el-option v-for="item in vehicleBrandOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select
            v-else-if="field.type === 'vehicleModel'"
            v-model="form[field.prop]"
            allow-create
            clearable
            default-first-option
            filterable
            class="form-control"
            placeholder="输入型号关键字搜索，可新增"
            @change="handleVehicleModelChange"
          >
            <el-option v-for="item in vehicleModelOptions" :key="item" :label="item" :value="item" />
          </el-select>
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
            :maxlength="inputMaxLength(field)"
            :inputmode="inputMode(field)"
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
          <el-select
            v-if="isSelectField(field)"
            v-model="actionForm[field.prop]"
            clearable
            filterable
            :multiple="field.type === 'billObjectMulti'"
            class="form-control"
            :placeholder="field.label"
            @change="handleActionFieldChange(field)"
          >
            <el-option
              v-for="option in optionsForActionField(field)"
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
import { Bell, Download, Edit, Plus, Refresh, Search, SwitchButton, Tools, Upload } from '@element-plus/icons-vue'
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
  visible?: (row?: Record<string, unknown>) => boolean
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
const form = reactive<Record<string, unknown>>({})
const actionForm = reactive<Record<string, unknown>>({})
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
const plateProvinceOptions = '京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼'.split('')
const plateLetterOptions = 'ABCDEFGHJKLMNPQRSTUVWXYZ'.split('')
const plateProvinceStorageKey = 'property-saas.default-plate-province'
const plateLetterStorageKey = 'property-saas.default-plate-letter'
const vehicleBrandStorageKey = 'property-saas.vehicle-brands'
const vehicleBrandModelStorageKey = 'property-saas.vehicle-brand-models'
const builtInVehicleBrandModels: Record<string, string[]> = {
  比亚迪: ['秦PLUS', '宋PLUS', '汉', '唐', '海豚', '海豹', '元PLUS'],
  特斯拉: ['Model 3', 'Model Y', 'Model S', 'Model X'],
  丰田: ['卡罗拉', '凯美瑞', 'RAV4荣放', '汉兰达', '亚洲龙', '普拉多'],
  本田: ['雅阁', '思域', 'CR-V', '皓影', '飞度', '奥德赛'],
  大众: ['朗逸', '速腾', '迈腾', '帕萨特', '途观L', 'ID.4'],
  奔驰: ['A级', 'C级', 'E级', 'GLA', 'GLC', 'GLE'],
  宝马: ['1系', '3系', '5系', 'X1', 'X3', 'X5'],
  奥迪: ['A3', 'A4L', 'A6L', 'Q3', 'Q5L', 'Q7'],
  吉利: ['帝豪', '星瑞', '博越L', '缤越', '银河L7'],
  长安: ['逸动', 'CS55 PLUS', 'CS75 PLUS', 'UNI-V', '深蓝SL03'],
  长城: ['哈弗H6', '哈弗大狗', '坦克300', '魏牌摩卡', '欧拉好猫'],
  奇瑞: ['艾瑞泽8', '瑞虎8', '瑞虎9', '风云A8', '捷途旅行者'],
  红旗: ['H5', 'H9', 'HS5', 'HS7', 'E-QM5'],
  蔚来: ['ET5', 'ET7', 'ES6', 'ES8', 'EC6'],
  小鹏: ['P7', 'P7i', 'G6', 'G9', 'X9'],
  理想: ['L6', 'L7', 'L8', 'L9', 'MEGA'],
  问界: ['M5', 'M7', 'M9'],
  五菱: ['宏光MINIEV', '缤果', '星光', '凯捷'],
  别克: ['英朗', '君威', '君越', 'GL8', '昂科威'],
  雪佛兰: ['科鲁泽', '迈锐宝XL', '探界者', '开拓者'],
  日产: ['轩逸', '天籁', '逍客', '奇骏', '骐达'],
  福特: ['福克斯', '蒙迪欧', '锐界', '探险者', '烈马'],
  现代: ['伊兰特', '索纳塔', '途胜', '胜达'],
  起亚: ['K3', 'K5', '狮铂拓界', '嘉华'],
  沃尔沃: ['S60', 'S90', 'XC40', 'XC60', 'XC90'],
  雷克萨斯: ['ES', 'NX', 'RX', 'UX', 'LM'],
  保时捷: ['Macan', 'Cayenne', 'Panamera', 'Taycan', '911'],
  路虎: ['揽胜', '揽胜运动版', '发现', '卫士', '极光'],
}
const customVehicleBrands = ref<string[]>(loadJsonArray(vehicleBrandStorageKey))
const customVehicleBrandModels = ref<Record<string, string[]>>(loadJsonRecord(vehicleBrandModelStorageKey))
const remoteVehicleModels = ref<string[]>([])
const memberSearchKeyword = ref('')
const memberSearchResults = ref<Record<string, unknown>[]>([])
const selectedParkingMember = ref<Record<string, unknown> | null>(null)
const refundableOrders = ref<Record<string, unknown>[]>([])
const selectedRefundOrder = ref<Record<string, unknown> | null>(null)
const memberSearchTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const memberSearchRequestId = ref(0)
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
  spaceId: [],
  itemId: [],
  standardId: [],
  objectId: [],
  brandId: [],
})

function billCollectionIds(row?: Record<string, unknown>) {
  if (!row?.billId) return []
  const payableStatuses = new Set(['UNPAID', 'OVERDUE', 'PARTIAL_PAID'])
  const projectId = row.projectId
  const memberId = row.memberId
  const houseId = row.houseId
  const billPeriod = row.billPeriod
  const ids = records.value
    .filter((item) => item.projectId === projectId)
    .filter((item) => item.memberId === memberId)
    .filter((item) => String(item.billPeriod ?? '') === String(billPeriod ?? ''))
    .filter((item) => (houseId ? item.houseId === houseId : true))
    .filter((item) => payableStatuses.has(String(item.status ?? '')))
    .map((item) => Number(item.billId))
    .filter((id) => Number.isFinite(id))
  return ids.length ? Array.from(new Set(ids)) : [Number(row.billId)]
}

function billCollectionRemainingAmount(row?: Record<string, unknown>) {
  if (!row?.billId) return 0
  const ids = new Set(billCollectionIds(row))
  const amount = records.value
    .filter((item) => ids.has(Number(item.billId)))
    .map((item) => Number(item.remainingAmount ?? item.receivableAmount ?? 0))
    .filter((value) => Number.isFinite(value) && value > 0)
    .reduce((sum, value) => sum + value, 0)
  return Math.round(amount * 100) / 100
}

const businessActions: Record<string, BusinessAction[]> = {
  bills: [
    {
      key: 'bill-cash-collect',
      label: '现金收款',
      scope: 'row',
      method: 'POST',
      path: '/payment/offline-collections',
      type: 'success',
      permission: 'payment:order:create',
      fields: [
        {
          prop: 'amount',
          label: '收款金额',
          type: 'number',
          required: true,
          help: '可小于、等于或大于应收金额：少收时保留待收款，超收时超出部分转为业主/住户预存款。',
        },
      ],
      buildPayload: (row, formData = {}) => ({
        projectId: Number(row?.projectId),
        billIds: billCollectionIds(row),
        payChannel: 'CASH',
        amount: formData.amount,
      }),
    },
    {
      key: 'bill-qr-collect',
      label: '收款码',
      scope: 'row',
      method: 'POST',
      path: '/payment/orders',
      type: 'primary',
      permission: 'payment:order:create',
      confirm: '确认生成当前业主/房屋/账期的收款码订单？',
      buildPayload: (row) => ({
        projectId: Number(row?.projectId),
        billIds: billCollectionIds(row),
        payChannel: 'WECHAT',
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
        { prop: 'channel', label: '发送渠道', type: 'select', options: [
          { label: '站内消息', value: 'SITE' },
          { label: '短信', value: 'SMS' },
          { label: '微信', value: 'WECHAT' },
        ] },
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
  'member-bindings': [
    {
      key: 'member-binding-approve',
      label: '通过',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/base/member-bindings/${row?.bindId}/audit`,
      type: 'success',
      permission: 'base:memberBinding:audit',
      visible: (row) => row?.status === 'PENDING',
      fields: [{ prop: 'auditRemark', label: '审核备注', type: 'textarea' }],
      buildPayload: (_row, formData = {}) => ({ auditResult: 'APPROVED', auditRemark: formData.auditRemark }),
    },
    {
      key: 'member-binding-reject',
      label: '驳回',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/base/member-bindings/${row?.bindId}/audit`,
      type: 'danger',
      permission: 'base:memberBinding:audit',
      visible: (row) => row?.status === 'PENDING',
      fields: [{ prop: 'auditRemark', label: '驳回原因', type: 'textarea', required: true }],
      buildPayload: (_row, formData = {}) => ({ auditResult: 'REJECTED', auditRemark: formData.auditRemark }),
    },
    {
      key: 'member-binding-unbind',
      label: '解绑',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/base/member-bindings/${row?.bindId}/unbind`,
      type: 'warning',
      permission: 'base:memberBinding:unbind',
      visible: (row) => row?.status === 'APPROVED',
      fields: [{ prop: 'reason', label: '解绑原因', type: 'textarea', required: true }],
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
const isVehiclePage = computed(() => config.value.key === 'vehicles')
const isRefundPage = computed(() => config.value.key === 'refunds')
const needsMemberPicker = computed(() => isParkingSpacePage.value || isVehiclePage.value || isRefundPage.value)
const visibleFormFields = computed(() => {
  if (isParkingSpacePage.value) {
    return formFields.value.filter((field) => !['projectId', 'buildingId', 'unitId', 'houseId'].includes(field.prop))
  }
  if (isVehiclePage.value) {
    return formFields.value.filter((field) => !['projectId', 'memberId', 'houseId'].includes(field.prop))
  }
  if (isRefundPage.value) {
    return formFields.value.filter((field) => !['projectId', 'orderId'].includes(field.prop))
  }
  return formFields.value
})
const plateProvince = computed({
  get: () => String(form.plateNo ?? '').slice(0, 1),
  set: (value: string) => setPlatePart({ province: value }),
})
const plateLetter = computed({
  get: () => String(form.plateNo ?? '').slice(1, 2),
  set: (value: string) => setPlatePart({ letter: value }),
})
const plateSuffix = computed({
  get: () => String(form.plateNo ?? '').slice(2),
  set: (value: string) => setPlatePart({ suffix: value }),
})
const vehicleBrandOptions = computed(() => uniqueTextOptions([
  ...remoteOptions.brandId.map((item) => optionLabel(item)),
  ...Object.keys(builtInVehicleBrandModels),
  ...customVehicleBrands.value,
]))
const vehicleModelOptions = computed(() => {
  const brand = String(form.vehicleBrand ?? '')
  return uniqueTextOptions([
    ...remoteVehicleModels.value,
    ...(builtInVehicleBrandModels[brand] ?? []),
    ...(customVehicleBrandModels.value[brand] ?? []),
  ])
})
const unitPriceHelpText = computed(() => {
  if (form.chargeMethod === 'AREA') return '当前口径：每平方米每月单价。账单金额 = 建筑面积 × 单价 × 周期月数。'
  if (form.chargeMethod === 'FIXED') return '当前口径：当前周期固定金额。周期为年时，单价就是每年总金额。'
  if (form.chargeMethod === 'METER') return '当前口径：每个计量单位的单价。'
  if (form.chargeMethod === 'FORMULA') return '当前口径：作为公式中的 unitPrice / price 参数使用。'
  return '请先选择计费方式，系统会按计费方式显示单价口径。'
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
  return rowActions.value.length > 3 ? 360 : rowActions.value.length > 1 ? 260 : 140
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
  applyDefaultPlateParts()
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
  const invalidPlateField = formFields.value.find((field) => isPlateField(field) && !PLATE_NO_PATTERN.test(String(form[field.prop] ?? '')))
  if (invalidPlateField) {
    ElMessage.warning('车牌号格式不正确，请输入可被道闸识别的标准车牌')
    return
  }
  if (isParkingSpacePage.value && (!form.projectId || !form.buildingId || !form.unitId || !form.houseId)) {
    ElMessage.warning('请先搜索并选择业主/住户')
    return
  }
  if (isVehiclePage.value && (!form.projectId || !form.memberId || !form.houseId)) {
    ElMessage.warning('请先搜索并选择业主/住户')
    return
  }
  if (isVehiclePage.value && form.spaceId) {
    const selectedSpace = selectedParkingSpaceOption()
    if (!selectedSpace) {
      ElMessage.warning('该车位不可用或已被其它车辆占用，请选择空闲车位')
      return
    }
  }
  if (isRefundPage.value) {
    if (!form.projectId) {
      ElMessage.warning('请先选择小区名称')
      return
    }
    if (!selectedParkingMember.value?.memberId) {
      ElMessage.warning('请先搜索并选择业主/住户')
      return
    }
    if (!form.orderId || !selectedRefundOrder.value) {
      ElMessage.warning('请选择已收款账单')
      return
    }
    const refundAmount = Number(form.refundAmount ?? 0)
    const refundableAmount = Number(selectedRefundOrder.value.refundableAmount ?? 0)
    if (!Number.isFinite(refundAmount) || refundAmount <= 0) {
      ElMessage.warning('退款金额必须大于0')
      return
    }
    if (refundableAmount > 0 && refundAmount > refundableAmount) {
      ElMessage.warning(`退款金额不能超过可退金额 ${moneyText(refundableAmount)}`)
      return
    }
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

const PLATE_NO_PATTERN = /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-Z][A-Z0-9挂学警港澳领使]{5,6}$/

function isPlateField(field: FieldConfig) {
  return field.prop === 'plateNo'
}

function inputMaxLength(field: FieldConfig) {
  if (isMobileField(field)) return 11
  if (isPlateField(field)) return 8
  return undefined
}

function inputMode(field: FieldConfig) {
  return isMobileField(field) ? 'numeric' : undefined
}

function handleTextInput(field: FieldConfig, value: string) {
  if (isMobileField(field)) {
    form[field.prop] = value.replace(/\D/g, '').slice(0, 11)
    return
  }
  if (isPlateField(field)) {
    form[field.prop] = normalizePlateNo(value)
  }
}

function normalizePlateNo(value: string) {
  return value.replace(/[\s\-·.]/g, '').toUpperCase().slice(0, 8)
}

function setPlatePart(parts: { province?: string; letter?: string; suffix?: string }) {
  const current = normalizePlateNo(String(form.plateNo ?? ''))
  const province = parts.province ?? current.slice(0, 1)
  const letter = parts.letter ?? current.slice(1, 2)
  const suffix = parts.suffix ?? current.slice(2)
  const normalizedProvince = plateProvinceOptions.includes(province) ? province : ''
  const normalizedLetter = normalizePlateLetter(letter)
  form.plateNo = [
    normalizedProvince,
    normalizedLetter,
    normalizePlateSuffix(suffix),
  ].join('')
  rememberPlateParts(normalizedProvince, normalizedLetter)
}

function syncPlateNo() {
  setPlatePart({})
}

function normalizePlateLetter(value: string) {
  const letter = value.toUpperCase().replace(/[^A-Z]/g, '').slice(0, 1)
  return plateLetterOptions.includes(letter) ? letter : ''
}

function normalizePlateSuffix(value: string) {
  return value.toUpperCase().replace(/[^A-Z0-9挂学警港澳领使]/g, '').slice(0, 6)
}

function applyDefaultPlateParts() {
  if (!isVehiclePage.value) return
  const province = localStorage.getItem(plateProvinceStorageKey) ?? ''
  const letter = localStorage.getItem(plateLetterStorageKey) ?? ''
  const normalizedProvince = plateProvinceOptions.includes(province) ? province : ''
  const normalizedLetter = normalizePlateLetter(letter)
  if (normalizedProvince || normalizedLetter) {
    form.plateNo = `${normalizedProvince}${normalizedLetter}`
  }
}

function rememberPlateParts(province: string, letter: string) {
  if (province) {
    localStorage.setItem(plateProvinceStorageKey, province)
  }
  if (letter) {
    localStorage.setItem(plateLetterStorageKey, letter)
  }
}

async function handleVehicleBrandChange(value: string | number | undefined) {
  const brand = String(value ?? '').trim()
  if (!brand) {
    form.vehicleModel = undefined
    remoteVehicleModels.value = []
    return
  }
  addCustomVehicleBrand(brand)
  await loadVehicleModels()
  if (form.vehicleModel && !vehicleModelOptions.value.includes(String(form.vehicleModel))) {
    form.vehicleModel = undefined
  }
}

function handleVehicleModelChange(value: string | number | undefined) {
  const brand = String(form.vehicleBrand ?? '').trim()
  const model = String(value ?? '').trim()
  if (!brand || !model) return
  addCustomVehicleBrand(brand)
  const models = uniqueTextOptions([...(customVehicleBrandModels.value[brand] ?? []), model])
  customVehicleBrandModels.value = { ...customVehicleBrandModels.value, [brand]: models }
  localStorage.setItem(vehicleBrandModelStorageKey, JSON.stringify(customVehicleBrandModels.value))
}

function addCustomVehicleBrand(brand: string) {
  if (vehicleBrandOptions.value.includes(brand)) return
  customVehicleBrands.value = uniqueTextOptions([...customVehicleBrands.value, brand])
  localStorage.setItem(vehicleBrandStorageKey, JSON.stringify(customVehicleBrands.value))
}

function uniqueTextOptions(values: string[]) {
  return Array.from(new Set(values.map((item) => item.trim()).filter(Boolean))).sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
}

function loadJsonArray(key: string) {
  try {
    const value = JSON.parse(localStorage.getItem(key) ?? '[]')
    return Array.isArray(value) ? value.map(String) : []
  } catch {
    return []
  }
}

function loadJsonRecord(key: string) {
  try {
    const value = JSON.parse(localStorage.getItem(key) ?? '{}')
    if (!value || typeof value !== 'object' || Array.isArray(value)) return {}
    return Object.fromEntries(Object.entries(value).map(([brand, models]) => [
      brand,
      Array.isArray(models) ? models.map(String) : [],
    ]))
  } catch {
    return {}
  }
}

function resetParkingMemberPicker() {
  if (memberSearchTimer.value) {
    clearTimeout(memberSearchTimer.value)
    memberSearchTimer.value = null
  }
  memberSearchKeyword.value = ''
  memberSearchResults.value = []
  selectedParkingMember.value = null
  refundableOrders.value = []
  selectedRefundOrder.value = null
}

async function searchMembers(showNotice = true) {
  if (!needsMemberPicker.value) return
  if (isRefundPage.value && !form.projectId) {
    memberSearchResults.value = []
    if (showNotice) {
      ElMessage.warning('请先选择小区名称')
    }
    return
  }
  const keyword = memberSearchKeyword.value.trim()
  if (!keyword) {
    memberSearchResults.value = []
    if (showNotice) {
      ElMessage.warning('请输入业主/住户姓名或手机号')
    }
    return
  }
  const requestId = memberSearchRequestId.value + 1
  memberSearchRequestId.value = requestId
  memberSearchLoading.value = true
  try {
    const { data } = await fetchPage('/base/members', {
      keyword,
      ...(isRefundPage.value ? { projectId: Number(form.projectId) } : {}),
      status: 'ACTIVE',
      pageNo: 1,
      pageSize: isRefundPage.value ? 50 : 10,
    })
    if (requestId !== memberSearchRequestId.value) return
    const members = toRecords(data.data)
    memberSearchResults.value = isRefundPage.value
      ? members.filter((member) => String(member.projectId ?? '') === String(form.projectId ?? ''))
      : members
    if (showNotice && !memberSearchResults.value.length) {
      ElMessage.info('未找到匹配的业主/住户')
    }
  } catch (err) {
    if (showNotice) {
      ElMessage.error(err instanceof Error ? err.message : '搜索业主/住户失败')
    }
  } finally {
    if (requestId === memberSearchRequestId.value) {
      memberSearchLoading.value = false
    }
  }
}

function scheduleMemberSearch() {
  if (memberSearchTimer.value) {
    clearTimeout(memberSearchTimer.value)
  }
  const keyword = memberSearchKeyword.value.trim()
  if (!keyword) {
    memberSearchResults.value = []
    memberSearchLoading.value = false
    return
  }
  memberSearchTimer.value = setTimeout(() => {
    searchMembers(false)
  }, 250)
}

async function selectParkingMember(member: Record<string, unknown>) {
  if (isRefundPage.value) {
    if (!form.projectId) {
      ElMessage.warning('请先选择小区名称')
      return
    }
    if (!member.memberId || String(member.projectId ?? '') !== String(form.projectId ?? '')) {
      ElMessage.warning('请选择当前小区对应的业主/住户')
      return
    }
  } else if (!member.projectId || !member.buildingId || !member.unitId || !member.houseId) {
    ElMessage.warning('该业主/住户没有已绑定房屋，不能用于当前新增')
    return
  }
  selectedParkingMember.value = member
  form.projectId = Number(member.projectId)
  form.houseId = member.houseId ? Number(member.houseId) : undefined
  if (isParkingSpacePage.value) {
    form.buildingId = Number(member.buildingId)
    form.unitId = Number(member.unitId)
    await loadBuildings()
    await loadUnits()
    await loadHouses()
    await loadParkingAreas()
  }
  if (isVehiclePage.value) {
    form.memberId = Number(member.memberId)
    form.spaceId = undefined
    form.monthlyRentStatus = form.monthlyRentStatus === 'NONE' ? undefined : form.monthlyRentStatus
    await loadParkingSpaces()
    if (!remoteOptions.spaceId.length) {
      ElMessage.warning('该业主/住户名下暂无空闲车位，请先在车位管理新增或释放车位')
    }
  }
  if (isRefundPage.value) {
    await loadRefundableOrders()
    if (!refundableOrders.value.length) {
      ElMessage.warning('该业主/住户暂无可退款的已收款账单')
      return
    }
  }
  ElMessage.success('已带出业主/住户房屋信息')
}

function handleRefundProjectChange() {
  resetParkingMemberPicker()
  form.orderId = undefined
  form.refundAmount = undefined
}

async function loadRefundableOrders() {
  refundableOrders.value = []
  selectedRefundOrder.value = null
  form.orderId = undefined
  form.refundAmount = undefined
  const memberId = selectedParkingMember.value?.memberId
  if (!form.projectId || !memberId) return
  try {
    const { data } = await fetchPage('/payment/refundable-orders', {
      projectId: Number(form.projectId),
      memberId: Number(memberId),
    })
    refundableOrders.value = toRecords(data.data)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载已收款账单失败')
  }
}

function selectRefundOrder(value: unknown) {
  selectedRefundOrder.value = refundableOrders.value.find((order) => Number(order.orderId) === Number(value)) ?? null
  if (selectedRefundOrder.value) {
    form.refundAmount = selectedRefundOrder.value.refundableAmount
  }
}

function refundOrderLabel(order: Record<string, unknown>) {
  return [
    order.orderNo,
    order.billSummary,
    `可退${moneyText(order.refundableAmount)}`,
  ].filter(Boolean).join(' / ')
}

function moneyText(value: unknown) {
  const amount = Number(value ?? 0)
  if (!Number.isFinite(amount)) return '0.00'
  return amount.toFixed(2)
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

function projectName(value: unknown) {
  return optionText('project', value)
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

type SelectOption = string | {
  label: string
  value: string | number
  status?: string
  monthlyRentStatus?: string
}
type AreaOption = { label: string; value: string; children?: AreaOption[] }

function isSelectField(field: FieldConfig) {
  return [
    'select',
    'province',
    'city',
    'district',
    'project',
    'building',
    'unit',
    'house',
    'parkingArea',
    'parkingSpace',
    'feeItem',
    'feeStandard',
    'billObject',
    'billObjectMulti',
    'vehicleBrandId',
  ].includes(field.type ?? '')
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
  if (field.type === 'parkingSpace') {
    return remoteOptions.spaceId
  }
  if (field.type === 'feeItem') {
    return remoteOptions.itemId
  }
  if (field.type === 'feeStandard') {
    return remoteOptions.standardId
  }
  if (field.type === 'billObject' || field.type === 'billObjectMulti') {
    return remoteOptions.objectId
  }
  if (field.type === 'vehicleBrandId') {
    return remoteOptions.brandId
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

function optionsForActionField(field: FieldConfig): SelectOption[] {
  return isSelectField(field) ? optionsForField(field) : field.options ?? []
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
    form.spaceId = undefined
    form.standardId = undefined
    loadBuildings()
    loadParkingAreas()
    loadParkingSpaces()
    loadFeeStandards()
    remoteOptions.unitId = []
    remoteOptions.houseId = []
  }
  if (field.type === 'feeItem') {
    form.standardId = undefined
    loadFeeStandards()
  }
  if (field.prop === 'objectType') {
    form.objectId = undefined
    loadBillObjects()
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
  if (field.type === 'parkingSpace' && isVehiclePage.value) {
    const selectedSpace = selectedParkingSpaceOption()
    if (!form.spaceId) {
      return
    }
    if (!selectedSpace) {
      ElMessage.warning('该车位不可用或已被其它车辆占用，请选择空闲车位')
      return
    }
    if (selectedSpace.status && selectedSpace.status !== 'AVAILABLE') {
      ElMessage.warning('该车位当前不是空闲状态，请确认是否为正在编辑的原车位')
    }
    if (!form.monthlyRentStatus || form.monthlyRentStatus === 'NONE') {
      form.monthlyRentStatus = selectedSpace.monthlyRentStatus && selectedSpace.monthlyRentStatus !== 'NONE'
        ? selectedSpace.monthlyRentStatus
        : 'ACTIVE'
    }
  }
}

function handleActionFieldChange(field: FieldConfig) {
  if (field.type === 'project' || field.type === 'feeItem') {
    actionForm.standardId = undefined
    loadFeeStandardsForAction()
  }
  if (field.type === 'project' || field.prop === 'objectType') {
    actionForm.objectIds = []
    loadBillObjectsForAction()
  }
}

function formFieldLabel(field: FieldConfig) {
  if (config.value.key === 'fee-standards' && field.prop === 'unitPrice') {
    if (form.chargeMethod === 'AREA') return '单价（元/㎡/月）'
    if (form.chargeMethod === 'FIXED') return '单价（本周期金额）'
    if (form.chargeMethod === 'METER') return '单价（元/计量单位）'
    if (form.chargeMethod === 'FORMULA') return '单价（公式参数）'
  }
  return field.label
}

function displayCell(row: Record<string, unknown>, field: FieldConfig) {
  const value = row[field.prop]
  if (config.value.key === 'fee-standards' && field.prop === 'unitPrice') {
    return formatFeeStandardUnitPrice(row)
  }
  if (field.type === 'project' && row.projectName) {
    return row.projectName
  }
  if (field.type === 'building' && row.buildingName) {
    return row.buildingName
  }
  if (field.type === 'unit' && row.unitName) {
    return row.unitName
  }
  if (field.type === 'house' && row.houseNo) {
    return row.houseNo
  }
  if (field.type === 'parkingArea' && row.areaName) {
    return row.areaName
  }
  if (field.type === 'parkingSpace' && row.spaceNo) {
    return [row.areaName, row.spaceNo].filter(Boolean).join(' - ')
  }
  if (field.type === 'feeItem' && row.itemName) {
    return row.itemName
  }
  if (field.type === 'feeStandard' && row.standardName) {
    return row.standardName
  }
  if (field.type === 'billObject') {
    return billObjectLabel(row.objectType, value)
  }
  if (field.type === 'vehicleBrandId' && row.brandName) {
    return row.brandName
  }
  if (!isSelectField(field)) {
    return value ?? ''
  }
  const option = optionsForDisplay(field).find((item) => optionValue(item) === value)
  return option ? optionLabel(option) : value ?? ''
}

function formatFeeStandardUnitPrice(row: Record<string, unknown>) {
  const value = row.unitPrice ?? ''
  if (value === '') return ''
  if (row.chargeMethod === 'AREA') return `${value} 元/㎡/月`
  if (row.chargeMethod === 'FIXED') return `${value} 元/周期`
  if (row.chargeMethod === 'METER') return `${value} 元/计量单位`
  if (row.chargeMethod === 'FORMULA') return `${value}（公式参数）`
  return value
}


function selectedParkingSpaceOption() {
  return remoteOptions.spaceId.find((item) => optionValue(item) === form.spaceId && typeof item !== 'string') as
    | Exclude<SelectOption, string>
    | undefined
}

function billObjectLabel(objectType: unknown, objectId: unknown) {
  if (!objectId) return ''
  const option = remoteOptions.objectId.find((item) => optionValue(item) === objectId)
  if (option) return optionLabel(option)
  const labels: Record<string, string> = {
    HOUSE: '房屋',
    VEHICLE: '车辆',
    SPACE: '车位',
    CONTRACT: '合同',
  }
  const prefix = typeof objectType === 'string' ? labels[objectType] : ''
  return prefix ? `${prefix} ${objectId}` : objectId
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
  if (field.type === 'parkingSpace') return remoteOptions.spaceId
  if (field.type === 'feeItem') return remoteOptions.itemId
  if (field.type === 'feeStandard') return remoteOptions.standardId
  if (field.type === 'billObject' || field.type === 'billObjectMulti') return remoteOptions.objectId
  if (field.type === 'vehicleBrandId') return remoteOptions.brandId
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
  return fields.some((field) => [
    'project',
    'building',
    'unit',
    'house',
    'parkingArea',
    'parkingSpace',
    'feeItem',
    'feeStandard',
    'billObject',
    'billObjectMulti',
    'vehicleBrandId',
    'vehicleBrand',
    'vehicleModel',
  ].includes(field.type ?? ''))
}

async function loadVisibleRemoteOptions() {
  const fields = [...config.value.columns, ...formFields.value]
  if (!needsRemoteOptions(fields)) return
  await loadProjects()
  await loadBuildings()
  await loadUnits()
  await loadHouses()
  await loadParkingAreas()
  await loadParkingSpaces()
  await loadFeeItems()
  await loadFeeStandards()
  await loadBillObjects()
  await loadVehicleBrands()
  await loadVehicleModels()
}

async function loadFormRemoteOptions() {
  if (!needsRemoteOptions(formFields.value)) return
  await loadProjects()
  await loadBuildings()
  await loadUnits()
  await loadHouses()
  await loadParkingAreas()
  await loadParkingSpaces()
  await loadFeeItems()
  await loadFeeStandards()
  await loadBillObjects()
  await loadVehicleBrands()
  await loadVehicleModels()
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

async function loadParkingSpaces() {
  const projectId = Number(form.projectId ?? filters.projectId)
  const houseId = Number(form.houseId)
  const currentSpaceId = Number(form.spaceId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200 }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  const { data } = await fetchPage('/base/parking-spaces', params)
  remoteOptions.spaceId = toRecords(data.data)
    .filter((item) => !Number.isFinite(houseId) || houseId <= 0 || Number(item.houseId) === houseId)
    .filter((item) => {
      if (!isVehiclePage.value) return true
      const spaceId = Number(item.spaceId)
      return item.status === 'AVAILABLE' || (Number.isFinite(currentSpaceId) && currentSpaceId > 0 && spaceId === currentSpaceId)
    })
    .map((item) => ({
      label: String([
        item.areaName,
        item.spaceNo,
        isVehiclePage.value && item.status === 'AVAILABLE' ? '空闲' : '',
      ].filter(Boolean).join(' - ') || item.spaceId),
      value: Number(item.spaceId),
      status: String(item.status ?? ''),
      monthlyRentStatus: String(item.monthlyRentStatus ?? 'NONE'),
    }))
}

async function loadFeeItems() {
  const { data } = await fetchPage('/fee/items', { pageNo: 1, pageSize: 200, status: 'ACTIVE' })
  remoteOptions.itemId = toRecords(data.data).map((item) => ({
    label: String(item.itemName ?? item.itemCode ?? item.itemId),
    value: Number(item.itemId),
  }))
}

async function loadFeeStandards() {
  const projectId = Number(form.projectId ?? filters.projectId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200, status: 'ACTIVE' }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  if (form.itemId) params.itemId = Number(form.itemId)
  const { data } = await fetchPage('/fee/standards', params)
  remoteOptions.standardId = toRecords(data.data).map((item) => ({
    label: String(item.standardName ?? item.standardId),
    value: Number(item.standardId),
  }))
}

async function loadFeeStandardsForAction() {
  const projectId = Number(actionForm.projectId)
  const itemId = Number(actionForm.itemId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200, status: 'ACTIVE' }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  if (Number.isFinite(itemId) && itemId > 0) params.itemId = itemId
  const { data } = await fetchPage('/fee/standards', params)
  remoteOptions.standardId = toRecords(data.data).map((item) => ({
    label: String(item.standardName ?? item.standardId),
    value: Number(item.standardId),
  }))
}

async function loadBillObjects() {
  remoteOptions.objectId = await fetchBillObjectOptions(form.objectType, form.projectId ?? filters.projectId)
}

async function loadBillObjectsForAction() {
  remoteOptions.objectId = await fetchBillObjectOptions(actionForm.objectType, actionForm.projectId)
}

async function fetchBillObjectOptions(objectType: unknown, projectIdValue: unknown): Promise<SelectOption[]> {
  const type = String(objectType ?? '')
  const projectId = Number(projectIdValue)
  if (!type || !Number.isFinite(projectId) || projectId <= 0) {
    return []
  }
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200, projectId }
  if (type === 'HOUSE') {
    const { data } = await fetchPage('/base/houses', params)
    return toRecords(data.data).map((item) => ({
      label: String([
        item.projectName,
        item.buildingName,
        item.unitName,
        item.houseNo,
      ].filter(Boolean).join(' / ') || item.houseId),
      value: Number(item.houseId),
    }))
  }
  if (type === 'VEHICLE') {
    const { data } = await fetchPage('/base/vehicles', params)
    return toRecords(data.data).map((item) => ({
      label: String([
        item.plateNo,
        item.memberName,
        item.houseNo,
      ].filter(Boolean).join(' / ') || item.vehicleId),
      value: Number(item.vehicleId),
    }))
  }
  if (type === 'SPACE') {
    const { data } = await fetchPage('/base/parking-spaces', params)
    return toRecords(data.data).map((item) => ({
      label: String([
        item.areaName,
        item.spaceNo,
        item.houseNo,
      ].filter(Boolean).join(' / ') || item.spaceId),
      value: Number(item.spaceId),
    }))
  }
  return []
}

async function loadVehicleBrands() {
  const { data } = await fetchPage('/base/vehicle-brands', { pageNo: 1, pageSize: 200, status: 'ACTIVE' })
  remoteOptions.brandId = toRecords(data.data).map((item) => ({
    label: String(item.brandName ?? item.brandId),
    value: Number(item.brandId),
  }))
}

async function loadVehicleModels() {
  const brand = String(form.vehicleBrand ?? '').trim()
  if (!brand) {
    remoteVehicleModels.value = []
    return
  }
  const { data } = await fetchPage('/base/vehicle-models', { pageNo: 1, pageSize: 200, brandName: brand, status: 'ACTIVE' })
  remoteVehicleModels.value = toRecords(data.data).map((item) => String(item.modelName ?? '')).filter(Boolean)
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

function isActionVisible(action: BusinessAction, row?: Record<string, unknown>) {
  return !action.visible || action.visible(row)
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
  if (action.key === 'bill-cash-collect') {
    actionForm.amount = billCollectionRemainingAmount(row)
  }

  if (action.fields?.length) {
    await loadActionRemoteOptions(action.fields)
    actionDialogVisible.value = true
    return
  }

  if (action.confirm) {
    await ElMessageBox.confirm(action.confirm, action.label, { type: action.type === 'danger' ? 'warning' : 'info' })
  }
  await executeAction(action, row)
}

async function loadActionRemoteOptions(fields: FieldConfig[]) {
  if (!needsRemoteOptions(fields)) return
  await loadProjects()
  await loadFeeItems()
  await loadFeeStandardsForAction()
  await loadBillObjectsForAction()
}

async function submitAction() {
  if (!currentAction.value) return
  const missing = (currentAction.value.fields ?? []).find((field) => field.required && !actionForm[field.prop])
  if (missing) {
    ElMessage.warning(`请填写${missing.label}`)
    return
  }
  if (currentAction.value.key === 'bill-cash-collect') {
    const amount = Number(actionForm.amount)
    if (!Number.isFinite(amount) || amount <= 0) {
      ElMessage.warning('收款金额必须大于0')
      return
    }
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

watch(memberSearchKeyword, () => {
  if (!dialogVisible.value || !needsMemberPicker.value) return
  scheduleMemberSearch()
})

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

.project-filter-control {
  min-width: 220px;
  width: 220px;
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

.member-picker__project {
  margin-bottom: 10px;
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
  grid-template-columns: minmax(0, 1fr) minmax(140px, 1fr) minmax(110px, auto);
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

.refund-order-picker {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.refund-order-picker__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.refund-order-picker__head span {
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.refund-order-picker__head small {
  color: #64748b;
  font-size: 12px;
}

.refund-order-card {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
  border: 1px solid #d7e4e1;
  border-radius: 8px;
  background: #fff;
}

.refund-order-card div {
  display: grid;
  gap: 2px;
}

.refund-order-card span {
  color: #64748b;
  font-size: 12px;
}

.refund-order-card strong {
  color: #111827;
  font-size: 14px;
}

.refund-order-card__summary {
  grid-column: 1 / -1;
}

.plate-input {
  display: grid;
  grid-template-columns: 88px 100px minmax(0, 1fr);
  gap: 10px;
  width: 100%;
}

.plate-input__province,
.plate-input__letter,
.plate-input__suffix {
  width: 100%;
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
