package com.yongquan.propertysaas.patrol.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.file.service.FileObjectService;
import com.yongquan.propertysaas.patrol.domain.AssetEquipmentView;
import com.yongquan.propertysaas.patrol.domain.PatrolPlanView;
import com.yongquan.propertysaas.patrol.domain.PatrolPointView;
import com.yongquan.propertysaas.patrol.domain.PatrolTaskDetailView;
import com.yongquan.propertysaas.patrol.domain.PatrolTaskItemView;
import com.yongquan.propertysaas.patrol.domain.PatrolTaskView;
import com.yongquan.propertysaas.patrol.dto.AssetEquipmentRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolPlanRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolPointRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolTaskCreateRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolTaskSubmitItem;
import com.yongquan.propertysaas.patrol.dto.PatrolTaskSubmitRequest;
import com.yongquan.propertysaas.patrol.dto.RectificationRequest;
import com.yongquan.propertysaas.patrol.repository.PatrolRepository;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatrolService {

    private static final Set<String> EQUIPMENT_STATUSES = Set.of("NORMAL", "DISABLED", "REPAIRING", "SCRAPPED");
    private static final Set<String> POINT_STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> PLAN_STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> CYCLE_TYPES = Set.of("DAY", "WEEK", "MONTH", "CUSTOM");
    private static final Set<String> TASK_STATUSES = Set.of("PENDING", "IN_PROGRESS", "COMPLETED", "MISSED",
            "EXCEPTION", "RECTIFYING", "RECTIFIED");
    private static final Set<String> ITEM_RESULTS = Set.of("NORMAL", "EXCEPTION", "MISSED");

    private final PatrolRepository repository;
    private final FileObjectService fileObjectService;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public PatrolService(PatrolRepository repository, FileObjectService fileObjectService) {
        this.repository = repository;
        this.fileObjectService = fileObjectService;
    }

    public PageResult<AssetEquipmentView> pageEquipments(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, EQUIPMENT_STATUSES, "资产状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findEquipments(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countEquipments(tenantId, scope, projectId, status), pageNo, pageSize);
    }

    @Transactional
    public Long createEquipment(AssetEquipmentRequest request) {
        ensureProjectAllowed(request.projectId());
        String status = normalize(request.status(), "NORMAL");
        validate(status, EQUIPMENT_STATUSES, "资产状态");
        if (request.responsibleUserId() != null) {
            ensureUserExists(request.responsibleUserId());
        }
        Long id = newId();
        repository.insertEquipment(tenantId(), id, userId(), request, status);
        return id;
    }

    public PageResult<PatrolPointView> pagePoints(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, POINT_STATUSES, "点位状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findPoints(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countPoints(tenantId, scope, projectId, status), pageNo, pageSize);
    }

    @Transactional
    public Long createPoint(PatrolPointRequest request) {
        ensureProjectAllowed(request.projectId());
        String status = normalize(request.status(), "ACTIVE");
        validate(status, POINT_STATUSES, "点位状态");
        if (request.equipmentId() != null && !repository.equipmentExists(tenantId(), request.projectId(), request.equipmentId())) {
            throw new IllegalArgumentException("资产不存在：" + request.equipmentId());
        }
        Long id = newId();
        repository.insertPoint(tenantId(), id, userId(), request, status);
        return id;
    }

    public PageResult<PatrolPlanView> pagePlans(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, PLAN_STATUSES, "计划状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findPlans(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countPlans(tenantId, scope, projectId, status), pageNo, pageSize);
    }

    @Transactional
    public Long createPlan(PatrolPlanRequest request) {
        ensureProjectAllowed(request.projectId());
        String status = normalize(request.status(), "ACTIVE");
        validate(status, PLAN_STATUSES, "计划状态");
        validate(normalize(request.cycleType(), null), CYCLE_TYPES, "计划周期");
        if (request.executorUserId() != null) {
            ensureUserExists(request.executorUserId());
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("计划结束日期不能早于开始日期");
        }
        Long id = newId();
        repository.insertPlan(tenantId(), id, userId(), request, status);
        return id;
    }

    public PageResult<PatrolTaskView> pageTasks(Long projectId, String status, Long executorUserId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, TASK_STATUSES, "巡检任务状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findTasks(tenantId, scope, projectId, status, executorUserId,
                offset(pageNo, pageSize), pageSize), repository.countTasks(tenantId, scope, projectId, status, executorUserId),
                pageNo, pageSize);
    }

    public PatrolTaskDetailView getTask(Long taskId) {
        PatrolTaskView task = repository.getTask(tenantId(), taskId);
        ensureProjectAllowed(task.projectId());
        return detail(task);
    }

    @Transactional
    public Long createTask(PatrolTaskCreateRequest request) {
        ensureProjectAllowed(request.projectId());
        if (request.plannedEndAt().isBefore(request.plannedStartAt())) {
            throw new IllegalArgumentException("计划结束时间不能早于开始时间");
        }
        Long executorUserId = request.executorUserId();
        if (request.planId() != null) {
            PatrolPlanView plan = repository.getPlan(tenantId(), request.planId());
            if (!plan.projectId().equals(request.projectId())) {
                throw new IllegalArgumentException("巡检计划不属于当前项目");
            }
            executorUserId = executorUserId == null ? plan.executorUserId() : executorUserId;
        }
        if (executorUserId != null) {
            ensureUserExists(executorUserId);
        }
        for (Long pointId : request.pointIds()) {
            if (!repository.pointExists(tenantId(), request.projectId(), pointId)) {
                throw new IllegalArgumentException("巡检点位不存在或未启用：" + pointId);
            }
        }
        Long taskId = newId();
        repository.insertTask(tenantId(), taskId, userId(), request.projectId(), request.planId(), taskNo(taskId),
                request.taskName(), executorUserId, request.plannedStartAt(), request.plannedEndAt());
        for (Long pointId : request.pointIds()) {
            repository.insertTaskItem(tenantId(), newId(), request.projectId(), taskId, pointId);
        }
        return taskId;
    }

    @Transactional
    public PatrolTaskDetailView submitTask(Long taskId, PatrolTaskSubmitRequest request) {
        PatrolTaskView task = repository.getTask(tenantId(), taskId);
        ensureProjectAllowed(task.projectId());
        if (!Set.of("PENDING", "IN_PROGRESS").contains(task.status())) {
            throw new IllegalArgumentException("当前状态不可提交巡检：" + task.status());
        }
        if ("PENDING".equals(task.status())) {
            repository.updateTaskStart(tenantId(), taskId);
        }
        for (PatrolTaskSubmitItem item : request.items()) {
            if (!repository.taskItemExists(tenantId(), taskId, item.itemId())) {
                throw new IllegalArgumentException("巡检任务明细不存在：" + item.itemId());
            }
            String result = normalize(item.result(), null);
            validate(result, ITEM_RESULTS, "巡检结果");
            String itemStatus = switch (result) {
                case "NORMAL" -> "COMPLETED";
                case "EXCEPTION" -> "EXCEPTION";
                case "MISSED" -> "MISSED";
                default -> throw new IllegalArgumentException("非法巡检结果：" + result);
            };
            fileObjectService.validateImageFileIds(task.projectId(), "patrol", item.imageFileIds());
            repository.updateTaskItem(tenantId(), item.itemId(), result, itemStatus, item.content(), item.imageFileIds());
        }
        refreshTaskStatus(taskId);
        return getTask(taskId);
    }

    @Transactional
    public void startRectification(Long taskId, Long itemId, RectificationRequest request) {
        PatrolTaskView task = repository.getTask(tenantId(), taskId);
        ensureProjectAllowed(task.projectId());
        if (!repository.taskItemExists(tenantId(), taskId, itemId)) {
            throw new IllegalArgumentException("巡检任务明细不存在：" + itemId);
        }
        fileObjectService.validateImageFileIds(task.projectId(), "patrol", request.imageFileIds());
        repository.updateTaskItemStatus(tenantId(), itemId, "RECTIFYING", request.content(), request.imageFileIds());
        repository.updateTaskStatus(tenantId(), taskId, "RECTIFYING", false);
    }

    @Transactional
    public PatrolTaskDetailView completeRectification(Long taskId, Long itemId, RectificationRequest request) {
        PatrolTaskView task = repository.getTask(tenantId(), taskId);
        ensureProjectAllowed(task.projectId());
        if (!repository.taskItemExists(tenantId(), taskId, itemId)) {
            throw new IllegalArgumentException("巡检任务明细不存在：" + itemId);
        }
        fileObjectService.validateImageFileIds(task.projectId(), "patrol", request.imageFileIds());
        repository.updateTaskItemStatus(tenantId(), itemId, "RECTIFIED", request.content(), request.imageFileIds());
        refreshTaskStatus(taskId);
        return getTask(taskId);
    }

    @Transactional
    public int markMissed(Integer limit) {
        int max = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        Long tenantId = tenantId();
        int count = 0;
        for (PatrolTaskView task : repository.findMissedCandidates(tenantId, projectScope(tenantId), max)) {
            repository.markTaskItemsMissed(tenantId, task.taskId());
            repository.updateTaskStatus(tenantId, task.taskId(), "MISSED", true);
            count++;
        }
        return count;
    }

    private void refreshTaskStatus(Long taskId) {
        List<PatrolTaskItemView> items = repository.findTaskItems(tenantId(), taskId);
        boolean hasPending = items.stream().anyMatch(item -> "PENDING".equals(item.status()));
        boolean hasException = items.stream().anyMatch(item -> "EXCEPTION".equals(item.status()));
        boolean hasRectifying = items.stream().anyMatch(item -> "RECTIFYING".equals(item.status()));
        boolean hasMissed = items.stream().anyMatch(item -> "MISSED".equals(item.status()));
        if (hasException) {
            repository.updateTaskStatus(tenantId(), taskId, "EXCEPTION", false);
        } else if (hasRectifying) {
            repository.updateTaskStatus(tenantId(), taskId, "RECTIFYING", false);
        } else if (hasPending) {
            repository.updateTaskStatus(tenantId(), taskId, "IN_PROGRESS", false);
        } else if (hasMissed) {
            repository.updateTaskStatus(tenantId(), taskId, "MISSED", true);
        } else if (items.stream().anyMatch(item -> "RECTIFIED".equals(item.status()))) {
            repository.updateTaskStatus(tenantId(), taskId, "RECTIFIED", true);
        } else {
            repository.updateTaskStatus(tenantId(), taskId, "COMPLETED", true);
        }
    }

    private PatrolTaskDetailView detail(PatrolTaskView task) {
        return new PatrolTaskDetailView(task, repository.findTaskItems(tenantId(), task.taskId()));
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

    private void ensureUserExists(Long userId) {
        if (!repository.userExists(tenantId(), userId)) {
            throw new IllegalArgumentException("用户不存在：" + userId);
        }
    }

    private List<Long> projectScope(Long tenantId) {
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private void validateIfPresent(String value, Set<String> allowed, String label) {
        if (value != null && !value.isBlank()) {
            validate(value, allowed, label);
        }
    }

    private void validate(String value, Set<String> allowed, String label) {
        if (value == null || !allowed.contains(value)) {
            throw new IllegalArgumentException("非法" + label + "：" + value);
        }
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("分页参数错误");
        }
    }

    private String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
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

    private String taskNo(Long taskId) {
        return "PATROL-" + tenantId() + "-" + taskId;
    }
}
