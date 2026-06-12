package com.yongquan.propertysaas.importing.controller;

import com.yongquan.propertysaas.base.domain.ImportResultView;
import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.fee.domain.BillGenerateResultView;
import com.yongquan.propertysaas.importing.domain.ImportBatchView;
import com.yongquan.propertysaas.importing.domain.ImportErrorDetailView;
import com.yongquan.propertysaas.importing.dto.CsvImportRequest;
import com.yongquan.propertysaas.importing.service.ImportBatchService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImportBatchController {

    private final ImportBatchService service;

    public ImportBatchController(ImportBatchService service) {
        this.service = service;
    }

    @GetMapping("/api/import/batches")
    @RequiresPermission("import:batch:list")
    public ApiResponse<PageResult<ImportBatchView>> page(@RequestParam(name = "projectId", required = false) Long projectId,
                                                         @RequestParam(name = "importType", required = false) String importType,
                                                         @RequestParam(name = "importStatus", required = false) String importStatus,
                                                         @RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                                         @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.page(projectId, importType, importStatus, pageNo, pageSize));
    }

    @GetMapping("/api/import/batches/{batchId}")
    @RequiresPermission("import:batch:view")
    public ApiResponse<ImportBatchView> get(@PathVariable Long batchId) {
        return ApiResponse.success(service.get(batchId));
    }

    @GetMapping("/api/import/batches/{batchId}/errors")
    @RequiresPermission("import:batch:errors")
    public ApiResponse<PageResult<ImportErrorDetailView>> errors(@PathVariable Long batchId,
                                                                 @RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                                                 @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.errors(batchId, pageNo, pageSize));
    }

    @GetMapping("/api/import/batches/{batchId}/errors.csv")
    @RequiresPermission("import:batch:errors")
    public ResponseEntity<byte[]> errorCsv(@PathVariable Long batchId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("import-errors-" + batchId + ".csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(service.errorCsv(batchId));
    }

    @PostMapping("/api/import/house-csv")
    @RequiresPermission("base:house:import")
    public ApiResponse<ImportResultView> importHouseCsv(@Valid @RequestBody CsvImportRequest request) {
        return ApiResponse.success(service.importHouseCsv(request));
    }

    @PostMapping("/api/import/history-bill-csv")
    @RequiresPermission("fee:bill:import")
    public ApiResponse<BillGenerateResultView> importHistoryBillCsv(@Valid @RequestBody CsvImportRequest request) {
        return ApiResponse.success(service.importHistoryBillCsv(request));
    }
}
