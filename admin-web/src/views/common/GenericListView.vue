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
        <el-date-picker
          v-else-if="field.type === 'date'"
          v-model="filters[field.prop]"
          type="date"
          value-format="YYYY-MM-DD"
          class="form-control"
          :placeholder="field.label"
        />
        <el-input v-else v-model="filters[field.prop]" clearable :placeholder="field.label" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div v-if="isReconcileExceptionPage" class="reconcile-stats">
      <div class="reconcile-stats__card reconcile-stats__card--danger">
        <span>高风险</span>
        <strong>{{ reconcileStats.highRiskCount }}</strong>
      </div>
      <div class="reconcile-stats__card reconcile-stats__card--warning">
        <span>中风险</span>
        <strong>{{ reconcileStats.mediumRiskCount }}</strong>
      </div>
      <div class="reconcile-stats__card reconcile-stats__card--success">
        <span>低风险</span>
        <strong>{{ reconcileStats.lowRiskCount }}</strong>
      </div>
      <button class="reconcile-stats__card" type="button" @click="setReconcileStatusFilter('OPEN')">
        <span>待处理</span>
        <strong>{{ reconcileStats.openCount }}</strong>
      </button>
      <button class="reconcile-stats__card reconcile-stats__card--link" type="button" @click="goToPendingReconcileReviews()">
        <span>待复核</span>
        <strong>{{ reconcileStats.pendingReviewCount }}</strong>
      </button>
      <div class="reconcile-stats__card reconcile-stats__card--muted">
        <span>已处理</span>
        <strong>{{ reconcileStats.handledCount }}</strong>
      </div>
      <div class="reconcile-stats__total">
        <span>{{ reconcileStatsLoading ? '统计刷新中' : '当前口径合计' }}</span>
        <strong>{{ reconcileStats.totalCount }}</strong>
      </div>
    </div>

    <div v-if="isReconcileReviewPage" class="reconcile-stats reconcile-stats--review">
      <button class="reconcile-stats__card reconcile-stats__card--link" type="button" @click="setReconcileReviewStatusFilter('PENDING')">
        <span>待复核</span>
        <strong>{{ reconcileReviewStats.pendingCount }}</strong>
      </button>
      <button class="reconcile-stats__card reconcile-stats__card--success" type="button" @click="setReconcileReviewStatusFilter('APPROVED')">
        <span>已通过</span>
        <strong>{{ reconcileReviewStats.approvedCount }}</strong>
      </button>
      <button class="reconcile-stats__card reconcile-stats__card--warning" type="button" @click="setReconcileReviewStatusFilter('REJECTED')">
        <span>已退回</span>
        <strong>{{ reconcileReviewStats.rejectedCount }}</strong>
      </button>
      <button class="reconcile-stats__card reconcile-stats__card--success" type="button" @click="setReconcileCurrentCheckFilter('RESOLVED')">
        <span>复算已解决</span>
        <strong>{{ reconcileReviewStats.resolvedCount }}</strong>
      </button>
      <button class="reconcile-stats__card reconcile-stats__card--danger" type="button" @click="setReconcileCurrentCheckFilter('STILL_ABNORMAL')">
        <span>复算仍异常</span>
        <strong>{{ reconcileReviewStats.stillAbnormalCount }}</strong>
      </button>
      <div class="reconcile-stats__total">
        <span>{{ reconcileReviewStatsLoading ? '统计刷新中' : '当前口径合计' }}</span>
        <strong>{{ reconcileReviewStats.totalCount }}</strong>
      </div>
    </div>

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
        v-for="column in tableColumns"
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
          <el-input
            v-else-if="field.type === 'password'"
            v-model="form[field.prop]"
            type="password"
            show-password
            autocomplete="new-password"
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

    <el-dialog
      v-model="detailDialogVisible"
      :title="`${config.title}详情`"
      :width="isReconcileExceptionDetail ? '760px' : '620px'"
      draggable
    >
      <div v-if="isReconcileExceptionDetail && detailRow" class="reconcile-detail">
        <div class="reconcile-detail__summary">
          <div>
            <span>差异金额</span>
            <strong>{{ moneyText(detailRow.amount) }}</strong>
          </div>
          <el-tag :type="reconcileLevelTagType(detailRow.exceptionLevel)" effect="light">
            {{ detailRow.exceptionLevel }}
          </el-tag>
          <el-tag :type="detailRow.status === 'HANDLED' ? 'success' : 'danger'" effect="light">
            {{ reconcileStatusText(detailRow.status) }}
          </el-tag>
          <el-tag :type="detailRow.reviewStatus === 'APPROVED' ? 'success' : detailRow.reviewStatus === 'REJECTED' ? 'warning' : 'info'" effect="light">
            {{ reconcileReviewStatusText(detailRow.reviewStatus) }}
          </el-tag>
        </div>
        <div class="reconcile-detail__grid">
          <div>
            <span>异常类型</span>
            <strong>{{ detailRow.exceptionType }}</strong>
          </div>
          <div>
            <span>业务类型</span>
            <strong>{{ detailRow.businessType }}</strong>
          </div>
          <div>
            <span>业务单号</span>
            <strong>{{ detailRow.businessNo || '-' }}</strong>
          </div>
          <div>
            <span>业主/住户</span>
            <strong>{{ [detailRow.memberName, detailRow.memberMobile].filter(Boolean).join(' / ') || '-' }}</strong>
          </div>
        </div>
        <div class="reconcile-detail__section">
          <h3>异常原因</h3>
          <p>{{ detailRow.reason || '-' }}</p>
        </div>
        <div class="reconcile-detail__section reconcile-rule">
          <h3>分级处理规则</h3>
          <p>{{ reconcileLevelRuleText(detailRow) }}</p>
          <small>{{ reconcileMissingActionText(detailRow) }}</small>
        </div>
        <div class="reconcile-detail__section">
          <h3>系统计算口径</h3>
          <p>{{ reconcileCalculationText(detailRow) }}</p>
        </div>
        <div class="reconcile-detail__section">
          <h3>建议处理动作</h3>
          <p>{{ reconcileSuggestionText(detailRow) }}</p>
        </div>
        <div class="reconcile-detail__actions">
          <span class="reconcile-detail__actions-label">修复入口</span>
          <el-button
            v-for="target in reconcileTargets(detailRow)"
            :key="target.label"
            type="primary"
            plain
            @click="goToReconcileTarget(target.path)"
          >
            {{ target.label }}
          </el-button>
        </div>
        <div class="reconcile-detail__section">
          <h3>处理历史</h3>
          <el-skeleton v-if="reconcileHistoryLoading" :rows="3" animated />
          <el-empty v-else-if="!reconcileHistories.length" description="暂无处理历史" />
          <el-timeline v-else class="reconcile-history">
            <el-timeline-item
              v-for="history in reconcileHistories"
              :key="String(history.historyId)"
              :timestamp="String(history.createdAt ?? '')"
              placement="top"
            >
              <div class="reconcile-history__card">
                <div class="reconcile-history__head">
                  <strong>{{ reconcileHistoryActionText(history.actionType) }}</strong>
                  <span>处理人：{{ history.operatorId ?? '-' }}</span>
                </div>
                <p>{{ history.remark || '-' }}</p>
                <small>
                  状态：{{ reconcileStatusText(history.beforeStatus) }} -> {{ reconcileStatusText(history.afterStatus) }}
                  ｜复核：{{ reconcileReviewStatusText(history.beforeReviewStatus) }} -> {{ reconcileReviewStatusText(history.afterReviewStatus) }}
                </small>
                <div v-if="attachmentIds(history.attachmentFileIds).length" class="reconcile-history__attachments">
                  <span>附件：</span>
                  <el-link
                    v-for="fileId in attachmentIds(history.attachmentFileIds)"
                    :key="fileId"
                    type="primary"
                    :href="`/api/files/${fileId}/content`"
                    target="_blank"
                  >
                    #{{ fileId }}
                  </el-link>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      <el-form v-if="detailRow" label-position="top" class="detail-form">
        <el-form-item v-for="field in detailFields" :key="field.prop" :label="field.label">
          <el-input :model-value="displayDetailCell(detailRow, field)" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="receiptDialogVisible" title="收款凭证" width="760px" draggable>
      <section v-if="receiptRow" class="receipt-print-area">
        <div class="receipt-title">
          <h2>智慧物业收费凭证</h2>
          <p>用于核对物业收款记录，正式发票以财务开票系统为准</p>
        </div>
        <div class="receipt-grid">
          <div>
            <span>凭证编号</span>
            <strong>{{ receiptNo }}</strong>
          </div>
          <div>
            <span>订单号</span>
            <strong>{{ receiptRow.orderNo ?? '-' }}</strong>
          </div>
          <div>
            <span>小区名称</span>
            <strong>{{ projectName(receiptRow.projectId) }}</strong>
          </div>
          <div>
            <span>业主/住户</span>
            <strong>{{ receiptRow.memberName ?? '-' }}</strong>
          </div>
          <div>
            <span>手机号</span>
            <strong>{{ receiptRow.memberMobile ?? '-' }}</strong>
          </div>
          <div>
            <span>房号</span>
            <strong>{{ receiptRow.houseNo ?? '-' }}</strong>
          </div>
          <div>
            <span>支付方式</span>
            <strong>{{ payChannelText(receiptRow.payChannel) }}</strong>
          </div>
          <div>
            <span>收款状态</span>
            <strong>{{ payOrderStatusText(receiptRow.status) }}</strong>
          </div>
          <div>
            <span>订单金额</span>
            <strong>{{ moneyText(receiptRow.amount) }} 元</strong>
          </div>
          <div>
            <span>实收金额</span>
            <strong>{{ moneyText(receiptRow.transactionAmount) }} 元</strong>
          </div>
          <div>
            <span>已退金额</span>
            <strong>{{ moneyText(receiptRow.refundedAmount) }} 元</strong>
          </div>
          <div>
            <span>可退金额</span>
            <strong>{{ moneyText(receiptRow.refundableAmount) }} 元</strong>
          </div>
          <div>
            <span>转预存款</span>
            <strong>{{ moneyText(receiptRow.prepaymentAmount) }} 元</strong>
          </div>
          <div>
            <span>预存款余额</span>
            <strong>{{ moneyText(receiptRow.prepaymentRemainingAmount) }} 元</strong>
          </div>
          <div>
            <span>收款时间</span>
            <strong>{{ receiptRow.paidAt ?? '-' }}</strong>
          </div>
        </div>
        <div class="receipt-summary">
          <span>费用明细</span>
          <p>{{ receiptRow.billSummary ?? '-' }}</p>
        </div>
        <div class="receipt-footer">
          <span>经办人：{{ auth.realName || '-' }}</span>
          <span>打印时间：{{ printTime }}</span>
        </div>
      </section>
      <template #footer>
        <el-button @click="receiptDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="printReceipt">打印凭证</el-button>
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
          <template v-else-if="field.type === 'fileUpload'">
            <el-upload
              drag
              multiple
              :show-file-list="false"
              :http-request="createActionAttachmentUploader(field)"
            >
              <div class="upload-placeholder">
                <strong>上传处理凭证</strong>
                <span>支持图片、PDF、表格等文件，上传后自动写入附件</span>
              </div>
            </el-upload>
            <div v-if="uploadedActionFiles[field.prop]?.length" class="action-attachments">
              <el-tag
                v-for="file in uploadedActionFiles[field.prop]"
                :key="file.fileId"
                closable
                @close="removeActionAttachment(field, file.fileId)"
              >
                <a :href="file.downloadUrl" target="_blank" rel="noreferrer">{{ file.originalName }}</a>
              </el-tag>
            </div>
          </template>
          <el-input
            v-else-if="field.type === 'password'"
            v-model="actionForm[field.prop]"
            type="password"
            show-password
            autocomplete="new-password"
            class="form-control"
          />
          <el-input v-else-if="field.type === 'textarea'" v-model="actionForm[field.prop]" type="textarea" :rows="4" />
          <el-input v-else v-model="actionForm[field.prop]" />
          <p v-if="field.help" class="field-help">{{ field.help }}</p>
        </el-form-item>
        <el-alert
          v-if="collectionActionSummary"
          class="collection-summary"
          :title="collectionActionSummary"
          type="info"
          show-icon
          :closable="false"
        />
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
import { useRoute, useRouter } from 'vue-router'
import { Bell, Download, Edit, Plus, Refresh, Search, SwitchButton, Tools, Upload } from '@element-plus/icons-vue'
import { pcaTextArr } from 'element-china-area-data'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { createRecord, downloadFile, fetchPage, postAction, putAction, updateRecord, uploadFile } from '@/api/admin'
import { allPages, type FieldConfig, type PageConfig } from '@/config/pages'
import { useAuthStore } from '@/store/auth'

