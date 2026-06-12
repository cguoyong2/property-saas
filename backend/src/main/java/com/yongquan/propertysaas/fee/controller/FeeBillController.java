package com.yongquan.propertysaas.fee.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.fee.domain.BillGenerateResultView;
import com.yongquan.propertysaas.fee.domain.FeeBillView;
import com.yongquan.propertysaas.fee.dto.BillGenerateRequest;
import com.yongquan.propertysaas.fee.dto.BillImportRequest;
import com.yongquan.propertysaas.fee.dto.BillManualRequest;
import com.yongquan.propertysaas.fee.dto.BillRemindRequest;
import com.yongquan.propertysaas.fee.dto.ReasonRequest;
import com.yongquan.propertysaas.fee.service.FeeBillService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeeBillController {

    private final FeeBillService service;

    public FeeBillController(FeeBillService service) {
        this.service = service;
    }

    @GetMapping("/api/fee/bills")
    @RequiresPermission("fee:bill:list")
    public ApiResponse<PageResult<FeeBillView>> pageBills(@RequestParam(required = false) Long projectId,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String billPeriod,
                                                          @RequestParam(defaultValue = "1") long pageNo,
                                                          @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageBills(projectId, status, billPeriod, pageNo, pageSize));
    }

    @PostMapping("/api/fee/bills")
    @RequiresPermission("fee:bill:create")
    public ApiResponse<Map<String, Long>> createBill(@Valid @RequestBody BillManualRequest request) {
        return ApiResponse.success(Map.of("billId", service.createBill(request)));
    }

    @PostMapping("/api/fee/bills/generate")
    @RequiresPermission("fee:bill:generate")
    public ApiResponse<BillGenerateResultView> generateBills(@Valid @RequestBody BillGenerateRequest request) {
        return ApiResponse.success(service.generateBills(request));
    }

    @PostMapping("/api/fee/bills/import")
    @RequiresPermission("fee:bill:import")
    public ApiResponse<BillGenerateResultView> importBills(@Valid @RequestBody BillImportRequest request) {
        return ApiResponse.success(service.importBills(request));
    }

    @PostMapping("/api/fee/bills/remind")
    @RequiresPermission("fee:bill:remind")
    public ApiResponse<Map<String, Integer>> remindBills(@Valid @RequestBody BillRemindRequest request) {
        return ApiResponse.success(Map.of("messageCount", service.remindBills(request)));
    }

    @GetMapping("/api/fee/bills/{billId}")
    @RequiresPermission("fee:bill:view")
    public ApiResponse<FeeBillView> getBill(@PathVariable Long billId) {
        return ApiResponse.success(service.getBill(billId));
    }

    @PutMapping("/api/fee/bills/{billId}/void")
    @RequiresPermission("fee:bill:void")
    public ApiResponse<Void> voidBill(@PathVariable Long billId, @Valid @RequestBody ReasonRequest request) {
        service.voidBill(billId, request.reason());
        return ApiResponse.success();
    }
}
