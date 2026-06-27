package com.yongquan.propertysaas.app.controller;

import com.yongquan.propertysaas.app.service.AppService;
import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.payment.domain.PayOrderCreateResult;
import com.yongquan.propertysaas.payment.domain.PaymentNotifyResult;
import com.yongquan.propertysaas.payment.dto.PayOrderCreateRequest;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {

    private final AppService service;

    public AppController(AppService service) {
        this.service = service;
    }

    @GetMapping("/api/app/home")
    @RequiresPermission("app:home:view")
    public ApiResponse<Map<String, Object>> home() {
        return ApiResponse.success(service.home());
    }

    @GetMapping("/api/app/houses")
    @RequiresPermission("app:house:list")
    public ApiResponse<PageResult<Map<String, Object>>> houses() {
        return ApiResponse.success(service.houses());
    }

    @GetMapping("/api/app/bind-options/projects")
    @RequiresPermission("app:house:list")
    public ApiResponse<PageResult<Map<String, Object>>> bindProjects() {
        return ApiResponse.success(service.bindProjects());
    }

    @GetMapping("/api/app/bind-options/buildings")
    @RequiresPermission("app:house:list")
    public ApiResponse<PageResult<Map<String, Object>>> bindBuildings(@RequestParam Long projectId) {
        return ApiResponse.success(service.bindBuildings(projectId));
    }

    @GetMapping("/api/app/bind-options/units")
    @RequiresPermission("app:house:list")
    public ApiResponse<PageResult<Map<String, Object>>> bindUnits(@RequestParam Long projectId,
                                                                  @RequestParam Long buildingId) {
        return ApiResponse.success(service.bindUnits(projectId, buildingId));
    }

    @GetMapping("/api/app/bind-options/houses")
    @RequiresPermission("app:house:list")
    public ApiResponse<PageResult<Map<String, Object>>> bindHouses(@RequestParam Long projectId,
                                                                   @RequestParam Long buildingId,
                                                                   @RequestParam Long unitId) {
        return ApiResponse.success(service.bindHouses(projectId, buildingId, unitId));
    }

    @GetMapping("/api/app/bills")
    @RequiresPermission("app:bill:list")
    public ApiResponse<PageResult<Map<String, Object>>> bills(@RequestParam Long houseId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(defaultValue = "1") long pageNo,
                                                              @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.bills(houseId, status, pageNo, pageSize));
    }

    @GetMapping("/api/app/bills/{billId}")
    @RequiresPermission("app:bill:view")
    public ApiResponse<Map<String, Object>> bill(@PathVariable Long billId) {
        return ApiResponse.success(service.bill(billId));
    }

    @PostMapping("/api/app/pay/orders")
    @RequiresPermission("app:pay:create")
    public ApiResponse<PayOrderCreateResult> createPayOrder(@Valid @RequestBody PayOrderCreateRequest request) {
        return ApiResponse.success(service.createPayOrder(request));
    }

    @PostMapping("/api/app/pay/orders/{orderNo}/demo-confirm")
    @RequiresPermission("app:pay:create")
    public ApiResponse<PaymentNotifyResult> confirmDemoPayOrder(@PathVariable String orderNo) {
        return ApiResponse.success(service.confirmDemoPayOrder(orderNo));
    }

    @GetMapping("/api/app/pay/orders")
    @RequiresPermission("app:bill:list")
    public ApiResponse<PageResult<Map<String, Object>>> payOrders(@RequestParam(required = false) String status,
                                                                  @RequestParam(defaultValue = "1") long pageNo,
                                                                  @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.payOrders(status, pageNo, pageSize));
    }

    @GetMapping("/api/app/pay/orders/{orderNo}")
    @RequiresPermission("app:bill:view")
    public ApiResponse<Map<String, Object>> payOrder(@PathVariable String orderNo) {
        return ApiResponse.success(service.payOrder(orderNo));
    }

    @GetMapping("/api/app/prepayments")
    @RequiresPermission("app:bill:list")
    public ApiResponse<PageResult<Map<String, Object>>> prepayments(@RequestParam(defaultValue = "1") long pageNo,
                                                                    @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.prepayments(pageNo, pageSize));
    }

    @GetMapping("/api/app/prepayments/summary")
    @RequiresPermission("app:bill:list")
    public ApiResponse<Map<String, Object>> prepaymentSummary() {
        return ApiResponse.success(service.prepaymentSummary());
    }

    @GetMapping("/api/app/vehicles")
    @RequiresPermission("app:vehicle:list")
    public ApiResponse<PageResult<Map<String, Object>>> vehicles(@RequestParam(defaultValue = "1") long pageNo,
                                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.vehicles(pageNo, pageSize));
    }

    @GetMapping("/api/app/mine")
    @RequiresPermission("app:mine:view")
    public ApiResponse<Map<String, Object>> mine() {
        return ApiResponse.success(service.mine());
    }
}
