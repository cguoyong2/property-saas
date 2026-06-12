package com.yongquan.propertysaas.importing.service;

import com.yongquan.propertysaas.base.domain.ImportResultView;
import com.yongquan.propertysaas.base.dto.HouseImportRequest;
import com.yongquan.propertysaas.base.dto.HouseRequest;
import com.yongquan.propertysaas.base.service.BaseArchiveService;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.fee.domain.BillGenerateResultView;
import com.yongquan.propertysaas.fee.dto.BillImportRequest;
import com.yongquan.propertysaas.fee.dto.BillManualRequest;
import com.yongquan.propertysaas.fee.service.FeeBillService;
import com.yongquan.propertysaas.file.domain.StoredFileResource;
import com.yongquan.propertysaas.file.service.FileObjectService;
import com.yongquan.propertysaas.importing.domain.ImportBatchView;
import com.yongquan.propertysaas.importing.domain.ImportErrorDetailView;
import com.yongquan.propertysaas.importing.dto.CsvImportRequest;
import com.yongquan.propertysaas.importing.repository.ImportBatchRepository;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class ImportBatchService {

    private final ImportBatchRepository repository;
    private final FileObjectService fileObjectService;
    private final BaseArchiveService baseArchiveService;
    private final FeeBillService feeBillService;
    private final OperationLogService operationLogService;

    public ImportBatchService(ImportBatchRepository repository, FileObjectService fileObjectService,
                              BaseArchiveService baseArchiveService, FeeBillService feeBillService,
                              OperationLogService operationLogService) {
        this.repository = repository;
        this.fileObjectService = fileObjectService;
        this.baseArchiveService = baseArchiveService;
        this.feeBillService = feeBillService;
        this.operationLogService = operationLogService;
    }

    public PageResult<ImportBatchView> page(Long projectId, String importType, String importStatus,
                                            long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findBatches(tenantId, scope, projectId, importType, importStatus, offset(pageNo, pageSize), pageSize),
                repository.countBatches(tenantId, scope, projectId, importType, importStatus),
                pageNo,
                pageSize);
    }

    public ImportBatchView get(Long batchId) {
        ImportBatchView batch = repository.getBatch(tenantId(), batchId);
        ensureBatchProjectAllowed(batch);
        return batch;
    }

    public PageResult<ImportErrorDetailView> errors(Long batchId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        get(batchId);
        return new PageResult<>(
                repository.findErrors(tenantId(), batchId, offset(pageNo, pageSize), pageSize),
                repository.countErrors(tenantId(), batchId),
                pageNo,
                pageSize);
    }

    public byte[] errorCsv(Long batchId) {
        get(batchId);
        StringBuilder csv = new StringBuilder("rowNo,fieldName,rawValue,errorCode,errorMessage\n");
        for (ImportErrorDetailView error : repository.findAllErrors(tenantId(), batchId)) {
            csv.append(error.rowNo()).append(',')
                    .append(escape(error.fieldName())).append(',')
                    .append(escape(error.rawValue())).append(',')
                    .append(escape(error.errorCode())).append(',')
                    .append(escape(error.errorMessage())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public ImportResultView importHouseCsv(CsvImportRequest request) {
        ensureProjectAllowed(request.projectId());
        List<Map<String, String>> rows = loadCsv(request, "import");
        List<HouseRequest> houses = rows.stream().map(this::houseRow).toList();
        ImportResultView result = baseArchiveService.importHouses(
                new HouseImportRequest(request.projectId(), request.sourceFileId(), houses));
        recordImport("HOUSE_CSV_IMPORT", request.projectId(), result.batchId(), request.sourceFileId(), rows.size(), result.importStatus());
        return result;
    }

    public BillGenerateResultView importHistoryBillCsv(CsvImportRequest request) {
        ensureProjectAllowed(request.projectId());
        List<Map<String, String>> rows = loadCsv(request, "import");
        List<BillManualRequest> bills = rows.stream().map(this::billRow).toList();
        BillGenerateResultView result = feeBillService.importBills(
                new BillImportRequest(request.projectId(), request.sourceFileId(), bills));
        recordImport("HISTORY_BILL_CSV_IMPORT", request.projectId(), result.batchId(), request.sourceFileId(), rows.size(), result.status());
        return result;
    }

    private List<Map<String, String>> loadCsv(CsvImportRequest request, String moduleCode) {
        StoredFileResource resource = fileObjectService.loadForBusiness(request.sourceFileId(), request.projectId(), moduleCode);
        String name = resource.metadata().originalName();
        if (name == null || !name.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("当前文件导入仅支持 CSV");
        }
        try {
            String content = StreamUtils.copyToString(resource.resource().getInputStream(), StandardCharsets.UTF_8);
            List<Map<String, String>> rows = CsvParser.parse(content);
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV 不包含有效数据行");
            }
            return rows;
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取导入文件失败", ex);
        }
    }

    private HouseRequest houseRow(Map<String, String> row) {
        return new HouseRequest(longValue(row, "projectId"), longValue(row, "buildingId"), optionalLong(row, "unitId"),
                required(row, "houseNo"), optionalInt(row, "floorNo"), optionalMoney(row, "buildingArea"),
                optionalMoney(row, "innerArea"), optional(row, "houseUsage"), optional(row, "houseStatus"),
                optional(row, "chargeObject"));
    }

    private BillManualRequest billRow(Map<String, String> row) {
        return new BillManualRequest(longValue(row, "projectId"), longValue(row, "itemId"),
                optionalLong(row, "standardId"), required(row, "objectType"), longValue(row, "objectId"),
                optionalLong(row, "memberId"), optionalLong(row, "houseId"), required(row, "billPeriod"),
                money(row, "receivableAmount"), optionalMoney(row, "discountAmount"), optionalDate(row, "dueDate"));
    }

    private void recordImport(String action, Long projectId, Long batchId, Long sourceFileId, int rows, String status) {
        operationLogService.record(new OperationLogWrite(tenantId(), projectId, "import", action,
                "import_batch", batchId, Map.of(), Map.of("sourceFileId", sourceFileId, "rows", rows, "status", status), null));
    }

    private void ensureBatchProjectAllowed(ImportBatchView batch) {
        if (batch.projectId() != null) {
            ensureProjectAllowed(batch.projectId());
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
        return repository.findAllowedProjectIds(tenantId, TenantContext.getUserId());
    }

    private String required(Map<String, String> row, String key) {
        String value = optional(row, key);
        if (value == null) {
            throw new IllegalArgumentException("CSV 缺少必填字段：" + key);
        }
        return value;
    }

    private String optional(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long longValue(Map<String, String> row, String key) {
        return Long.valueOf(required(row, key));
    }

    private Long optionalLong(Map<String, String> row, String key) {
        String value = optional(row, key);
        return value == null ? null : Long.valueOf(value);
    }

    private Integer optionalInt(Map<String, String> row, String key) {
        String value = optional(row, key);
        return value == null ? null : Integer.valueOf(value);
    }

    private BigDecimal money(Map<String, String> row, String key) {
        return new BigDecimal(required(row, key));
    }

    private BigDecimal optionalMoney(Map<String, String> row, String key) {
        String value = optional(row, key);
        return value == null ? null : new BigDecimal(value);
    }

    private LocalDate optionalDate(Map<String, String> row, String key) {
        String value = optional(row, key);
        return value == null ? null : LocalDate.parse(value);
    }

    private String escape(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("分页参数错误");
        }
    }

    private long offset(long pageNo, long pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }
}
