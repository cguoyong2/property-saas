package com.yongquan.propertysaas.fee.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.fee.domain.FeeItemView;
import com.yongquan.propertysaas.fee.domain.FeeStandardBindView;
import com.yongquan.propertysaas.fee.domain.FeeStandardView;
import com.yongquan.propertysaas.fee.dto.FeeItemRequest;
import com.yongquan.propertysaas.fee.dto.FeeStandardBindRequest;
import com.yongquan.propertysaas.fee.dto.FeeStandardRequest;
import com.yongquan.propertysaas.fee.service.FeeService;
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
public class FeeController {

    private final FeeService service;

    public FeeController(FeeService service) {
        this.service = service;
    }

    @GetMapping("/api/fee/items")
    @RequiresPermission("fee:item:list")
    public ApiResponse<PageResult<FeeItemView>> pageItems(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") long pageNo,
                                                          @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageItems(keyword, status, pageNo, pageSize));
    }

    @PostMapping("/api/fee/items")
    @RequiresPermission("fee:item:create")
    public ApiResponse<Map<String, Long>> createItem(@Valid @RequestBody FeeItemRequest request) {
        return ApiResponse.success(Map.of("itemId", service.createItem(request)));
    }

    @GetMapping("/api/fee/items/{itemId}")
    @RequiresPermission("fee:item:view")
    public ApiResponse<FeeItemView> getItem(@PathVariable Long itemId) {
        return ApiResponse.success(service.getItem(itemId));
    }

    @PutMapping("/api/fee/items/{itemId}")
    @RequiresPermission("fee:item:update")
    public ApiResponse<Void> updateItem(@PathVariable Long itemId, @Valid @RequestBody FeeItemRequest request) {
        service.updateItem(itemId, request);
        return ApiResponse.success();
    }

    @GetMapping("/api/fee/standards")
    @RequiresPermission("fee:standard:list")
    public ApiResponse<PageResult<FeeStandardView>> pageStandards(@RequestParam(required = false) Long projectId,
                                                                  @RequestParam(required = false) Long itemId,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(defaultValue = "1") long pageNo,
                                                                  @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageStandards(projectId, itemId, status, pageNo, pageSize));
    }

    @PostMapping("/api/fee/standards")
    @RequiresPermission("fee:standard:create")
    public ApiResponse<Map<String, Long>> createStandard(@Valid @RequestBody FeeStandardRequest request) {
        return ApiResponse.success(Map.of("standardId", service.createStandard(request)));
    }

    @GetMapping("/api/fee/standards/{standardId}")
    @RequiresPermission("fee:standard:view")
    public ApiResponse<FeeStandardView> getStandard(@PathVariable Long standardId) {
        return ApiResponse.success(service.getStandard(standardId));
    }

    @PutMapping("/api/fee/standards/{standardId}")
    @RequiresPermission("fee:standard:update")
    public ApiResponse<Void> updateStandard(@PathVariable Long standardId, @Valid @RequestBody FeeStandardRequest request) {
        service.updateStandard(standardId, request);
        return ApiResponse.success();
    }

    @GetMapping("/api/fee/standard-binds")
    @RequiresPermission("fee:standardBind:list")
    public ApiResponse<PageResult<FeeStandardBindView>> pageBinds(@RequestParam(required = false) Long projectId,
                                                                  @RequestParam(required = false) Long standardId,
                                                                  @RequestParam(required = false) String objectType,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(defaultValue = "1") long pageNo,
                                                                  @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageBinds(projectId, standardId, objectType, status, pageNo, pageSize));
    }

    @PostMapping("/api/fee/standard-binds")
    @RequiresPermission("fee:standardBind:create")
    public ApiResponse<Map<String, Long>> createBind(@Valid @RequestBody FeeStandardBindRequest request) {
        return ApiResponse.success(Map.of("bindId", service.createBind(request)));
    }
}
