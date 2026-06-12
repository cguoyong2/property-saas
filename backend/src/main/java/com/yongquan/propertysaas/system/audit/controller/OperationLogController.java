package com.yongquan.propertysaas.system.audit.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import com.yongquan.propertysaas.system.audit.domain.OperationLogView;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperationLogController {

    private final OperationLogService service;

    public OperationLogController(OperationLogService service) {
        this.service = service;
    }

    @GetMapping("/api/system/operation-logs")
    @RequiresPermission("system:operationLog:list")
    public ApiResponse<PageResult<OperationLogView>> page(@RequestParam(required = false) Long tenantId,
                                                          @RequestParam(required = false) Long projectId,
                                                          @RequestParam(required = false) String moduleCode,
                                                          @RequestParam(required = false) String actionCode,
                                                          @RequestParam(required = false) String objectType,
                                                          @RequestParam(required = false) Long objectId,
                                                          @RequestParam(defaultValue = "1") long pageNo,
                                                          @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.page(tenantId, projectId, moduleCode, actionCode, objectType, objectId, pageNo, pageSize));
    }
}