type ActionScope = 'page' | 'row'
type ActionMethod = 'POST' | 'PUT' | 'DOWNLOAD' | 'CUSTOM'

interface ActionFileField extends FieldConfig {
  help?: string
  moduleCode?: string
}

interface UploadedActionFile {
  fileId: number
  originalName: string
  downloadUrl: string
}

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
  fields?: ActionFileField[]
  buildPayload?: (row?: Record<string, unknown>, form?: Record<string, unknown>) => Record<string, unknown>
  filename?: (row?: Record<string, unknown>) => string
  visible?: (row?: Record<string, unknown>) => boolean
}

interface ReconcileExceptionStats {
  totalCount: number
  highRiskCount: number
  mediumRiskCount: number
  lowRiskCount: number
  openCount: number
  pendingReviewCount: number
  handledCount: number
}

interface ReconcileReviewStats {
  totalCount: number
  pendingCount: number
  approvedCount: number
  rejectedCount: number
  resolvedCount: number
  stillAbnormalCount: number
}

const route = useRoute()
const router = useRouter()
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
const uploadedActionFiles = reactive<Record<string, UploadedActionFile[]>>({})
const dialogVisible = ref(false)
const actionDialogVisible = ref(false)
const bulkBuildingDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const receiptDialogVisible = ref(false)
const editingId = ref<string | number | null>(null)
const actionSaving = ref(false)
const bulkBuildingSaving = ref(false)
const memberSearchLoading = ref(false)
const currentAction = ref<BusinessAction | null>(null)
const currentActionRow = ref<Record<string, unknown> | undefined>()
const detailRow = ref<Record<string, unknown> | null>(null)
const receiptRow = ref<Record<string, unknown> | null>(null)
const reconcileHistories = ref<Record<string, unknown>[]>([])
const reconcileHistoryLoading = ref(false)
const reconcileStatsLoading = ref(false)
const reconcileStats = reactive<ReconcileExceptionStats>({
  totalCount: 0,
  highRiskCount: 0,
  mediumRiskCount: 0,
  lowRiskCount: 0,
  openCount: 0,
  pendingReviewCount: 0,
  handledCount: 0,
})
const reconcileReviewStatsLoading = ref(false)
const reconcileReviewStats = reactive<ReconcileReviewStats>({
  totalCount: 0,
  pendingCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
  resolvedCount: 0,
  stillAbnormalCount: 0,
})
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
  memberId: [],
  areaId: [],
  spaceId: [],
  equipmentId: [],
  userId: [],
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
  'system-users': [
    {
      key: 'system-user-reset-password',
      label: '重置密码',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/system/users/${row?.userId}/password`,
      type: 'warning',
      permission: 'system:user:update',
      fields: [
        {
          prop: 'password',
          label: '新密码',
          type: 'password',
          required: true,
          help: '密码长度不能小于8位。',
        },
      ],
    },
  ],
  'payment-orders': [
    {
      key: 'payment-order-receipt',
      label: '收款凭证',
      scope: 'row',
      method: 'CUSTOM',
      path: '',
      type: 'success',
      permission: 'payment:order:list',
      visible: (row) => ['PAID', 'REFUNDING', 'PARTIAL_REFUNDED', 'REFUNDED'].includes(String(row?.status ?? '')),
    },
    {
      key: 'payment-orders-export',
      label: '导出订单',
      scope: 'page',
      method: 'DOWNLOAD',
      path: () => `/payment/orders.csv${tableExportQuery()}`,
      type: 'primary',
      icon: Download,
      permission: 'payment:order:list',
      filename: () => `收款订单-${dateStamp()}.csv`,
    },
  ],
  'payment-transactions': [
    {
      key: 'payment-transactions-export',
      label: '导出流水',
      scope: 'page',
      method: 'DOWNLOAD',
      path: () => `/payment/transactions.csv${tableExportQuery()}`,
      type: 'primary',
      icon: Download,
      permission: 'payment:transaction:list',
      filename: () => `支付流水-${dateStamp()}.csv`,
    },
  ],
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
      fields: [
        {
          prop: 'amount',
          label: '收款金额',
          type: 'number',
          required: true,
          help: '生成收款码的金额。可小于、等于或大于应收金额：少收时保留待收款，超收支付成功后转为业主/住户预存款。',
        },
      ],
      buildPayload: (row, formData = {}) => ({
        projectId: Number(row?.projectId),
        billIds: billCollectionIds(row),
        payChannel: 'WECHAT',
        amount: formData.amount,
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
      key: 'refunds-export',
      label: '导出退款',
      scope: 'page',
      method: 'DOWNLOAD',
      path: () => `/payment/refunds.csv${tableExportQuery()}`,
      type: 'primary',
      icon: Download,
      permission: 'payment:refund:list',
      filename: () => `退款管理-${dateStamp()}.csv`,
    },
    {
      key: 'refund-approve',
      label: '通过',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/refunds/${row?.refundId}/audit`,
      type: 'success',
      permission: 'payment:refund:audit',
      visible: (row) => row?.status === 'APPLYING',
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
      visible: (row) => row?.status === 'APPLYING',
      fields: [{ prop: 'auditRemark', label: '拒绝原因', type: 'textarea', required: true }],
      buildPayload: (_row, formData = {}) => ({ auditResult: 'REJECTED', auditRemark: formData.auditRemark }),
    },
    {
      key: 'refund-offline-confirm',
      label: '确认退款',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/refunds/${row?.refundId}/offline-confirm`,
      type: 'warning',
      permission: 'payment:refund:audit',
      visible: (row) => row?.status === 'REFUNDING',
      confirm: '确认该退款已线下完成？确认后会写入退款流水并更新账单/订单退款状态。',
      fields: [{ prop: 'auditRemark', label: '退款备注', type: 'textarea', required: true }],
      buildPayload: (_row, formData = {}) => ({ auditResult: 'APPROVED', auditRemark: formData.auditRemark }),
    },
  ],
  'payment-reconcile-exceptions': [
    {
      key: 'reconcile-view-pending-reviews',
      label: '查看待复核',
      scope: 'page',
      method: 'CUSTOM',
      path: '',
      type: 'warning',
      icon: Search,
      permission: 'payment:reconcile:view',
    },
    {
      key: 'reconcile-exception-handle',
      label: '标记已处理',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/reconcile/exceptions/${encodeURIComponent(String(row?.exceptionKey ?? ''))}/handle`,
      type: 'success',
      permission: 'payment:reconcile:view',
      visible: (row) => row?.status !== 'HANDLED',
      fields: [
        { prop: 'handleRemark', label: '处理备注', type: 'textarea', required: true },
        { prop: 'attachmentFileIds', label: '处理凭证', type: 'fileUpload', moduleCode: 'reconcile', help: '高风险异常必须上传处理凭证；高/中风险处理后进入复核，低风险处理后自动归档。' },
      ],
      buildPayload: (_row, formData = {}) => ({
        handleRemark: formData.handleRemark,
        attachmentFileIds: formData.attachmentFileIds,
      }),
    },
    {
      key: 'reconcile-exception-review-approve',
      label: '复核通过',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/reconcile/exceptions/${encodeURIComponent(String(row?.exceptionKey ?? ''))}/review`,
      type: 'success',
      permission: 'payment:reconcile:view',
      visible: (row) => needsReconcileReview(row),
      fields: [
        {
          prop: 'reviewRemark',
          label: '复核备注',
          type: 'textarea',
          help: '复核通过前系统会重新复算；若该异常仍存在，系统会阻止通过。',
        },
      ],
      buildPayload: (_row, formData = {}) => ({ reviewResult: 'APPROVED', reviewRemark: formData.reviewRemark }),
    },
    {
      key: 'reconcile-exception-review-reject',
      label: '退回重办',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/reconcile/exceptions/${encodeURIComponent(String(row?.exceptionKey ?? ''))}/review`,
      type: 'warning',
      permission: 'payment:reconcile:view',
      visible: (row) => needsReconcileReview(row),
      fields: [{ prop: 'reviewRemark', label: '退回原因', type: 'textarea', required: true }],
      buildPayload: (_row, formData = {}) => ({ reviewResult: 'REJECTED', reviewRemark: formData.reviewRemark }),
    },
  ],
  'payment-reconcile-reviews': [
    {
      key: 'reconcile-review-approve',
      label: '复核通过',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/reconcile/exceptions/${encodeURIComponent(String(row?.exceptionKey ?? ''))}/review`,
      type: 'success',
      permission: 'payment:reconcile:view',
      visible: (row) => needsReconcileReview(row),
      fields: [
        {
          prop: 'reviewRemark',
          label: '复核备注',
          type: 'textarea',
          help: '只有复算状态为“复算已解决”时才允许复核通过；仍异常会被系统阻止。',
        },
      ],
      buildPayload: (_row, formData = {}) => ({ reviewResult: 'APPROVED', reviewRemark: formData.reviewRemark }),
    },
    {
      key: 'reconcile-review-reject',
      label: '退回重办',
      scope: 'row',
      method: 'POST',
      path: (row) => `/payment/reconcile/exceptions/${encodeURIComponent(String(row?.exceptionKey ?? ''))}/review`,
      type: 'warning',
      permission: 'payment:reconcile:view',
      visible: (row) => needsReconcileReview(row),
      fields: [{ prop: 'reviewRemark', label: '退回原因', type: 'textarea', required: true }],
      buildPayload: (_row, formData = {}) => ({ reviewResult: 'REJECTED', reviewRemark: formData.reviewRemark }),
    },
  ],
  'member-bindings': [
    {
      key: 'member-binding-approve',
      label: '审核通过',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/base/member-bindings/${row?.bindId}/audit`,
      type: 'success',
      permission: 'base:memberBinding:audit',
      visible: (row) => rowStatus(row) === 'PENDING',
      fields: [{ prop: 'auditRemark', label: '审核备注', type: 'textarea' }],
      buildPayload: (_row, formData = {}) => ({ auditResult: 'APPROVED', auditRemark: formData.auditRemark }),
    },
    {
      key: 'member-binding-reject',
      label: '审核驳回',
      scope: 'row',
      method: 'PUT',
      path: (row) => `/base/member-bindings/${row?.bindId}/audit`,
      type: 'danger',
      permission: 'base:memberBinding:audit',
      visible: (row) => rowStatus(row) === 'PENDING',
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
      visible: (row) => rowStatus(row) === 'APPROVED',
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
        { prop: 'projectId', label: '小区名称', type: 'project' },
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
const tableColumns = computed(() => config.value.columns.filter((field) => !field.tableHidden))
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
  if (config.value.key === 'system-users' && editingId.value) {
    return formFields.value.filter((field) => field.prop !== 'password')
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
const isReconcileExceptionPage = computed(() => config.value.key === 'payment-reconcile-exceptions')
const isReconcileReviewPage = computed(() => config.value.key === 'payment-reconcile-reviews')
const isReconcileExceptionDetail = computed(() => ['payment-reconcile-exceptions', 'payment-reconcile-reviews'].includes(config.value.key))
const hasOperationColumn = computed(() => Boolean((config.value.updatePath && canUpdate.value) || config.value.showDetails || rowActions.value.length))
const operationWidth = computed(() => {
  if (config.value.showDetails && config.value.updatePath && canUpdate.value) return 180
  if (config.value.key === 'member-bindings') return 340
  return rowActions.value.length > 3 ? 360 : rowActions.value.length > 1 ? 260 : 140
})
const collectionActionSummary = computed(() => {
  if (currentAction.value?.key !== 'bill-cash-collect' && currentAction.value?.key !== 'bill-qr-collect') return ''
  const amount = Number(actionForm.amount)
  const receivable = billCollectionRemainingAmount(currentActionRow.value)
  if (!Number.isFinite(amount) || amount <= 0 || receivable <= 0) return ''
  const diff = Math.round((amount - receivable) * 100) / 100
  if (diff < 0) return `本次少收 ${moneyText(Math.abs(diff))}，收款后账单会保留待收款。`
  if (diff > 0) return `本次超收 ${moneyText(diff)}，收款成功后超出部分会转为业主/住户预存款。`
  return '本次收款等于当前待收金额，收款成功后账单将结清。'
})
const receiptNo = computed(() => `RCPT-${receiptRow.value?.orderNo ?? '-'}`)
const printTime = computed(() => new Date().toLocaleString('zh-CN', { hour12: false }))

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
    await loadReconcileStats()
    await loadReconcileReviewStats()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadReconcileStats() {
  if (!isReconcileExceptionPage.value) return
  reconcileStatsLoading.value = true
  try {
    const params: Record<string, string | number> = {}
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== '') params[key] = value
    })
    const { data } = await fetchPage('/payment/reconcile/exceptions/stats', params)
    Object.assign(reconcileStats, normalizeReconcileStats(data.data))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '异常统计加载失败'
  } finally {
    reconcileStatsLoading.value = false
  }
}

