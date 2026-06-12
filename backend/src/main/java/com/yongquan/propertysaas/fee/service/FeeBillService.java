package com.yongquan.propertysaas.fee.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.fee.domain.BillGenerateResultView;
import com.yongquan.propertysaas.fee.domain.BillObjectTarget;
import com.yongquan.propertysaas.fee.domain.BillStandardCandidate;
import com.yongquan.propertysaas.fee.domain.FeeBillView;
import com.yongquan.propertysaas.fee.dto.BillGenerateRequest;
import com.yongquan.propertysaas.fee.dto.BillImportRequest;
import com.yongquan.propertysaas.fee.dto.BillManualRequest;
import com.yongquan.propertysaas.fee.dto.BillRemindRequest;
import com.yongquan.propertysaas.fee.repository.FeeBillRepository;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeBillService {

    private static final Set<String> OBJECT_TYPES = Set.of("HOUSE", "VEHICLE", "SPACE");
    private static final Set<String> BILL_STATUSES = Set.of(
            "UNPAID", "PAYING", "PARTIAL_PAID", "PAID", "OVERDUE", "VOID",
            "REFUNDING", "REFUNDED", "PARTIAL_REFUNDED");
    private static final Set<String> REMIND_STATUSES = Set.of("UNPAID", "OVERDUE", "PARTIAL_PAID");

    private final FeeBillRepository repository;
    private final OperationLogService operationLogService;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public FeeBillService(FeeBillRepository repository, OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
    }

    public PageResult<FeeBillView> pageBills(Long projectId, String status, String billPeriod, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateBillStatus(status);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findBills(tenantId, scope, projectId, status, billPeriod, offset(pageNo, pageSize), pageSize),
                repository.countBills(tenantId, scope, projectId, status, billPeriod),
                pageNo,
                pageSize);
    }

    public FeeBillView getBill(Long billId) {
        FeeBillView bill = repository.getBill(tenantId(), billId);
        ensureProjectAllowed(bill.projectId());
        return bill;
    }

    @Transactional
    public Long createBill(BillManualRequest request) {
        BillManualRequest normalized = validateAndNormalizeManual(request);
        Long billId = newId();
        String billNo = billNo("MANUAL", normalized.projectId(), billId);
        insertBill(tenantId(), billId, billNo, normalized, "MANUAL");
        return billId;
    }

    @Transactional
    public BillGenerateResultView generateBills(BillGenerateRequest request) {
        ensureProjectAllowed(request.projectId());
        validateItem(request.itemId());
        validateObjectTypeIfPresent(request.objectType());
        LocalDate billDate = parseBillDate(request.billPeriod());
        Long tenantId = tenantId();
        Long batchId = newId();
        String batchNo = "BILL-GEN-" + batchId;
        List<BillStandardCandidate> candidates = repository.findGenerateCandidates(
                tenantId, request.projectId(), request.itemId(), request.objectType(), request.objectIds(), billDate);
        int success = 0;
        int fail = 0;
        for (int i = 0; i < candidates.size(); i++) {
            BillStandardCandidate candidate = candidates.get(i);
            try {
                if ("FORMULA".equals(candidate.chargeMethod())) {
                    throw new IllegalArgumentException("公式计费标准暂不支持自动生成，请先人工确认金额");
                }
                BigDecimal amount = calculateGeneratedAmount(tenantId, candidate);
                BillObjectTarget target = repository.resolveObjectTarget(
                        tenantId, candidate.projectId(), candidate.objectType(), candidate.objectId());
                BillManualRequest row = new BillManualRequest(candidate.projectId(), candidate.itemId(),
                        candidate.standardId(), candidate.objectType(), candidate.objectId(), target.memberId(),
                        target.houseId(), request.billPeriod(), amount, BigDecimal.ZERO, request.dueDate());
                validateBillDuplicate(tenantId, row);
                Long billId = newId();
                insertBill(tenantId, billId, billNo("GEN", row.projectId(), billId), row, "GENERATED");
                success++;
            } catch (RuntimeException ex) {
                fail++;
                repository.insertImportError(tenantId, request.projectId(), batchId, newId(), i + 1, "objectId",
                        String.valueOf(candidate.objectId()), "BILL_GENERATE_ERROR", ex.getMessage());
            }
        }
        String status = batchStatus(success, fail);
        repository.insertImportBatch(tenantId, request.projectId(), batchId, batchNo, null,
                candidates.size(), success, fail, status, userId(), "BILL_GENERATE");
        return new BillGenerateResultView(batchId, batchNo, candidates.size(), success, fail, status);
    }

    @Transactional
    public BillGenerateResultView importBills(BillImportRequest request) {
        ensureProjectAllowed(request.projectId());
        Long tenantId = tenantId();
        Long batchId = newId();
        String batchNo = "BILL-IMPORT-" + batchId;
        int success = 0;
        int fail = 0;
        List<BillManualRequest> rows = request.rows();
        for (int i = 0; i < rows.size(); i++) {
            BillManualRequest row = rows.get(i);
            try {
                if (row == null) {
                    throw new IllegalArgumentException("导入行不能为空");
                }
                if (!request.projectId().equals(row.projectId())) {
                    throw new IllegalArgumentException("导入行项目ID必须等于批次项目ID");
                }
                BillManualRequest normalized = validateAndNormalizeManual(row);
                Long billId = newId();
                insertBill(tenantId, billId, billNo("IMP", normalized.projectId(), billId), normalized, "IMPORT");
                success++;
            } catch (RuntimeException ex) {
                fail++;
                repository.insertImportError(tenantId, request.projectId(), batchId, newId(), i + 1, "objectId",
                        row == null ? null : String.valueOf(row.objectId()), "BILL_IMPORT_ERROR", ex.getMessage());
            }
        }
        String status = batchStatus(success, fail);
        repository.insertImportBatch(tenantId, request.projectId(), batchId, batchNo, request.sourceFileId(),
                rows.size(), success, fail, status, userId(), "HISTORY_BILL");
        return new BillGenerateResultView(batchId, batchNo, rows.size(), success, fail, status);
    }

    @Transactional
    public void voidBill(Long billId, String reason) {
        FeeBillView bill = getBill(billId);
        if (!Set.of("UNPAID", "OVERDUE", "PARTIAL_PAID").contains(bill.status())) {
            throw new IllegalArgumentException("当前状态不可作废：" + bill.status());
        }
        int updated = repository.voidBill(tenantId(), billId, userId(), reason);
        if (updated == 0) {
            throw new IllegalArgumentException("账单状态已变化，作废失败");
        }
        operationLogService.record(new OperationLogWrite(tenantId(), bill.projectId(), "fee", "BILL_VOID",
                "fee_bill", billId, Map.of("status", bill.status(), "remainingAmount", bill.remainingAmount()),
                Map.of("status", "VOID"), reason));
    }

    @Transactional
    public int remindBills(BillRemindRequest request) {
        int count = 0;
        String channel = text(request.channel(), "SITE");
        Long tenantId = tenantId();
        for (Long billId : request.billIds()) {
            FeeBillView bill = getBill(billId);
            if (!REMIND_STATUSES.contains(bill.status())) {
                throw new IllegalArgumentException("账单不可催缴：" + bill.billNo());
            }
            BillObjectTarget target = repository.resolveObjectTarget(tenantId, bill.projectId(), bill.objectType(), bill.objectId());
            Long receiverId = bill.memberId() == null ? target.memberId() : bill.memberId();
            String mobile = target.mobile();
            if (receiverId == null && (mobile == null || mobile.isBlank())) {
                throw new IllegalArgumentException("账单缺少可催缴接收人：" + bill.billNo());
            }
            String receiverType = receiverId == null ? "MOBILE" : "MEMBER";
            String content = text(request.content(), "您有待缴物业账单：" + bill.billNo() + "，待缴金额 "
                    + bill.remainingAmount() + " 元，请及时缴费。");
            repository.insertMessage(tenantId, bill.projectId(), newId(), receiverType, receiverId, mobile,
                    channel, request.templateCode(), "账单催缴", content);
            count++;
        }
        return count;
    }

    private BillManualRequest validateAndNormalizeManual(BillManualRequest request) {
        ensureProjectAllowed(request.projectId());
        validateItem(request.itemId());
        validateObjectType(request.objectType());
        parseBillDate(request.billPeriod());
        if (!repository.standardExists(tenantId(), request.itemId(), request.standardId(), request.projectId())) {
            throw new IllegalArgumentException("收费标准不存在或不属于收费项目：" + request.standardId());
        }
        if (!repository.objectExists(tenantId(), request.projectId(), request.objectType(), request.objectId())) {
            throw new IllegalArgumentException("账单对象不存在：" + request.objectId());
        }
        BigDecimal receivable = money(request.receivableAmount());
        BigDecimal discount = money(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());
        if (receivable.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("账单金额不能小于0");
        }
        if (discount.compareTo(receivable) > 0) {
            throw new IllegalArgumentException("优惠金额不能大于应收金额");
        }
        BillObjectTarget target = repository.resolveObjectTarget(
                tenantId(), request.projectId(), request.objectType(), request.objectId());
        Long houseId = request.houseId() == null ? target.houseId() : request.houseId();
        Long memberId = request.memberId() == null ? target.memberId() : request.memberId();
        BillManualRequest normalized = new BillManualRequest(request.projectId(), request.itemId(), request.standardId(),
                request.objectType(), request.objectId(), memberId, houseId, request.billPeriod(), receivable,
                discount, request.dueDate());
        validateBillDuplicate(tenantId(), normalized);
        return normalized;
    }

    private void insertBill(Long tenantId, Long billId, String billNo, BillManualRequest request, String sourceType) {
        BigDecimal receivable = money(request.receivableAmount());
        BigDecimal discount = money(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());
        BigDecimal remaining = receivable.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        repository.insertBill(tenantId, billId, billNo, userId(), request, receivable, discount, remaining, sourceType);
    }

    private void validateBillDuplicate(Long tenantId, BillManualRequest request) {
        if (repository.duplicateBillExists(tenantId, request.projectId(), request.objectType(), request.objectId(),
                request.itemId(), request.billPeriod())) {
            throw new IllegalArgumentException("同对象同收费项目同账期账单已存在");
        }
    }

    private BigDecimal calculateGeneratedAmount(Long tenantId, BillStandardCandidate candidate) {
        BigDecimal unitPrice = money(candidate.unitPrice());
        if ("AREA".equals(candidate.chargeMethod())) {
            if (!"HOUSE".equals(candidate.objectType())) {
                throw new IllegalArgumentException("面积计费仅支持房屋对象");
            }
            BigDecimal area = repository.findHouseBuildingArea(tenantId, candidate.projectId(), candidate.objectId());
            return money(area.multiply(unitPrice));
        }
        return unitPrice;
    }

    private void validateItem(Long itemId) {
        if (!repository.itemExists(tenantId(), itemId)) {
            throw new IllegalArgumentException("收费项目不存在：" + itemId);
        }
    }

    private void validateObjectType(String objectType) {
        if (!OBJECT_TYPES.contains(objectType)) {
            throw new IllegalArgumentException("非法账单对象类型：" + objectType);
        }
    }

    private void validateObjectTypeIfPresent(String objectType) {
        if (objectType != null && !objectType.isBlank()) {
            validateObjectType(objectType);
        }
    }

    private void validateBillStatus(String status) {
        if (status != null && !status.isBlank() && !BILL_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法账单状态：" + status);
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

    private LocalDate parseBillDate(String billPeriod) {
        if (billPeriod == null || !billPeriod.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("账期格式必须为 yyyy-MM");
        }
        return LocalDate.parse(billPeriod + "-01");
    }

    private String batchStatus(int success, int fail) {
        if (fail == 0) {
            return "SUCCESS";
        }
        if (success == 0) {
            return "FAILED";
        }
        return "PARTIAL_SUCCESS";
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
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

    private String billNo(String prefix, Long projectId, Long billId) {
        return prefix + "-" + tenantId() + "-" + projectId + "-" + billId;
    }
}
