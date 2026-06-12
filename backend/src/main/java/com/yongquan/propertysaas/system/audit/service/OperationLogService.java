package com.yongquan.propertysaas.system.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.common.api.TraceId;
import com.yongquan.propertysaas.system.audit.domain.OperationLogView;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.repository.OperationLogRepository;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogService {

    private final OperationLogRepository repository;
    private final ObjectMapper objectMapper;

    public OperationLogService(OperationLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(OperationLogWrite log) {
        repository.insert(newId(), log.tenantId(), log.projectId(), operatorType(), TenantContext.getUserId(),
                log.moduleCode(), log.actionCode(), log.objectType(), log.objectId(), json(log.beforeData()),
                json(withTrace(log.afterData())), log.reason(), clientIp(), userAgent());
    }

    public PageResult<OperationLogView> page(Long tenantId, Long projectId, String moduleCode, String actionCode,
                                             String objectType, Long objectId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        Long scopedTenantId = TenantContext.isPlatformUser() ? tenantId : TenantContext.requiredTenantId();
        return new PageResult<>(
                repository.find(scopedTenantId, projectId, moduleCode, actionCode, objectType, objectId, offset(pageNo, pageSize), pageSize),
                repository.count(scopedTenantId, projectId, moduleCode, actionCode, objectType, objectId),
                pageNo,
                pageSize
        );
    }

    private Object withTrace(Object afterData) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("traceId", TraceId.current());
        if (afterData instanceof Map<?, ?> map) {
            map.forEach((key, value) -> data.put(String.valueOf(key), value));
        } else if (afterData != null) {
            data.put("data", afterData);
        }
        return data;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("操作日志数据无法序列化", ex);
        }
    }

    private String operatorType() {
        String userType = TenantContext.getUserType();
        if ("PLATFORM".equals(userType)) {
            return "PLATFORM_USER";
        }
        if ("MEMBER".equals(userType)) {
            return "MEMBER";
        }
        return TenantContext.getUserId() == null ? "SYSTEM" : "TENANT_USER";
    }

    private String clientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
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
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }
}
