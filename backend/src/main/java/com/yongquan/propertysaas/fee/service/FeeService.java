package com.yongquan.propertysaas.fee.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.fee.domain.FeeItemView;
import com.yongquan.propertysaas.fee.domain.FeeStandardBindView;
import com.yongquan.propertysaas.fee.domain.FeeStandardView;
import com.yongquan.propertysaas.fee.dto.FeeItemRequest;
import com.yongquan.propertysaas.fee.dto.FeeStandardBindRequest;
import com.yongquan.propertysaas.fee.dto.FeeStandardRequest;
import com.yongquan.propertysaas.fee.repository.FeeRepository;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> ITEM_TYPES = Set.of("PERIODIC", "ONCE", "DEPOSIT", "PREPAID", "AGENCY");
    private static final Set<String> CHARGE_METHODS = Set.of("AREA", "HOUSE", "VEHICLE", "SPACE", "CONTRACT", "FIXED", "FORMULA");
    private static final Set<String> CYCLES = Set.of("MONTH", "QUARTER", "YEAR", "ONCE");
    private static final Set<String> OBJECT_TYPES = Set.of("HOUSE", "VEHICLE", "SPACE");

    private final FeeRepository repository;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public FeeService(FeeRepository repository) {
        this.repository = repository;
    }

    public PageResult<FeeItemView> pageItems(String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        Long tenantId = tenantId();
        return new PageResult<>(
                repository.findItems(tenantId, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countItems(tenantId, keyword, status),
                pageNo,
                pageSize);
    }

    public FeeItemView getItem(Long itemId) {
        return repository.getItem(tenantId(), itemId);
    }

    @Transactional
    public Long createItem(FeeItemRequest request) {
        validateItem(request);
        Long itemId = newId();
        repository.insertItem(tenantId(), itemId, userId(), autoItemCode(request.itemName(), itemId), request);
        return itemId;
    }

    @Transactional
    public void updateItem(Long itemId, FeeItemRequest request) {
        getItem(itemId);
        validateItem(request);
        repository.updateItem(tenantId(), itemId, userId(), request);
    }

    public PageResult<FeeStandardView> pageStandards(Long projectId, Long itemId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findStandards(tenantId, scope, projectId, itemId, status, offset(pageNo, pageSize), pageSize),
                repository.countStandards(tenantId, scope, projectId, itemId, status),
                pageNo,
                pageSize);
    }

    public FeeStandardView getStandard(Long standardId) {
        FeeStandardView standard = repository.getStandard(tenantId(), standardId);
        if (standard.projectId() != null) {
            ensureProjectAllowed(standard.projectId());
        }
        return standard;
    }

    @Transactional
    public Long createStandard(FeeStandardRequest request) {
        validateStandard(request);
        Long standardId = newId();
        repository.insertStandard(tenantId(), standardId, userId(), standardName(request), request);
        return standardId;
    }

    @Transactional
    public void updateStandard(Long standardId, FeeStandardRequest request) {
        getStandard(standardId);
        validateStandard(request);
        repository.updateStandard(tenantId(), standardId, userId(), standardName(request), request);
    }

    public PageResult<FeeStandardBindView> pageBinds(Long projectId, Long standardId, String objectType,
                                                     String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findBinds(tenantId, scope, projectId, standardId, objectType, status, offset(pageNo, pageSize), pageSize),
                repository.countBinds(tenantId, scope, projectId, standardId, objectType, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createBind(FeeStandardBindRequest request) {
        validateBind(request);
        Long bindId = newId();
        repository.insertBind(tenantId(), bindId, userId(), request);
        return bindId;
    }

    private void validateItem(FeeItemRequest request) {
        if (!ITEM_TYPES.contains(request.itemType())) {
            throw new IllegalArgumentException("非法收费项目类型：" + request.itemType());
        }
        validateStatus(request.status());
    }

    private String autoItemCode(String itemName, Long itemId) {
        String prefix = itemCodePrefix(itemName);
        return "%s_%06d".formatted(prefix, Math.abs(itemId % 1_000_000));
    }

    private String itemCodePrefix(String itemName) {
        String name = itemName == null ? "" : itemName.trim();
        if (name.contains("物业")) return "PROPERTY_FEE";
        if (name.contains("停车") || name.contains("车位")) return "PARKING_FEE";
        if (name.contains("水")) return "WATER_FEE";
        if (name.contains("电梯")) return "ELEVATOR_FEE";
        if (name.contains("电")) return "ELECTRICITY_FEE";
        if (name.contains("燃气") || name.contains("天然气")) return "GAS_FEE";
        if (name.contains("垃圾")) return "GARBAGE_FEE";
        if (name.contains("押金")) return "DEPOSIT_FEE";
        if (name.contains("租金") || name.contains("租赁")) return "RENT_FEE";
        String asciiPrefix = name.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return asciiPrefix.isBlank() ? "FEE_ITEM" : asciiPrefix;
    }

    private void validateStandard(FeeStandardRequest request) {
        if (!repository.itemExists(tenantId(), request.itemId())) {
            throw new IllegalArgumentException("收费项目不存在：" + request.itemId());
        }
        if (request.projectId() != null) {
            ensureProjectAllowed(request.projectId());
        }
        if (!CHARGE_METHODS.contains(request.chargeMethod())) {
            throw new IllegalArgumentException("非法计费方式：" + request.chargeMethod());
        }
        String cycle = request.cycle() == null || request.cycle().isBlank() ? "MONTH" : request.cycle();
        if (!CYCLES.contains(cycle)) {
            throw new IllegalArgumentException("非法计费周期：" + cycle);
        }
        if (request.unitPrice() != null && request.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("单价不能小于0");
        }
        if ("FORMULA".equals(request.chargeMethod()) && (request.formula() == null || request.formula().isBlank())) {
            throw new IllegalArgumentException("公式计费必须配置公式");
        }
        validateStatus(request.status());
    }

    private String standardName(FeeStandardRequest request) {
        String explicitName = request.standardName() == null ? "" : request.standardName().trim();
        if (!explicitName.isBlank()) {
            return explicitName;
        }
        return repository.getItem(tenantId(), request.itemId()).itemName();
    }

    private void validateBind(FeeStandardBindRequest request) {
        ensureProjectAllowed(request.projectId());
        FeeStandardView standard = getStandard(request.standardId());
        if (standard.projectId() != null && !standard.projectId().equals(request.projectId())) {
            throw new IllegalArgumentException("收费标准不属于绑定项目：" + request.standardId());
        }
        if (!OBJECT_TYPES.contains(request.objectType())) {
            throw new IllegalArgumentException("非法绑定对象类型：" + request.objectType());
        }
        if (!repository.bindObjectExists(tenantId(), request.projectId(), request.objectType(), request.objectId())) {
            throw new IllegalArgumentException("绑定对象不存在：" + request.objectId());
        }
        validateStatus(request.status());
    }

    private void validateStatus(String status) {
        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法状态：" + status);
        }
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

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("分页参数错误");
        }
    }

    private long offset(long pageNo, long pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
