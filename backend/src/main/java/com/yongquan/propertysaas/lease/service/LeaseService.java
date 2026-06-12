package com.yongquan.propertysaas.lease.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.lease.domain.LeaseContractDetailView;
import com.yongquan.propertysaas.lease.domain.LeaseContractView;
import com.yongquan.propertysaas.lease.domain.LeaseCustomerView;
import com.yongquan.propertysaas.lease.domain.LeaseFollowRecordView;
import com.yongquan.propertysaas.lease.domain.LeaseResourceView;
import com.yongquan.propertysaas.lease.dto.LeaseContractRequest;
import com.yongquan.propertysaas.lease.dto.LeaseContractStatusRequest;
import com.yongquan.propertysaas.lease.dto.LeaseCustomerRequest;
import com.yongquan.propertysaas.lease.dto.LeaseFollowRequest;
import com.yongquan.propertysaas.lease.dto.LeaseResourceRequest;
import com.yongquan.propertysaas.lease.repository.LeaseRepository;
import com.yongquan.propertysaas.service.domain.NoticeRecipient;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaseService {

    private static final Set<String> RESOURCE_TYPES = Set.of("HOUSE", "SHOP", "SPACE", "AD", "WAREHOUSE");
    private static final Set<String> RESOURCE_STATUSES = Set.of("VACANT", "RESERVED", "LEASED", "DISABLED");
    private static final Set<String> CUSTOMER_STATUSES = Set.of("POTENTIAL", "FOLLOWING", "INTENTIONAL", "SIGNED", "LOST");
    private static final Set<String> CONTRACT_STATUSES = Set.of("DRAFT", "WAIT_EFFECTIVE", "ACTIVE", "WILL_EXPIRE", "EXPIRED", "TERMINATED", "VOID");
    private static final Set<String> PAYMENT_CYCLES = Set.of("MONTH", "QUARTER", "YEAR");

    private final LeaseRepository repository;
    private final OperationLogService operationLogService;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public LeaseService(LeaseRepository repository, OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
    }

    public PageResult<LeaseResourceView> pageResources(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, RESOURCE_STATUSES, "资源状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findResources(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countResources(tenantId, scope, projectId, status), pageNo, pageSize);
    }

    @Transactional
    public Long createResource(LeaseResourceRequest request) {
        ensureProjectAllowed(request.projectId());
        String resourceType = normalize(request.resourceType(), null);
        validate(resourceType, RESOURCE_TYPES, "资源类型");
        String status = normalize(request.status(), "VACANT");
        validate(status, RESOURCE_STATUSES, "资源状态");
        Long id = newId();
        LeaseResourceRequest normalized = new LeaseResourceRequest(request.projectId(), resourceType, request.resourceName(),
                request.refObjectId(), money(request.area()), status);
        repository.insertResource(tenantId(), id, userId(), normalized, status);
        return id;
    }

    public PageResult<LeaseCustomerView> pageCustomers(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, CUSTOMER_STATUSES, "客户状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findCustomers(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countCustomers(tenantId, scope, projectId, status), pageNo, pageSize);
    }

    @Transactional
    public Long createCustomer(LeaseCustomerRequest request) {
        ensureProjectAllowed(request.projectId());
        if (request.ownerUserId() != null) {
            ensureUserExists(request.ownerUserId());
        }
        String status = normalize(request.status(), "POTENTIAL");
        validate(status, CUSTOMER_STATUSES, "客户状态");
        Long id = newId();
        repository.insertCustomer(tenantId(), id, userId(), request, status);
        return id;
    }

    @Transactional
    public Long addFollow(Long customerId, LeaseFollowRequest request) {
        LeaseCustomerView customer = repository.getCustomer(tenantId(), customerId);
        ensureProjectAllowed(customer.projectId());
        Long id = newId();
        repository.insertFollow(tenantId(), customer.projectId(), id, userId(), customerId, request);
        return id;
    }

    public List<LeaseFollowRecordView> customerFollows(Long customerId) {
        LeaseCustomerView customer = repository.getCustomer(tenantId(), customerId);
        ensureProjectAllowed(customer.projectId());
        return repository.findFollows(tenantId(), customerId);
    }

    public PageResult<LeaseContractView> pageContracts(Long projectId, String status, Long customerId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, CONTRACT_STATUSES, "合同状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findContracts(tenantId, scope, projectId, status, customerId,
                offset(pageNo, pageSize), pageSize), repository.countContracts(tenantId, scope, projectId, status, customerId),
                pageNo, pageSize);
    }

    public LeaseContractDetailView getContract(Long contractId) {
        LeaseContractView contract = repository.getContract(tenantId(), contractId);
        ensureProjectAllowed(contract.projectId());
        return new LeaseContractDetailView(contract, repository.findContractBills(tenantId(), contractId));
    }

    @Transactional
    public Long createContract(LeaseContractRequest request) {
        validateContractRequest(request);
        Long id = newId();
        String status = Boolean.TRUE.equals(request.activeNow()) ? "ACTIVE" : "DRAFT";
        LeaseContractRequest normalized = normalizeContract(request);
        repository.insertContract(tenantId(), id, contractNo(id), userId(), normalized, status);
        if ("ACTIVE".equals(status)) {
            activateContract(id);
        }
        return id;
    }

    @Transactional
    public void activateContract(Long contractId) {
        LeaseContractView contract = repository.getContract(tenantId(), contractId);
        ensureProjectAllowed(contract.projectId());
        if ("ACTIVE".equals(contract.status())) {
            generateRentBills(contract);
            repository.updateResourceStatus(tenantId(), contract.resourceId(), "LEASED", userId());
            operationLogService.record(new OperationLogWrite(tenantId(), contract.projectId(), "lease", "CONTRACT_ACTIVATE_REPLAY",
                    "lease_contract", contractId, Map.of("status", contract.status()),
                    Map.of("status", "ACTIVE", "resourceId", contract.resourceId()), null));
            return;
        }
        if (!Set.of("DRAFT", "WAIT_EFFECTIVE").contains(contract.status())) {
            throw new IllegalArgumentException("当前状态不可生效：" + contract.status());
        }
        int updated = repository.updateContractStatus(tenantId(), contractId, contract.status(), "ACTIVE", userId());
        if (updated == 0) {
            throw new IllegalArgumentException("合同状态已变化，生效失败");
        }
        LeaseContractView active = repository.getContract(tenantId(), contractId);
        repository.updateResourceStatus(tenantId(), active.resourceId(), "LEASED", userId());
        generateRentBills(active);
        operationLogService.record(new OperationLogWrite(tenantId(), contract.projectId(), "lease", "CONTRACT_ACTIVATE",
                "lease_contract", contractId, Map.of("status", contract.status()),
                Map.of("status", "ACTIVE", "resourceId", active.resourceId()), null));
    }

    @Transactional
    public void terminateContract(Long contractId, LeaseContractStatusRequest request) {
        LeaseContractView contract = repository.getContract(tenantId(), contractId);
        ensureProjectAllowed(contract.projectId());
        if (!"ACTIVE".equals(contract.status()) && !"WILL_EXPIRE".equals(contract.status())) {
            throw new IllegalArgumentException("当前状态不可终止：" + contract.status());
        }
        int updated = repository.updateContractStatus(tenantId(), contractId, contract.status(), "TERMINATED", userId());
        if (updated == 0) {
            throw new IllegalArgumentException("合同状态已变化，终止失败");
        }
        repository.updateResourceStatus(tenantId(), contract.resourceId(), "VACANT", userId());
        operationLogService.record(new OperationLogWrite(tenantId(), contract.projectId(), "lease", "CONTRACT_TERMINATE",
                "lease_contract", contractId, Map.of("status", contract.status(), "resourceId", contract.resourceId()),
                Map.of("status", "TERMINATED", "resourceStatus", "VACANT"),
                request == null ? null : request.reason()));
    }

    @Transactional
    public int remindExpiring(Integer days) {
        int dueDays = days == null ? 30 : Math.max(1, Math.min(days, 365));
        PageResult<LeaseContractView> activeContracts = pageContracts(null, "ACTIVE", null, 1, 200);
        int count = 0;
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(dueDays);
        for (LeaseContractView contract : activeContracts.records()) {
            if (!contract.endDate().isBefore(today) && !contract.endDate().isAfter(threshold)) {
                for (NoticeRecipient recipient : repository.findProjectUserRecipients(tenantId(), contract.projectId())) {
                    repository.insertMessage(newId(), tenantId(), contract.projectId(), recipient, "SITE",
                            "LEASE_CONTRACT_EXPIRE", "租赁合同到期提醒",
                            "合同 " + contract.contractNo() + " 将于 " + contract.endDate() + " 到期");
                    count++;
                }
            }
        }
        return count;
    }

    public PageResult<LeaseContractView> appContracts(Long projectId, String mobile, long pageNo, long pageSize) {
        ensureProjectAllowed(projectId);
        PageResult<LeaseContractView> page = pageContracts(projectId, null, null, pageNo, pageSize);
        if (mobile == null || mobile.isBlank()) {
            return page;
        }
        List<LeaseContractView> filtered = page.records().stream()
                .filter(contract -> mobile.equals(contract.lesseeMobile()))
                .toList();
        return new PageResult<>(filtered, filtered.size(), pageNo, pageSize);
    }

    private void validateContractRequest(LeaseContractRequest request) {
        ensureProjectAllowed(request.projectId());
        LeaseResourceView resource = repository.getResource(tenantId(), request.resourceId());
        if (!resource.projectId().equals(request.projectId())) {
            throw new IllegalArgumentException("可租资源不属于当前项目");
        }
        if (!Set.of("VACANT", "RESERVED").contains(resource.status())) {
            throw new IllegalArgumentException("资源当前不可出租：" + resource.status());
        }
        if (request.customerId() != null) {
            LeaseCustomerView customer = repository.getCustomer(tenantId(), request.customerId());
            if (!customer.projectId().equals(request.projectId())) {
                throw new IllegalArgumentException("意向客户不属于当前项目");
            }
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("合同结束日期不能早于开始日期");
        }
        String paymentCycle = normalize(request.paymentCycle(), "MONTH");
        validate(paymentCycle, PAYMENT_CYCLES, "付款周期");
        if (money(request.rentAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("租金必须大于0");
        }
    }

    private LeaseContractRequest normalizeContract(LeaseContractRequest request) {
        return new LeaseContractRequest(request.projectId(), request.customerId(), request.resourceId(), request.lesseeName(),
                request.lesseeMobile(), request.startDate(), request.endDate(), money(request.rentAmount()),
                money(request.depositAmount()), normalize(request.paymentCycle(), "MONTH"),
                request.freeRentDays() == null ? 0 : request.freeRentDays(), request.attachmentFileIds(), request.activeNow());
    }

    private void generateRentBills(LeaseContractView contract) {
        Long itemId = repository.findOrCreateRentItem(tenantId(), userId(), newId());
        LocalDate billDate = contract.startDate().plusDays(contract.freeRentDays());
        if (billDate.isAfter(contract.endDate())) {
            return;
        }
        while (!billDate.isAfter(contract.endDate())) {
            String period = billDate.toString().substring(0, 7);
            if (!repository.billExists(tenantId(), contract.contractId(), period)) {
                Long billId = newId();
                repository.insertRentBill(tenantId(), billId, "LEASE-BILL-" + tenantId() + "-" + billId,
                        userId(), contract.projectId(), itemId, contract.contractId(), period, contract.rentAmount(),
                        billDate.withDayOfMonth(Math.min(billDate.lengthOfMonth(), contract.startDate().getDayOfMonth())));
            }
            billDate = nextBillDate(billDate, contract.paymentCycle());
        }
    }

    private LocalDate nextBillDate(LocalDate value, String cycle) {
        return switch (cycle) {
            case "YEAR" -> value.plusYears(1);
            case "QUARTER" -> value.plusMonths(3);
            default -> value.plusMonths(1);
        };
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

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
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

    private String contractNo(Long contractId) {
        return "LEASE-" + tenantId() + "-" + contractId;
    }
}
