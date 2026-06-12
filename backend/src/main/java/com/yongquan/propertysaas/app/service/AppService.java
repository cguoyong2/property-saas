package com.yongquan.propertysaas.app.service;

import com.yongquan.propertysaas.app.repository.AppRepository;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.payment.domain.PayOrderCreateResult;
import com.yongquan.propertysaas.payment.dto.PayOrderCreateRequest;
import com.yongquan.propertysaas.payment.service.PaymentService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AppService {

    private final AppRepository repository;
    private final PaymentService paymentService;

    public AppService(AppRepository repository, PaymentService paymentService) {
        this.repository = repository;
        this.paymentService = paymentService;
    }

    public Map<String, Object> home() {
        return repository.homeSummary(tenantId(), memberId());
    }

    public Map<String, Object> mine() {
        Map<String, Object> member = repository.getMember(tenantId(), memberId());
        member.put("summary", repository.homeSummary(tenantId(), memberId()));
        return member;
    }

    public PageResult<Map<String, Object>> houses() {
        var records = repository.findHouses(tenantId(), memberId());
        return new PageResult<>(records, records.size(), 1, Math.max(records.size(), 1));
    }

    public PageResult<Map<String, Object>> bills(Long houseId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        ensureApprovedHouse(houseId);
        return new PageResult<>(
                repository.findBills(tenantId(), memberId(), houseId, offset(pageNo, pageSize), pageSize),
                repository.countBills(tenantId(), memberId(), houseId),
                pageNo,
                pageSize);
    }

    public Map<String, Object> bill(Long billId) {
        return repository.getBill(tenantId(), memberId(), billId);
    }

    public PayOrderCreateResult createPayOrder(PayOrderCreateRequest request) {
        return paymentService.createOrder(request);
    }

    public PageResult<Map<String, Object>> vehicles(long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        return new PageResult<>(
                repository.findVehicles(tenantId(), memberId(), offset(pageNo, pageSize), pageSize),
                repository.countVehicles(tenantId(), memberId()),
                pageNo,
                pageSize);
    }

    private void ensureApprovedHouse(Long houseId) {
        if (!repository.approvedHouseExists(tenantId(), memberId(), houseId)) {
            throw new IllegalArgumentException("房屋未绑定或未审核通过：" + houseId);
        }
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long memberId() {
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
}
