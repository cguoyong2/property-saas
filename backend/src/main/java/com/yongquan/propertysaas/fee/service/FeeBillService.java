package com.yongquan.propertysaas.fee.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.fee.domain.BillGenerateResultView;
import com.yongquan.propertysaas.fee.domain.BillObjectTarget;
import com.yongquan.propertysaas.fee.domain.BillStandardCandidate;
import com.yongquan.propertysaas.fee.domain.FeeBillView;
import com.yongquan.propertysaas.fee.domain.MemberPrepaymentBalance;
import com.yongquan.propertysaas.fee.dto.BillGenerateRequest;
import com.yongquan.propertysaas.fee.dto.BillImportRequest;
import com.yongquan.propertysaas.fee.dto.BillManualRequest;
import com.yongquan.propertysaas.fee.dto.BillRemindRequest;
import com.yongquan.propertysaas.fee.repository.FeeBillRepository;
import com.yongquan.propertysaas.service.service.AppMessageService;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final AppMessageService appMessageService;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public FeeBillService(FeeBillRepository repository, OperationLogService operationLogService,
                          AppMessageService appMessageService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.appMessageService = appMessageService;
    }

    @Transactional
    public PageResult<FeeBillView> pageBills(Long projectId, String status, String billPeriod, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateBillStatus(status);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        autoGenerateVisibleBills(tenantId, scope, projectId, billPeriod);
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
    public int generateDueBillsForTenant(Long tenantId, LocalDate billDate, Integer limit) {
        return generateMissingBills(tenantId, null, null, billDate, effectiveAutoGenerateLimit(limit));
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
            String templateCode = text(request.templateCode(), "BILL_DUE");
            String content = text(request.content(), "您有待缴物业账单：" + bill.billNo() + "，待缴金额 "
                    + bill.remainingAmount() + " 元，请及时缴费。");
            AppMessageService.RenderedMessage rendered = appMessageService.renderMessage(tenantId, channel, templateCode,
                    "账单催缴", content, billVariables(bill, Map.of(
                            "remainingAmount", money(bill.remainingAmount())
                    )));
            repository.insertMessage(tenantId, bill.projectId(), newId(), receiverType, receiverId, mobile,
                    channel, templateCode, rendered.title(), rendered.content());
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
        insertBill(tenantId, billId, billNo, userId(), request, sourceType);
    }

    private void insertBill(Long tenantId, Long billId, String billNo, Long createdBy, BillManualRequest request,
                            String sourceType) {
        BigDecimal receivable = money(request.receivableAmount());
        BigDecimal discount = money(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());
        BigDecimal remaining = receivable.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        repository.insertBill(tenantId, billId, billNo, createdBy, request, receivable, discount, remaining, sourceType);
        applyAvailablePrepayment(tenantId, billId, billNo, request, remaining, createdBy);
        FeeBillView createdBill = repository.getBill(tenantId, billId);
        notifyBillCreated(tenantId, createdBill, request);
    }

    private void applyAvailablePrepayment(Long tenantId, Long billId, String billNo, BillManualRequest request,
                                          BigDecimal remainingAmount, Long userId) {
        if (request.memberId() == null || remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal need = money(remainingAmount);
        for (MemberPrepaymentBalance prepayment : repository.findAvailablePrepayments(
                tenantId, request.projectId(), request.memberId())) {
            if (need.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = money(prepayment.remainingAmount());
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal amount = available.compareTo(need) > 0 ? need : available;
            if (repository.deductPrepayment(tenantId, prepayment.prepaymentId(), amount) == 0) {
                continue;
            }
            int billUpdated = repository.applyPrepaymentToBill(tenantId, billId, amount, userId);
            if (billUpdated == 0) {
                throw new IllegalStateException("预存款抵扣失败，账单状态已变化：" + billNo);
            }
            repository.insertPrepaymentUsage(tenantId, request.projectId(), newId(), prepayment.prepaymentId(),
                    request.memberId(), billId, billNo, amount, "AUTO_BILL_OFFSET",
                    "账单生成后自动抵扣预存款", userId);
            need = need.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private void validateBillDuplicate(Long tenantId, BillManualRequest request) {
        if (repository.duplicateBillExists(tenantId, request.projectId(), request.objectType(), request.objectId(),
                request.itemId(), request.billPeriod())) {
            throw new IllegalArgumentException("同对象同收费项目同账期账单已存在");
        }
    }

    private BigDecimal calculateGeneratedAmount(Long tenantId, BillStandardCandidate candidate) {
        BigDecimal unitPrice = money(candidate.unitPrice());
        BigDecimal months = BigDecimal.valueOf(cycleMonths(candidate.cycle()));
        if ("FORMULA".equals(candidate.chargeMethod())) {
            return money(calculateFormula(tenantId, candidate, unitPrice, months));
        }
        if ("AREA".equals(candidate.chargeMethod())) {
            if (!"HOUSE".equals(candidate.objectType())) {
                throw new IllegalArgumentException("面积计费仅支持房屋对象");
            }
            BigDecimal area = repository.findHouseBuildingArea(tenantId, candidate.projectId(), candidate.objectId());
            return money(area.multiply(unitPrice).multiply(months));
        }
        return money(unitPrice);
    }

    private BigDecimal calculateFormula(Long tenantId, BillStandardCandidate candidate, BigDecimal unitPrice,
                                        BigDecimal months) {
        if (candidate.formula() == null || candidate.formula().isBlank()) {
            throw new IllegalArgumentException("自定义公式不能为空");
        }
        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("unitPrice", unitPrice);
        variables.put("price", unitPrice);
        variables.put("months", months);
        variables.put("cycleMonths", months);
        variables.put("area", formulaArea(tenantId, candidate));
        return new FormulaParser(candidate.formula(), variables).parse();
    }

    private BigDecimal formulaArea(Long tenantId, BillStandardCandidate candidate) {
        if (!"HOUSE".equals(candidate.objectType())) {
            return BigDecimal.ZERO;
        }
        return money(repository.findHouseBuildingArea(tenantId, candidate.projectId(), candidate.objectId()));
    }

    private int cycleMonths(String cycle) {
        if ("QUARTER".equals(cycle)) {
            return 3;
        }
        if ("YEAR".equals(cycle)) {
            return 12;
        }
        return 1;
    }

    private void autoGenerateVisibleBills(Long tenantId, List<Long> allowedProjectIds, Long projectId, String billPeriod) {
        LocalDate billDate = billPeriod == null || billPeriod.isBlank() ? LocalDate.now() : parseBillDate(billPeriod);
        generateMissingBills(tenantId, allowedProjectIds, projectId, billDate, 1000);
    }

    private int generateMissingBills(Long tenantId, List<Long> allowedProjectIds, Long projectId, LocalDate billDate,
                                     int limit) {
        String billPeriod = billDate.toString().substring(0, 7);
        LocalDate dueDate = billDate.withDayOfMonth(billDate.lengthOfMonth());
        int created = 0;
        for (BillStandardCandidate candidate : repository.findAutoGenerateCandidates(
                tenantId, allowedProjectIds, projectId, billDate, limit)) {
            try {
                if (repository.duplicateBillExists(tenantId, candidate.projectId(), candidate.objectType(),
                        candidate.objectId(), candidate.itemId(), billPeriod)) {
                    continue;
                }
                BigDecimal amount = calculateGeneratedAmount(tenantId, candidate);
                BillObjectTarget target = repository.resolveObjectTarget(
                        tenantId, candidate.projectId(), candidate.objectType(), candidate.objectId());
                BillManualRequest row = new BillManualRequest(candidate.projectId(), candidate.itemId(),
                        candidate.standardId(), candidate.objectType(), candidate.objectId(), target.memberId(),
                        target.houseId(), billPeriod, amount, BigDecimal.ZERO, dueDate);
                Long billId = newId();
                insertBill(tenantId, billId, billNo("GEN", tenantId, row.projectId(), billId), null, row, "GENERATED");
                created++;
            } catch (RuntimeException ignored) {
                // A bad fee rule must not block the bill list; operators can correct the underlying standard/binding.
            }
        }
        return created;
    }

    private int effectiveAutoGenerateLimit(Integer limit) {
        int value = limit == null ? 1000 : limit;
        return Math.max(1, Math.min(value, 10000));
    }

    private static final class FormulaParser {

        private final String expression;
        private final Map<String, BigDecimal> variables;
        private int index;

        private FormulaParser(String expression, Map<String, BigDecimal> variables) {
            this.expression = expression;
            this.variables = variables;
        }

        private BigDecimal parse() {
            BigDecimal value = expression();
            skipSpaces();
            if (index != expression.length()) {
                throw new IllegalArgumentException("公式存在无法识别的内容：" + expression.substring(index));
            }
            return value;
        }

        private BigDecimal expression() {
            BigDecimal value = term();
            while (true) {
                skipSpaces();
                if (match('+')) {
                    value = value.add(term());
                } else if (match('-')) {
                    value = value.subtract(term());
                } else {
                    return value;
                }
            }
        }

        private BigDecimal term() {
            BigDecimal value = factor();
            while (true) {
                skipSpaces();
                if (match('*')) {
                    value = value.multiply(factor());
                } else if (match('/')) {
                    BigDecimal divisor = factor();
                    if (BigDecimal.ZERO.compareTo(divisor) == 0) {
                        throw new IllegalArgumentException("公式除数不能为0");
                    }
                    value = value.divide(divisor, MathContext.DECIMAL64);
                } else {
                    return value;
                }
            }
        }

        private BigDecimal factor() {
            skipSpaces();
            if (match('+')) {
                return factor();
            }
            if (match('-')) {
                return factor().negate();
            }
            if (match('(')) {
                BigDecimal value = expression();
                skipSpaces();
                if (!match(')')) {
                    throw new IllegalArgumentException("公式括号不完整");
                }
                return value;
            }
            if (index >= expression.length()) {
                throw new IllegalArgumentException("公式不完整");
            }
            char current = expression.charAt(index);
            if (Character.isDigit(current) || current == '.') {
                return number();
            }
            if (Character.isLetter(current) || current == '_') {
                return variable();
            }
            throw new IllegalArgumentException("公式存在非法字符：" + current);
        }

        private BigDecimal number() {
            int start = index;
            while (index < expression.length()) {
                char current = expression.charAt(index);
                if (!Character.isDigit(current) && current != '.') {
                    break;
                }
                index++;
            }
            try {
                return new BigDecimal(expression.substring(start, index));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("公式数字格式错误：" + expression.substring(start, index));
            }
        }

        private BigDecimal variable() {
            int start = index;
            while (index < expression.length()) {
                char current = expression.charAt(index);
                if (!Character.isLetterOrDigit(current) && current != '_') {
                    break;
                }
                index++;
            }
            String name = expression.substring(start, index);
            BigDecimal value = variables.get(name);
            if (value == null) {
                throw new IllegalArgumentException("公式变量不支持：" + name);
            }
            return value;
        }

        private boolean match(char expected) {
            if (index < expression.length() && expression.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipSpaces() {
            while (index < expression.length() && Character.isWhitespace(expression.charAt(index))) {
                index++;
            }
        }
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

    private void notifyBillCreated(Long tenantId, FeeBillView bill, BillManualRequest request) {
        if (request.memberId() == null) {
            return;
        }
        String content = "您有一笔新的物业账单。"
                + "\n账单号：" + bill.billNo()
                + "\n账期：" + request.billPeriod()
                + "\n应收：" + bill.receivableAmount() + " 元"
                + "\n优惠：" + bill.discountAmount() + " 元"
                + (bill.prepaymentAppliedAmount() == null || bill.prepaymentAppliedAmount().signum() == 0
                ? "" : "\n预存款抵扣：" + bill.prepaymentAppliedAmount() + " 元")
                + "\n待缴：" + bill.remainingAmount() + " 元"
                + (request.dueDate() == null ? "" : "\n到期日：" + request.dueDate());
        appMessageService.sendToMember(tenantId, request.projectId(), request.memberId(), "BILL_CREATED",
                "账单已生成", content, billVariables(bill, Map.of(
                        "dueDate", request.dueDate() == null ? "" : request.dueDate()
                )));
    }

    private Map<String, Object> billVariables(FeeBillView bill, Map<String, ?> extra) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("billNo", bill.billNo());
        variables.put("billPeriod", bill.billPeriod());
        variables.put("projectName", bill.projectName());
        variables.put("itemName", bill.itemName());
        variables.put("memberName", bill.memberName());
        variables.put("memberMobile", bill.memberMobile());
        variables.put("houseNo", bill.houseNo());
        variables.put("receivableAmount", money(bill.receivableAmount()));
        variables.put("discountAmount", money(bill.discountAmount()));
        variables.put("paidAmount", money(bill.paidAmount()));
        variables.put("refundAmount", money(bill.refundAmount()));
        variables.put("remainingAmount", money(bill.remainingAmount()));
        variables.put("prepaymentAppliedAmount", money(bill.prepaymentAppliedAmount()));
        if (bill.dueDate() != null) {
            variables.put("dueDate", bill.dueDate());
        }
        variables.put("detailSummary", text(bill.detailSummary(), ""));
        if (extra != null) {
            variables.putAll(extra);
        }
        return variables;
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
        return billNo(prefix, tenantId(), projectId, billId);
    }

    private String billNo(String prefix, Long tenantId, Long projectId, Long billId) {
        return prefix + "-" + tenantId + "-" + projectId + "-" + billId;
    }
}
