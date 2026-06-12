package com.yongquan.propertysaas.lease.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
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
import com.yongquan.propertysaas.lease.service.LeaseService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeaseController {

    private final LeaseService service;

    public LeaseController(LeaseService service) {
        this.service = service;
    }

    @GetMapping("/api/lease/resources")
    @RequiresPermission("lease:resource:list")
    public ApiResponse<PageResult<LeaseResourceView>> resources(@RequestParam(required = false) Long projectId,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(defaultValue = "1") long pageNo,
                                                                @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageResources(projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/lease/resources")
    @RequiresPermission("lease:resource:create")
    public ApiResponse<Map<String, Long>> createResource(@Valid @RequestBody LeaseResourceRequest request) {
        return ApiResponse.success(Map.of("resourceId", service.createResource(request)));
    }

    @GetMapping("/api/lease/customers")
    @RequiresPermission("lease:customer:list")
    public ApiResponse<PageResult<LeaseCustomerView>> customers(@RequestParam(required = false) Long projectId,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "1") long pageNo,
                                                               @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageCustomers(projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/lease/customers")
    @RequiresPermission("lease:customer:create")
    public ApiResponse<Map<String, Long>> createCustomer(@Valid @RequestBody LeaseCustomerRequest request) {
        return ApiResponse.success(Map.of("customerId", service.createCustomer(request)));
    }

    @GetMapping("/api/lease/customers/{customerId}/follows")
    @RequiresPermission("lease:customer:follow")
    public ApiResponse<List<LeaseFollowRecordView>> follows(@PathVariable Long customerId) {
        return ApiResponse.success(service.customerFollows(customerId));
    }

    @PostMapping("/api/lease/customers/{customerId}/follows")
    @RequiresPermission("lease:customer:follow")
    public ApiResponse<Map<String, Long>> addFollow(@PathVariable Long customerId,
                                                    @Valid @RequestBody LeaseFollowRequest request) {
        return ApiResponse.success(Map.of("followId", service.addFollow(customerId, request)));
    }

    @GetMapping("/api/lease/contracts")
    @RequiresPermission("lease:contract:list")
    public ApiResponse<PageResult<LeaseContractView>> contracts(@RequestParam(required = false) Long projectId,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = false) Long customerId,
                                                                @RequestParam(defaultValue = "1") long pageNo,
                                                                @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageContracts(projectId, status, customerId, pageNo, pageSize));
    }

    @PostMapping("/api/lease/contracts")
    @RequiresPermission("lease:contract:create")
    public ApiResponse<Map<String, Long>> createContract(@Valid @RequestBody LeaseContractRequest request) {
        return ApiResponse.success(Map.of("contractId", service.createContract(request)));
    }

    @GetMapping("/api/lease/contracts/{contractId}")
    @RequiresPermission("lease:contract:view")
    public ApiResponse<LeaseContractDetailView> getContract(@PathVariable Long contractId) {
        return ApiResponse.success(service.getContract(contractId));
    }

    @PutMapping("/api/lease/contracts/{contractId}/activate")
    @RequiresPermission("lease:contract:activate")
    public ApiResponse<Void> activate(@PathVariable Long contractId) {
        service.activateContract(contractId);
        return ApiResponse.success();
    }

    @PutMapping("/api/lease/contracts/{contractId}/terminate")
    @RequiresPermission("lease:contract:terminate")
    public ApiResponse<Void> terminate(@PathVariable Long contractId,
                                       @RequestBody(required = false) LeaseContractStatusRequest request) {
        service.terminateContract(contractId, request);
        return ApiResponse.success();
    }

    @PostMapping("/api/lease/contracts/expire-remind")
    @RequiresPermission("lease:contract:remind")
    public ApiResponse<Map<String, Integer>> remindExpiring(@RequestParam(required = false) Integer days) {
        return ApiResponse.success(Map.of("messageCount", service.remindExpiring(days)));
    }

    @GetMapping("/api/app/lease/contracts")
    @RequiresPermission("app:lease:contract:list")
    public ApiResponse<PageResult<LeaseContractView>> appContracts(@RequestParam Long projectId,
                                                                   @RequestParam(required = false) String mobile,
                                                                   @RequestParam(defaultValue = "1") long pageNo,
                                                                   @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.appContracts(projectId, mobile, pageNo, pageSize));
    }
}
