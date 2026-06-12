package com.yongquan.propertysaas.service.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.file.service.FileObjectService;
import com.yongquan.propertysaas.service.domain.NoticeRecipient;
import com.yongquan.propertysaas.service.domain.WorkOrderDetailView;
import com.yongquan.propertysaas.service.domain.WorkOrderView;
import com.yongquan.propertysaas.service.dto.WorkOrderActionRequest;
import com.yongquan.propertysaas.service.dto.WorkOrderCreateRequest;
import com.yongquan.propertysaas.service.dto.WorkOrderDispatchRequest;
import com.yongquan.propertysaas.service.dto.WorkOrderEvaluateRequest;
import com.yongquan.propertysaas.service.repository.WorkOrderRepository;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderService {

    private static final Set<String> ORDER_TYPES = Set.of("REPAIR", "COMPLAINT", "SUGGESTION", "HOUSEKEEPING", "RENOVATION");
    private static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Set<String> STATUSES = Set.of("SUBMITTED", "ACCEPTED", "DISPATCHED", "PROCESSING",
            "HANG_UP", "WAIT_CONFIRM", "COMPLETED", "EVALUATED", "CANCELLED", "REJECTED", "TRANSFERRED", "REWORK");

    private final WorkOrderRepository repository;
    private final OperationLogService operationLogService;
    private final FileObjectService fileObjectService;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public WorkOrderService(WorkOrderRepository repository, OperationLogService operationLogService,
                            FileObjectService fileObjectService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.fileObjectService = fileObjectService;
    }

    public PageResult<WorkOrderView> pageWorkOrders(Long projectId, String status, String orderType,
                                                    Long handlerUserId, Long memberId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateStatusIfPresent(status);
        validateOrderTypeIfPresent(orderType);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findWorkOrders(tenantId, scope, projectId, status, orderType, handlerUserId, memberId,
                        offset(pageNo, pageSize), pageSize),
                repository.countWorkOrders(tenantId, scope, projectId, status, orderType, handlerUserId, memberId),
                pageNo,
                pageSize);
    }

    public PageResult<WorkOrderView> pageComplaints(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateStatusIfPresent(status);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        List<String> types = List.of("COMPLAINT", "SUGGESTION");
        return new PageResult<>(
                repository.findWorkOrdersByTypes(tenantId, scope, projectId, status, types, offset(pageNo, pageSize), pageSize),
                repository.countWorkOrdersByTypes(tenantId, scope, projectId, status, types),
                pageNo,
                pageSize);
    }

    public WorkOrderDetailView getWorkOrder(Long workOrderId) {
        WorkOrderView workOrder = repository.getWorkOrder(tenantId(), workOrderId);
        ensureProjectAllowed(workOrder.projectId());
        return detail(workOrder);
    }

    @Transactional
    public Long createWorkOrder(WorkOrderCreateRequest request) {
        WorkOrderCreateRequest normalized = validateCreateRequest(request);
        Long tenantId = tenantId();
        Long workOrderId = newId();
        String priority = normalize(normalized.priority(), "NORMAL");
        LocalDateTime slaDeadline = LocalDateTime.now().plusHours(slaHours(priority));
        repository.insertWorkOrder(tenantId, workOrderId, orderNo(workOrderId), userId(), normalized, priority, slaDeadline);
        repository.insertEvent(newId(), tenantId, normalized.projectId(), workOrderId, null, "SUBMITTED",
                "CREATE", operatorType(), operatorId(normalized.memberId()), normalized.description(), normalized.imageFileIds());
        insertProjectMessage(tenantId, normalized.projectId(), "新工单待受理", "新工单：" + normalized.title());
        return workOrderId;
    }

    @Transactional
    public void accept(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "SUBMITTED", "ACCEPTED", "ACCEPT", request, userId(), null, null, false, false);
    }

    @Transactional
    public void reject(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "SUBMITTED", "REJECTED", "REJECT", request, null, null, null, false, false);
    }

    @Transactional
    public void cancel(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "SUBMITTED", "CANCELLED", "CANCEL", request, null, null, null, false, false);
    }

    @Transactional
    public void dispatch(Long workOrderId, WorkOrderDispatchRequest request) {
        WorkOrderView workOrder = getWorkOrder(workOrderId).workOrder();
        if (!repository.userExists(tenantId(), request.handlerUserId())) {
            throw new IllegalArgumentException("处理人不存在：" + request.handlerUserId());
        }
        if (!"ACCEPTED".equals(workOrder.status()) && !"DISPATCHED".equals(workOrder.status())) {
            throw new IllegalArgumentException("当前状态不可派单：" + workOrder.status());
        }
        String fromStatus = workOrder.status();
        String toStatus = "DISPATCHED";
        int updated = repository.updateStatus(tenantId(), workOrderId, fromStatus, toStatus, userId(),
                null, userId(), request.handlerUserId(), false, false);
        if (updated == 0) {
            throw new IllegalArgumentException("工单状态已变化，派单失败");
        }
        repository.insertEvent(newId(), tenantId(), workOrder.projectId(), workOrderId, fromStatus, toStatus,
                "DISPATCH", "USER", userId(), request.content(), null);
        repository.insertMessage(newId(), tenantId(), workOrder.projectId(),
                new NoticeRecipient("USER", request.handlerUserId(), null), "SITE", "WORKORDER_DISPATCH",
                "工单待处理", "工单已派给您：" + workOrder.title());
        operationLogService.record(new OperationLogWrite(tenantId(), workOrder.projectId(), "service", "WORKORDER_DISPATCH",
                "work_order", workOrderId, Map.of("status", fromStatus),
                Map.of("status", toStatus, "handlerUserId", request.handlerUserId()), request.content()));
    }

    @Transactional
    public void startProcessing(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "DISPATCHED", "PROCESSING", "START_PROCESS", request, null, null, userId(), false, false);
    }

    @Transactional
    public void hangUp(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "PROCESSING", "HANG_UP", "HANG_UP", request, null, null, null, false, false);
    }

    @Transactional
    public void resume(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "HANG_UP", "PROCESSING", "RESUME", request, null, null, null, false, false);
    }

    @Transactional
    public void submitResult(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "PROCESSING", "WAIT_CONFIRM", "SUBMIT_RESULT", request, null, null, null, false, false);
    }

    @Transactional
    public void rework(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "WAIT_CONFIRM", "REWORK", "REWORK", request, null, null, null, false, false);
        transition(workOrderId, "REWORK", "PROCESSING", "REWORK_PROCESS", request, null, null, null, false, false);
    }

    @Transactional
    public void revisit(Long workOrderId, WorkOrderActionRequest request) {
        transition(workOrderId, "WAIT_CONFIRM", "COMPLETED", "REVISIT_COMPLETE", request, null, null, null, true, false);
    }

    @Transactional
    public void evaluate(Long workOrderId, WorkOrderEvaluateRequest request) {
        WorkOrderView workOrder = getWorkOrder(workOrderId).workOrder();
        if (!"COMPLETED".equals(workOrder.status())) {
            throw new IllegalArgumentException("当前状态不可评价：" + workOrder.status());
        }
        if (workOrder.memberId() != null && !workOrder.memberId().equals(request.memberId())) {
            throw new AccessDeniedException("只能由工单提交会员评价");
        }
        if (repository.commentExists(tenantId(), workOrderId, request.memberId())) {
            throw new IllegalArgumentException("该工单已评价");
        }
        repository.insertComment(newId(), tenantId(), workOrder.projectId(), workOrderId,
                request.memberId(), request.score(), request.content());
        int updated = repository.updateStatus(tenantId(), workOrderId, "COMPLETED", "EVALUATED", userId(),
                null, null, null, false, true);
        if (updated == 0) {
            throw new IllegalArgumentException("工单状态已变化，评价失败");
        }
        repository.insertEvent(newId(), tenantId(), workOrder.projectId(), workOrderId, "COMPLETED", "EVALUATED",
                "EVALUATE", "MEMBER", request.memberId(), request.content(), null);
    }

    @Transactional
    public int markSlaOverdue(Integer limit) {
        int max = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        Long tenantId = tenantId();
        int count = 0;
        for (WorkOrderView workOrder : repository.findSlaOverdueCandidates(tenantId, projectScope(tenantId), max)) {
            repository.insertEvent(newId(), tenantId, workOrder.projectId(), workOrder.workOrderId(), workOrder.status(),
                    workOrder.status(), "SLA_OVERDUE", "SYSTEM", null, "工单已超过 SLA 截止时间", null);
            insertProjectMessage(tenantId, workOrder.projectId(), "工单 SLA 超时", "工单已超时：" + workOrder.orderNo());
            count++;
        }
        return count;
    }

    private void transition(Long workOrderId, String fromStatus, String toStatus, String action,
                            WorkOrderActionRequest request, Long acceptUserId, Long dispatchUserId,
                            Long handlerUserId, boolean completed, boolean evaluated) {
        WorkOrderView workOrder = getWorkOrder(workOrderId).workOrder();
        if (!fromStatus.equals(workOrder.status())) {
            throw new IllegalArgumentException("当前状态不可执行 " + action + "：" + workOrder.status());
        }
        validateActionImages(workOrder, request);
        int updated = repository.updateStatus(tenantId(), workOrderId, fromStatus, toStatus, userId(),
                acceptUserId, dispatchUserId, handlerUserId, completed, evaluated);
        if (updated == 0) {
            throw new IllegalArgumentException("工单状态已变化，操作失败");
        }
        repository.insertEvent(newId(), tenantId(), workOrder.projectId(), workOrderId, fromStatus, toStatus,
                action, "USER", userId(), request == null ? null : request.content(),
                request == null ? null : request.imageFileIds());
    }

    private WorkOrderCreateRequest validateCreateRequest(WorkOrderCreateRequest request) {
        validateOrderType(request.orderType());
        String priority = normalize(request.priority(), "NORMAL");
        if (!PRIORITIES.contains(priority)) {
            throw new IllegalArgumentException("非法工单优先级：" + priority);
        }
        ensureProjectAllowed(request.projectId());
        if (request.houseId() != null && !repository.houseExists(tenantId(), request.projectId(), request.houseId())) {
            throw new IllegalArgumentException("房屋不存在：" + request.houseId());
        }
        if (request.memberId() != null) {
            if (!repository.memberExists(tenantId(), request.memberId())) {
                throw new IllegalArgumentException("会员不存在：" + request.memberId());
            }
            if (request.houseId() != null && !repository.approvedBindingExists(tenantId(), request.projectId(),
                    request.memberId(), request.houseId())) {
                throw new IllegalArgumentException("会员未绑定该房屋");
            }
        }
        fileObjectService.validateImageFileIds(request.projectId(), "workorder", request.imageFileIds());
        return new WorkOrderCreateRequest(request.projectId(), request.memberId(), request.houseId(),
                normalize(request.orderType(), null), request.title(), request.description(), request.location(),
                request.imageFileIds(), priority);
    }

    private void insertProjectMessage(Long tenantId, Long projectId, String title, String content) {
        for (NoticeRecipient recipient : repository.findProjectUserRecipients(tenantId, projectId)) {
            repository.insertMessage(newId(), tenantId, projectId, recipient, "SITE", "WORKORDER_NOTIFY", title, content);
        }
    }

    private WorkOrderDetailView detail(WorkOrderView workOrder) {
        return new WorkOrderDetailView(workOrder,
                repository.findEvents(tenantId(), workOrder.workOrderId()),
                repository.findComments(tenantId(), workOrder.workOrderId()));
    }

    private void ensureProjectAllowed(Long projectId) {
        if (!repository.projectExists(tenantId(), projectId)) {
            throw new IllegalArgumentException("项目不存在：" + projectId);
        }
        List<Long> scope = projectScope(tenantId());
        if (scope != null && !scope.contains(projectId)) {
            throw new AccessDeniedException("无项目数据权限：" + projectId);
        }
    }

    private List<Long> projectScope(Long tenantId) {
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private void validateOrderTypeIfPresent(String orderType) {
        if (orderType != null && !orderType.isBlank()) {
            validateOrderType(orderType);
        }
    }

    private void validateOrderType(String orderType) {
        String normalized = normalize(orderType, null);
        if (!ORDER_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("非法工单类型：" + orderType);
        }
    }

    private void validateStatusIfPresent(String status) {
        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法工单状态：" + status);
        }
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("分页参数错误");
        }
    }

    private long slaHours(String priority) {
        return switch (priority) {
            case "URGENT" -> 4L;
            case "HIGH" -> 24L;
            case "LOW" -> 72L;
            default -> 48L;
        };
    }

    private String operatorType() {
        return "MEMBER".equals(TenantContext.getUserType()) ? "MEMBER" : "USER";
    }

    private Long operatorId(Long fallbackMemberId) {
        return TenantContext.getUserId() == null ? fallbackMemberId : TenantContext.getUserId();
    }

    private String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
    }

    private void validateActionImages(WorkOrderView workOrder, WorkOrderActionRequest request) {
        if (request != null) {
            fileObjectService.validateImageFileIds(workOrder.projectId(), "workorder", request.imageFileIds());
        }
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
    }

    private long offset(long pageNo, long pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }

    private String orderNo(Long workOrderId) {
        return "WO-" + tenantId() + "-" + workOrderId;
    }
}