async function loadReconcileReviewStats() {
  if (!isReconcileReviewPage.value) return
  reconcileReviewStatsLoading.value = true
  try {
    const params: Record<string, string | number> = {}
    Object.entries(filters).forEach(([key, value]) => {
      if (key === 'reviewStatus' || key === 'currentCheckStatus') return
      if (value !== '') params[key] = value
    })
    const { data } = await fetchPage('/payment/reconcile/reviews/stats', params)
    Object.assign(reconcileReviewStats, normalizeReconcileReviewStats(data.data))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '复核统计加载失败'
  } finally {
    reconcileReviewStatsLoading.value = false
  }
}

function normalizeReconcileStats(value: unknown): ReconcileExceptionStats {
  const item = (value && typeof value === 'object' ? value : {}) as Record<string, unknown>
  return {
    totalCount: numberValue(item.totalCount),
    highRiskCount: numberValue(item.highRiskCount),
    mediumRiskCount: numberValue(item.mediumRiskCount),
    lowRiskCount: numberValue(item.lowRiskCount),
    openCount: numberValue(item.openCount),
    pendingReviewCount: numberValue(item.pendingReviewCount),
    handledCount: numberValue(item.handledCount),
  }
}

function normalizeReconcileReviewStats(value: unknown): ReconcileReviewStats {
  const item = (value && typeof value === 'object' ? value : {}) as Record<string, unknown>
  return {
    totalCount: numberValue(item.totalCount),
    pendingCount: numberValue(item.pendingCount),
    approvedCount: numberValue(item.approvedCount),
    rejectedCount: numberValue(item.rejectedCount),
    resolvedCount: numberValue(item.resolvedCount),
    stillAbnormalCount: numberValue(item.stillAbnormalCount),
  }
}

function reset() {
  Object.keys(filters).forEach((key) => {
    filters[key] = ''
  })
  applyDefaultFilters()
  pageNo.value = 1
  if (Object.keys(route.query).length) {
    router.replace({ path: route.path, query: {} })
    return
  }
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
  if (config.value.key === 'system-users' && Array.isArray(row.roleIds) && row.roleIds.length) {
    form.roleId = Number(row.roleIds[0])
  }
  loadFormRemoteOptions()
  dialogVisible.value = true
}

function openDetail(row: Record<string, unknown>) {
  detailRow.value = row
  detailDialogVisible.value = true
  if (isReconcileExceptionDetail.value) {
    loadReconcileHistory(row)
  }
}

function reconcileStatusText(status: unknown) {
  return status === 'HANDLED' ? '已处理' : '未处理'
}

function reconcileReviewStatusText(status: unknown) {
  const labels: Record<string, string> = {
    NONE: '无需复核',
    PENDING: '待复核',
    APPROVED: '复核通过',
    REJECTED: '退回重办',
  }
  return labels[String(status ?? 'NONE')] ?? '无需复核'
}

function reconcileLevelTagType(level: unknown) {
  if (isHighRiskLevel(level)) return 'danger'
  if (isLowRiskLevel(level)) return 'success'
  return 'warning'
}

function reconcileHistoryActionText(actionType: unknown) {
  const labels: Record<string, string> = {
    HANDLE: '标记已处理',
    REVIEW_APPROVE: '复核通过',
    REVIEW_REJECT: '退回重办',
    AUTO_ARCHIVE: '自动归档',
  }
  return labels[String(actionType ?? '')] ?? String(actionType ?? '-')
}

async function loadReconcileHistory(row: Record<string, unknown>) {
  const exceptionKey = String(row.exceptionKey ?? '')
  reconcileHistories.value = []
  if (!exceptionKey) return
  reconcileHistoryLoading.value = true
  try {
    const { data } = await fetchPage(`/payment/reconcile/exceptions/${encodeURIComponent(exceptionKey)}/history`, {
      pageNo: 1,
      pageSize: 20,
    })
    const payload = data.data
    reconcileHistories.value = Array.isArray(payload?.records) ? payload.records : []
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载处理历史失败')
  } finally {
    reconcileHistoryLoading.value = false
  }
}

function reconcileCalculationText(row: Record<string, unknown>) {
  const type = String(row.exceptionType ?? '')
  if (type === '订单缺少支付流水') return '系统发现订单处于已支付、退款中、已退款或部分退款状态，但支付流水合计为 0。'
  if (type === '支付流水订单异常') return '系统发现支付流水无法匹配有效的已支付订单，可能是订单状态异常、重复流水或孤立流水。'
  if (type === '退款缺少退款流水') return '系统发现退款单已标记为已退款，但没有对应的退款流水。'
  if (type === '订单金额不一致') return '系统按“订单金额 = 订单账单核销金额 + 订单转入预存款金额”校验。'
  if (type === '订单实收核销不平') return '系统按“实收流水金额 = 账单核销金额 + 预存款转入金额”校验。'
  if (type === '退款流水金额不平') return '系统按“退款单金额 = 退款流水金额”校验已完成退款。'
  if (type === '预存款余额异常') return '系统按“预存款金额 = 剩余金额 + 已抵扣金额 + 已退款金额”校验，并检查余额是否小于 0 或大于原始金额。'
  if (type === '账单金额状态异常') return '系统校验账单状态与待收金额是否一致，例如已缴账单待收应为 0，未缴或部分缴费账单待收应大于 0。'
  return '系统按当前收费账务规则自动计算异常，建议结合业务单号和相关流水核对。'
}

function reconcileSuggestionText(row: Record<string, unknown>) {
  const type = String(row.exceptionType ?? '')
  if (type === '订单缺少支付流水') return '先核对收款订单是否真实收款；若确已收款，补齐或修复支付流水；若未收款，应修正订单状态。'
  if (type === '支付流水订单异常') return '先核对第三方或现金收款记录，再检查订单是否被误作废、误关闭或重复创建。'
  if (type === '退款缺少退款流水') return '先核对实际退款凭证；若已退款，补齐退款流水；若未退款，应恢复退款单状态。'
  if (type === '订单金额不一致' || type === '订单实收核销不平') return '重点检查该订单关联账单、核销金额和预存款转入记录，确认是否存在少核销、重复核销或超收未转预存。'
  if (type === '退款流水金额不平') return '核对退款单、退款流水和原收款订单，确认是否部分退款、重复退款或退款流水金额录入错误。'
  if (type === '预存款余额异常') return '核对预存款生成来源和抵扣明细，必要时修正抵扣记录或预存款余额。'
  if (type === '账单金额状态异常') return '核对该账单的应收、已收、已退、待收和状态；金额正确后再修正账单状态。'
  return '打开关联业务记录，核对金额、状态和流水后再标记为已处理。'
}

function reconcileLevelRuleText(row: Record<string, unknown>) {
  if (isHighRiskLevel(row.exceptionLevel)) return '高风险异常涉及账实不一致、支付/退款流水缺失或预存款余额异常，必须上传处理凭证，处理后进入复核池。'
  if (isLowRiskLevel(row.exceptionLevel)) return '低风险异常处理时需要填写处理备注，提交后系统自动归档，不再进入人工复核池。'
  return '中风险异常需要填写处理备注，处理后进入复核池，由复核人员确认复算结果后关闭或退回。'
}

function reconcileMissingActionText(row: Record<string, unknown>) {
  if (row.status !== 'HANDLED') {
    if (isHighRiskLevel(row.exceptionLevel) && !attachmentIds(row.attachmentFileIds).length) {
      return '当前缺口：待处理，且高风险异常还缺少处理凭证。'
    }
    return '当前缺口：待处理，需要先完成业务修复并填写处理备注。'
  }
  if (row.reviewStatus === 'PENDING') return '当前缺口：已处理，等待复核人员确认复算结果。'
  if (row.reviewStatus === 'REJECTED') return '当前缺口：复核已退回，需要重新修复后再次标记已处理。'
  return '当前状态：规则要求已满足。'
}

function reconcileTargets(row: Record<string, unknown>) {
  const businessType = String(row.businessType ?? '')
  const exceptionType = String(row.exceptionType ?? '')
  const businessNo = String(row.businessNo ?? '')
  const memberName = String(row.memberName ?? '')
  const memberMobile = String(row.memberMobile ?? '')
  const targets: Array<{ label: string; path: string }> = []
  const addTarget = (label: string, path: string) => {
    if (!targets.some((target) => target.path === path)) {
      targets.push({ label, path })
    }
  }
  const queryPath = (path: string, params: Record<string, unknown>) => {
    const search = new URLSearchParams()
    const exceptionKey = String(row.exceptionKey ?? '').trim()
    if (exceptionKey) {
      search.set('reconcileExceptionKey', exceptionKey)
      search.set('reconcileReturnPath', route.path)
    }
    Object.entries(params).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return
      search.set(key, String(value))
    })
    const query = search.toString()
    return query ? `${path}?${query}` : path
  }

  if (businessType === '支付订单') {
    addTarget('修复收款订单', queryPath('/payment/orders', { orderNo: businessNo }))
    addTarget('核对支付流水', queryPath('/payment/transactions', { orderNo: businessNo }))
    addTarget('核对账单核销', queryPath('/fee/bills', { memberName, memberMobile }))
    if (exceptionType.includes('预存')) {
      addTarget('核对预存款', queryPath('/payment/prepayments', { orderNo: businessNo, memberName }))
    }
  } else if (businessType === '支付流水') {
    addTarget('修复支付流水', queryPath('/payment/transactions', { orderNo: businessNo }))
    addTarget('核对收款订单', queryPath('/payment/orders', { orderNo: businessNo }))
  } else if (businessType === '退款单') {
    addTarget('修复退款单', queryPath('/payment/refunds', { refundNo: businessNo }))
    addTarget('核对原收款订单', queryPath('/payment/orders', { memberName, memberMobile }))
  } else if (businessType === '收费账单') {
    addTarget('修复账单', queryPath('/fee/bills', { billNo: businessNo }))
    addTarget('核对收款订单', queryPath('/payment/orders', { memberName, memberMobile }))
  } else if (businessType === '业主预存款') {
    addTarget('修复预存款', queryPath('/payment/prepayments', { orderNo: businessNo, memberName }))
    addTarget('核对收款订单', queryPath('/payment/orders', { orderNo: businessNo, memberName }))
  }
  if (memberName) {
    addTarget('查看业主/住户', queryPath('/base/members', { realName: memberName }))
  } else if (memberMobile) {
    addTarget('查看业主/住户', queryPath('/base/members', { mobile: memberMobile }))
  }
  return targets
}

function goToReconcileTarget(path: string) {
  detailDialogVisible.value = false
  router.push(path)
}

function reconcileFollowUpQuery(row?: Record<string, unknown>) {
  const query: Record<string, string> = {}
  const projectId = String(row?.projectId ?? '').trim()
  const exceptionType = String(row?.exceptionType ?? '').trim()
  const memberName = String(row?.memberName ?? '').trim()
  if (projectId) query.projectId = projectId
  if (exceptionType) query.exceptionType = exceptionType
  if (memberName) query.memberName = memberName
  return query
}

function reconcileReviewQueueQuery() {
  const query: Record<string, string> = { reviewStatus: 'PENDING' }
  ;['projectId', 'exceptionType', 'memberName'].forEach((key) => {
    const value = String(filters[key] ?? '').trim()
    if (value) query[key] = value
  })
  return query
}

function goToPendingReconcileReviews(row?: Record<string, unknown>) {
  const query = row
    ? { reviewStatus: 'PENDING', ...reconcileFollowUpQuery(row) }
    : reconcileReviewQueueQuery()
  router.push({ path: '/payment/reconcile/reviews', query })
}

function setReconcileStatusFilter(status: string) {
  filters.status = status
  pageNo.value = 1
  load()
}

function setReconcileReviewStatusFilter(status: string) {
  filters.reviewStatus = status
  pageNo.value = 1
  load()
}

function setReconcileCurrentCheckFilter(status: string) {
  filters.currentCheckStatus = status
  pageNo.value = 1
  load()
}

async function handleReconcileActionSuccess(action: BusinessAction, row?: Record<string, unknown>) {
  if (action.key === 'reconcile-exception-handle') {
    if (isLowRiskReconcileException(row)) {
      ElMessage.success('低风险异常已处理并自动归档')
      return
    }
    try {
      await ElMessageBox.confirm('该异常已进入复核池，是否现在去“异常复核”继续处理？', '处理完成', {
        confirmButtonText: '去复核',
        cancelButtonText: '留在当前页',
        type: 'success',
      })
      await goToPendingReconcileReviews(row)
    } catch {
      // User chose to stay on the current page.
    }
    return
  }

  if (action.key.startsWith('reconcile-review-') || action.key.startsWith('reconcile-exception-review-')) {
    try {
      await ElMessageBox.confirm('复核结果已记录，是否返回“账务异常”查看最新状态？', '复核完成', {
        confirmButtonText: '返回异常池',
        cancelButtonText: '留在当前页',
        type: 'success',
      })
      await router.push({
        path: '/payment/reconcile/exceptions',
        query: reconcileFollowUpQuery(row),
      })
    } catch {
      // User chose to stay on the current page.
    }
  }
}

function applyRouteQueryFilters() {
  const allowed = new Set(['projectId', ...filterFields.value.map((field) => field.prop)])
  Object.entries(route.query).forEach(([key, value]) => {
    if (!allowed.has(key)) return
    const firstValue = Array.isArray(value) ? value[0] : value
    if (firstValue === undefined || firstValue === null) return
    filters[key] = String(firstValue)
  })
}

function applyDefaultFilters() {
  Object.entries(config.value.defaultFilters ?? {}).forEach(([key, value]) => {
    filters[key] = value
  })
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
  if (config.value.key === 'system-users' && !editingId.value) {
    const password = String(form.password ?? '')
    if (password.length < 8) {
      ElMessage.warning('初始密码长度不能小于8位')
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
    if (config.value.key === 'system-users') {
      const roleId = payload.roleId
      if (roleId !== undefined && roleId !== null && roleId !== '') {
        payload.roleIds = [Number(roleId)]
      }
      delete payload.roleId
    }
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

function numberValue(value: unknown) {
  const count = Number(value ?? 0)
  return Number.isFinite(count) ? count : 0
}

function payChannelText(value: unknown) {
  const labels: Record<string, string> = {
    WECHAT: '微信支付',
    ALI: '支付宝',
    CASH: '现金',
    POS: 'POS刷卡',
    BANK_TRANSFER: '银行转账',
    OFFLINE: '线下收款',
  }
  return typeof value === 'string' ? labels[value] ?? value : '-'
}

function payOrderStatusText(value: unknown) {
  const labels: Record<string, string> = {
    PENDING: '待支付',
    PAYING: '支付中',
    PAID: '已支付',
    CLOSED: '已关闭',
    FAILED: '支付失败',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    PARTIAL_REFUNDED: '部分退款',
  }
  return typeof value === 'string' ? labels[value] ?? value : '-'
}

function rowStatus(row?: Record<string, unknown> | null) {
  return String(row?.status ?? '').trim().toUpperCase()
}

function printReceipt() {
  window.print()
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

function optionText(type: 'project' | 'building' | 'unit' | 'house' | 'member' | 'parkingArea' | 'parkingSpace' | 'patrolAsset' | 'user', value: unknown) {
  const keyMap = {
    project: 'projectId',
    building: 'buildingId',
    unit: 'unitId',
    house: 'houseId',
    member: 'memberId',
    parkingArea: 'areaId',
    parkingSpace: 'spaceId',
    patrolAsset: 'equipmentId',
    user: 'userId',
  } as const
  const option = remoteOptions[keyMap[type]].find((item) => optionValue(item) === value)
  return option ? optionLabel(option) : value ?? '-'
}

type SelectOption = string | {
  label: string
  value: string | number | boolean
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
    'member',
    'parkingArea',
    'parkingSpace',
    'patrolAsset',
    'user',
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
  if (field.type === 'member') {
    return remoteOptions.memberId
  }
  if (field.type === 'parkingArea') {
    return remoteOptions.areaId
  }
  if (field.type === 'parkingSpace') {
    return remoteOptions.spaceId
  }
  if (field.type === 'patrolAsset') {
    return remoteOptions.equipmentId
  }
  if (field.type === 'user') {
    return remoteOptions.userId
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
    form.inviterMemberId = undefined
    form.memberId = undefined
    form.areaId = undefined
    form.spaceId = undefined
    form.equipmentId = undefined
    form.standardId = undefined
    loadBuildings()
    loadMembers()
    loadParkingAreas()
    loadParkingSpaces()
    loadPatrolAssets()
    loadFeeStandards()
    remoteOptions.unitId = []
    remoteOptions.houseId = []
    remoteOptions.memberId = []
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
    form.inviterMemberId = undefined
    form.memberId = undefined
    loadUnits()
    remoteOptions.houseId = []
    remoteOptions.memberId = []
  }
  if (field.type === 'unit') {
    form.houseId = undefined
    form.inviterMemberId = undefined
    form.memberId = undefined
    loadHouses()
    loadMembers()
  }
  if (field.type === 'house') {
    form.inviterMemberId = undefined
    form.memberId = undefined
    loadMembers()
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

function syncActionAttachmentField(field: FieldConfig) {
  actionForm[field.prop] = (uploadedActionFiles[field.prop] ?? []).map((file) => file.fileId).join(',')
}

function createActionAttachmentUploader(field: ActionFileField) {
  return (options: { file: File; onSuccess?: (response: unknown) => void; onError?: (error: Error) => void }) =>
    uploadActionAttachment(field, options)
}

async function uploadActionAttachment(field: ActionFileField, options: { file: File; onSuccess?: (response: unknown) => void; onError?: (error: Error) => void }) {
  const projectId = Number(currentActionRow.value?.projectId ?? actionForm.projectId ?? filters.projectId)
  if (!Number.isFinite(projectId) || projectId <= 0) {
    const error = new Error('请先选择或确认小区后再上传附件')
    ElMessage.warning(error.message)
    options.onError?.(error)
    return
  }
  const data = new FormData()
  data.append('projectId', String(projectId))
  data.append('moduleCode', field.moduleCode ?? 'reconcile')
  data.append('sensitive', 'false')
  data.append('file', options.file)
  try {
    const response = await uploadFile(data)
    const payload = response.data?.data
    const fileId = Number(payload?.fileId)
    if (!Number.isFinite(fileId) || fileId <= 0) {
      throw new Error('上传成功但未返回文件ID')
    }
    const file: UploadedActionFile = {
      fileId,
      originalName: String(payload?.originalName ?? options.file.name),
      downloadUrl: String(payload?.downloadUrl ?? `/api/files/${fileId}/content`),
    }
    uploadedActionFiles[field.prop] = [...(uploadedActionFiles[field.prop] ?? []), file]
    syncActionAttachmentField(field)
    ElMessage.success('附件上传成功')
    options.onSuccess?.(payload)
  } catch (err) {
    const error = err instanceof Error ? err : new Error('附件上传失败')
    ElMessage.error(error.message)
    options.onError?.(error)
  }
}

function removeActionAttachment(field: FieldConfig, fileId: number) {
  uploadedActionFiles[field.prop] = (uploadedActionFiles[field.prop] ?? []).filter((file) => file.fileId !== fileId)
  syncActionAttachmentField(field)
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
  if (field.type === 'member') {
    return row.inviterMemberName || row.memberName || optionText('member', value)
  }
  if (field.prop === 'roomNo') {
    return value || [row.buildingName, row.unitName, row.houseNo].filter(Boolean).join('') || ''
  }
  if (field.type === 'parkingArea' && row.areaName) {
    return row.areaName
  }
  if (field.type === 'parkingSpace' && row.spaceNo) {
    return [row.areaName, row.spaceNo].filter(Boolean).join(' - ')
  }
  if (field.type === 'patrolAsset') {
    return optionText('patrolAsset', value)
  }
  if (field.type === 'user') {
    if (field.prop === 'handlerUserId' && row.handlerUserName) return row.handlerUserName
    return optionText('user', value)
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

function attachmentIds(value: unknown) {
  return String(value ?? '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function optionsForDisplay(field: FieldConfig) {
  if (field.type === 'project') return remoteOptions.projectId
  if (field.type === 'building') return remoteOptions.buildingId
  if (field.type === 'unit') return remoteOptions.unitId
  if (field.type === 'house') return remoteOptions.houseId
  if (field.type === 'member') return remoteOptions.memberId
  if (field.type === 'parkingArea') return remoteOptions.areaId
  if (field.type === 'parkingSpace') return remoteOptions.spaceId
  if (field.type === 'patrolAsset') return remoteOptions.equipmentId
  if (field.type === 'user') return remoteOptions.userId
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
    'member',
    'parkingArea',
    'parkingSpace',
    'patrolAsset',
    'user',
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
  await loadMembers()
  await loadParkingAreas()
  await loadParkingSpaces()
  await loadPatrolAssets()
  await loadUsers()
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
  await loadMembers()
  await loadParkingAreas()
  await loadParkingSpaces()
  await loadPatrolAssets()
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
    label: String([item.buildingName, item.unitName, item.houseNo].filter(Boolean).join('') || item.houseNo || item.houseId),
    value: Number(item.houseId),
  }))
}

async function loadMembers() {
  const projectId = Number(form.projectId ?? filters.projectId)
  const houseId = Number(form.houseId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200, status: 'ACTIVE' }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  const { data } = await fetchPage('/base/members', params)
  remoteOptions.memberId = toRecords(data.data)
    .filter((item) => !Number.isFinite(houseId) || houseId <= 0 || Number(item.houseId) === houseId)
    .map((item) => ({
      label: String([
        item.realName,
        item.mobile,
        item.roomNo || item.houseNo,
      ].filter(Boolean).join(' / ') || item.memberId),
      value: Number(item.memberId),
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

async function loadPatrolAssets() {
  const projectId = Number(form.projectId ?? filters.projectId)
  const params: Record<string, string | number> = { pageNo: 1, pageSize: 200 }
  if (Number.isFinite(projectId) && projectId > 0) params.projectId = projectId
  const { data } = await fetchPage('/patrol/assets', params)
  remoteOptions.equipmentId = toRecords(data.data).map((item) => ({
    label: String([item.equipmentName, item.equipmentCode].filter(Boolean).join(' - ') || item.equipmentId),
    value: Number(item.equipmentId),
  }))
}

async function loadUsers() {
  const { data } = await fetchPage('/system/users', { pageNo: 1, pageSize: 200, status: 'ACTIVE' })
  remoteOptions.userId = toRecords(data.data).map((item) => ({
    label: String([
      item.realName || item.username,
      item.mobile,
    ].filter(Boolean).join(' / ') || item.userId),
    value: Number(item.userId),
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

function tableExportQuery() {
  return toQuery(filters)
}

function dateStamp() {
  return new Date().toISOString().slice(0, 10)
}

function resolvePath(action: BusinessAction, row?: Record<string, unknown>, formData: Record<string, unknown> = {}) {
  return typeof action.path === 'function' ? action.path(row, formData) : action.path
}

async function runAction(action: BusinessAction, row?: Record<string, unknown>) {
  if (action.method === 'CUSTOM') {
    runCustomAction(action, row)
    return
  }
  currentAction.value = action
  currentActionRow.value = row
  Object.keys(actionForm).forEach((key) => delete actionForm[key])
  Object.keys(uploadedActionFiles).forEach((key) => delete uploadedActionFiles[key])
  ;(action.fields ?? []).forEach((field) => {
    actionForm[field.prop] = undefined
  })
  if (action.key === 'bill-cash-collect' || action.key === 'bill-qr-collect') {
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

function runCustomAction(action: BusinessAction, row?: Record<string, unknown>) {
  if (action.key === 'payment-order-receipt' && row) {
    receiptRow.value = row
    receiptDialogVisible.value = true
  }
  if (action.key === 'reconcile-view-pending-reviews') {
    goToPendingReconcileReviews()
  }
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
  if (currentAction.value.key === 'bill-cash-collect' || currentAction.value.key === 'bill-qr-collect') {
    const amount = Number(actionForm.amount)
    if (!Number.isFinite(amount) || amount <= 0) {
      ElMessage.warning('收款金额必须大于0')
      return
    }
  }
  if (currentAction.value.key === 'system-user-reset-password') {
    const password = String(actionForm.password ?? '')
    if (password.length < 8) {
      ElMessage.warning('新密码长度不能小于8位')
      return
    }
  }
  if (currentAction.value.key === 'reconcile-exception-handle' && isHighRiskReconcileException(currentActionRow.value)) {
    if (!attachmentIds(actionForm.attachmentFileIds).length) {
      ElMessage.warning('高风险对账异常必须上传处理凭证后才能标记已处理')
      return
    }
  }
  if (currentAction.value.confirm) {
    await ElMessageBox.confirm(currentAction.value.confirm, currentAction.value.label, {
      type: currentAction.value.type === 'danger' ? 'warning' : 'info',
    })
  }
  await executeAction(currentAction.value, currentActionRow.value, compactPayload(actionForm))
  actionDialogVisible.value = false
}

function isHighRiskReconcileException(row?: Record<string, unknown> | null) {
  return isHighRiskLevel(row?.exceptionLevel)
}

function needsReconcileReview(row?: Record<string, unknown>) {
  return row?.status === 'HANDLED' && row?.reviewStatus === 'PENDING' && !isLowRiskReconcileException(row)
}

function isLowRiskReconcileException(row?: Record<string, unknown> | null) {
  return isLowRiskLevel(row?.exceptionLevel)
}

function isHighRiskLevel(value: unknown) {
  const level = String(value ?? '').trim().toUpperCase()
  return level === '高' || level === 'HIGH' || level === 'CRITICAL'
}

function isLowRiskLevel(value: unknown) {
  const level = String(value ?? '').trim().toUpperCase()
  return level === '低' || level === 'LOW'
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
    await handleReconcileActionSuccess(action, row)
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
  () => route.fullPath,
  () => {
    pageNo.value = 1
    Object.keys(filters).forEach((key) => {
      filters[key] = ''
    })
    applyDefaultFilters()
    applyRouteQueryFilters()
    load()
  },
)

watch(memberSearchKeyword, () => {
  if (!dialogVisible.value || !needsMemberPicker.value) return
  scheduleMemberSearch()
})

onMounted(() => {
  applyDefaultFilters()
  applyRouteQueryFilters()
  load()
})
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

.upload-placeholder {
  display: grid;
  gap: 4px;
  color: #475569;
  line-height: 1.5;
}

.upload-placeholder strong {
  color: #111827;
  font-size: 14px;
}

.upload-placeholder span {
  color: #64748b;
  font-size: 12px;
}

.action-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.action-attachments a {
  color: inherit;
  text-decoration: none;
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

.reconcile-stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(108px, 1fr)) minmax(138px, 1.2fr);
  gap: 10px;
  margin-bottom: 14px;
}

.reconcile-stats--review {
  grid-template-columns: repeat(5, minmax(118px, 1fr)) minmax(138px, 1.2fr);
}

.reconcile-stats__card,
.reconcile-stats__total {
  display: grid;
  gap: 6px;
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

button.reconcile-stats__card {
  cursor: pointer;
  text-align: left;
}

button.reconcile-stats__card:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.reconcile-stats__card span,
.reconcile-stats__total span {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.reconcile-stats__card strong,
.reconcile-stats__total strong {
  color: #111827;
  font-size: 26px;
  line-height: 1;
}

.reconcile-stats__card--danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.reconcile-stats__card--danger strong {
  color: #dc2626;
}

.reconcile-stats__card--warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.reconcile-stats__card--warning strong {
  color: #d97706;
}

.reconcile-stats__card--success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.reconcile-stats__card--success strong {
  color: #16a34a;
}

.reconcile-stats__card--muted {
  background: #f8fafc;
}

.reconcile-stats__card--link {
  border-color: #fed7aa;
  background: #fff7ed;
}

.reconcile-stats__card--link strong {
  color: #ea580c;
}

.reconcile-stats__total {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.reconcile-stats__total strong {
  color: #2563eb;
}

.reconcile-detail {
  display: grid;
  gap: 14px;
  margin-bottom: 16px;
}

.reconcile-detail__summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid #d8e4e1;
  border-radius: 8px;
  background: #f7fbfa;
}

.reconcile-detail__summary div {
  display: grid;
  gap: 4px;
  margin-right: auto;
}

.reconcile-detail__summary span,
.reconcile-detail__grid span {
  color: #64748b;
  font-size: 12px;
}

.reconcile-detail__summary strong {
  color: #dc2626;
  font-size: 24px;
  font-weight: 800;
}

.reconcile-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.reconcile-detail__grid div,
.reconcile-detail__section {
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.reconcile-detail__grid div {
  display: grid;
  gap: 6px;
}

.reconcile-detail__grid strong {
  color: #111827;
  font-size: 14px;
}

.reconcile-detail__section h3 {
  margin: 0 0 8px;
  color: #111827;
  font-size: 15px;
}

.reconcile-detail__section p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.reconcile-detail__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.reconcile-detail__actions-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.reconcile-history {
  margin-top: 4px;
}

.reconcile-history__card {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.reconcile-history__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #111827;
}

.reconcile-history__head span,
.reconcile-history__card small {
  color: #64748b;
}

.reconcile-history__card p {
  margin: 0;
  color: #334155;
}

.reconcile-history__attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  color: #64748b;
  font-size: 12px;
}

.receipt-print-area {
  padding: 24px;
  border: 1px solid #d8e4e1;
  border-radius: 8px;
  background: #fff;
  color: #111827;
}

.receipt-title {
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 2px solid #0f766e;
  text-align: center;
}

.receipt-title h2 {
  margin: 0;
  color: #0f766e;
  font-size: 24px;
  font-weight: 800;
}

.receipt-title p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.receipt-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-top: 1px solid #d8e4e1;
  border-left: 1px solid #d8e4e1;
}

.receipt-grid div {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  min-height: 44px;
  border-right: 1px solid #d8e4e1;
  border-bottom: 1px solid #d8e4e1;
}

.receipt-grid span,
.receipt-summary span {
  padding: 12px;
  background: #f6faf9;
  color: #526274;
  font-weight: 700;
}

.receipt-grid strong {
  padding: 12px;
  color: #111827;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.receipt-summary {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  min-height: 82px;
  border-right: 1px solid #d8e4e1;
  border-bottom: 1px solid #d8e4e1;
  border-left: 1px solid #d8e4e1;
}

.receipt-summary p {
  margin: 0;
  padding: 12px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.receipt-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  color: #334155;
  font-size: 14px;
}

@media print {
  body * {
    visibility: hidden;
  }

  .receipt-print-area,
  .receipt-print-area * {
    visibility: visible;
  }

  .receipt-print-area {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    padding: 18mm;
    border: 0;
    border-radius: 0;
    box-sizing: border-box;
  }
}
</style>
