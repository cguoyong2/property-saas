package com.yongquan.propertysaas.device.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.device.adapter.AdapterResult;
import com.yongquan.propertysaas.device.adapter.DeviceAdapter;
import com.yongquan.propertysaas.device.domain.AccessPermissionView;
import com.yongquan.propertysaas.device.domain.AccessRecordView;
import com.yongquan.propertysaas.device.domain.DeviceConfigView;
import com.yongquan.propertysaas.device.domain.DeviceSyncResultView;
import com.yongquan.propertysaas.device.domain.VisitorRecordView;
import com.yongquan.propertysaas.device.dto.AccessPermissionRequest;
import com.yongquan.propertysaas.device.dto.AccessRecordRequest;
import com.yongquan.propertysaas.device.dto.AccessSyncRequest;
import com.yongquan.propertysaas.device.dto.DeviceConfigRequest;
import com.yongquan.propertysaas.device.dto.VisitorInviteRequest;
import com.yongquan.propertysaas.device.repository.DeviceRepository;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private static final Set<String> DEVICE_TYPES = Set.of("ACCESS", "ELEVATOR", "PARKING", "CAMERA");
    private static final Set<String> DEVICE_STATUSES = Set.of("ACTIVE", "DISABLED", "OFFLINE");
    private static final Set<String> VISITOR_STATUSES = Set.of("PENDING", "APPROVED", "USED", "REJECTED", "EXPIRED", "CANCELLED");
    private static final Set<String> PERMISSION_TYPES = Set.of("FACE", "CARD", "QRCODE", "REMOTE");
    private static final Set<String> PERMISSION_STATUSES = Set.of("ACTIVE", "DISABLED", "EXPIRED", "CANCELLED");
    private static final Set<String> SYNC_STATUSES = Set.of("PENDING", "SYNCED", "FAILED");

    private final DeviceRepository repository;
    private final DeviceAdapter adapter;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public DeviceService(DeviceRepository repository, DeviceAdapter adapter, OperationLogService operationLogService,
                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.adapter = adapter;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    public PageResult<DeviceConfigView> pageDevices(Long projectId, String deviceType, String vendorCode, String status,
                                                    long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(deviceType, DEVICE_TYPES, "设备类型");
        validateIfPresent(status, DEVICE_STATUSES, "设备状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findDevices(tenantId, scope, projectId, normalize(deviceType, null),
                normalize(vendorCode, null), normalize(status, null), offset(pageNo, pageSize), pageSize),
                repository.countDevices(tenantId, scope, projectId, normalize(deviceType, null), normalize(vendorCode, null),
                        normalize(status, null)), pageNo, pageSize);
    }

    public DeviceConfigView getDevice(Long deviceId) {
        DeviceConfigView device = repository.getDevice(tenantId(), deviceId);
        ensureProjectAllowed(device.projectId());
        return device;
    }

    @Transactional
    public Long createDevice(DeviceConfigRequest request) {
        validateDevice(request);
        Long id = newId();
        DeviceConfigRequest normalized = normalizeDevice(request);
        repository.insertDevice(tenantId(), id, userId(), normalized);
        operationLogService.record(new OperationLogWrite(tenantId(), normalized.projectId(), "device", "DEVICE_CONFIG_CREATE",
                "device_config", id, null, Map.of("deviceType", normalized.deviceType(), "status", normalized.status()), null));
        return id;
    }

    @Transactional
    public void updateDevice(Long deviceId, DeviceConfigRequest request) {
        DeviceConfigView before = getDevice(deviceId);
        validateDevice(request);
        DeviceConfigRequest normalized = normalizeDevice(request);
        repository.updateDevice(tenantId(), deviceId, userId(), normalized);
        operationLogService.record(new OperationLogWrite(tenantId(), before.projectId(), "device", "DEVICE_CONFIG_UPDATE",
                "device_config", deviceId, Map.of("deviceType", before.deviceType(), "status", before.status()),
                Map.of("deviceType", normalized.deviceType(), "status", normalized.status()), null));
    }

    public PageResult<VisitorRecordView> pageVisitors(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, VISITOR_STATUSES, "访客状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findVisitors(tenantId, scope, projectId, normalize(status, null),
                offset(pageNo, pageSize), pageSize), repository.countVisitors(tenantId, scope, projectId, normalize(status, null)),
                pageNo, pageSize);
    }

    @Transactional
    public Long createVisitorInvite(VisitorInviteRequest request) {
        validateVisitor(request);
        Long visitorId = newId();
        repository.insertVisitor(tenantId(), visitorId, request, "VISITOR-" + tenantId() + "-" + visitorId);
        if (request.deviceIds() != null) {
            for (Long deviceId : request.deviceIds()) {
                if (deviceId != null) {
                    ensureDeviceAllowed(request.projectId(), deviceId);
                    repository.insertAccessPermission(tenantId(), newId(),
                            new AccessPermissionRequest(request.projectId(), null, null, visitorId, deviceId, "QRCODE",
                                    request.validStartAt(), request.validEndAt(), "ACTIVE"));
                }
            }
        }
        return visitorId;
    }

    public PageResult<AccessPermissionView> pageAccessPermissions(Long projectId, Long deviceId, String status, String syncStatus,
                                                                  long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateIfPresent(status, PERMISSION_STATUSES, "门禁权限状态");
        validateIfPresent(syncStatus, SYNC_STATUSES, "同步状态");
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        if (deviceId != null) {
            DeviceConfigView device = getDevice(deviceId);
            if (projectId != null && !device.projectId().equals(projectId)) {
                throw new IllegalArgumentException("设备不属于当前项目：" + deviceId);
            }
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findAccessPermissions(tenantId, scope, projectId, deviceId, normalize(status, null),
                normalize(syncStatus, null), offset(pageNo, pageSize), pageSize),
                repository.countAccessPermissions(tenantId, scope, projectId, deviceId, normalize(status, null),
                        normalize(syncStatus, null)), pageNo, pageSize);
    }

    @Transactional
    public Long createAccessPermission(AccessPermissionRequest request) {
        validatePermission(request);
        Long id = newId();
        repository.insertAccessPermission(tenantId(), id, normalizePermission(request));
        return id;
    }

    public PageResult<AccessRecordView> pageAccessRecords(Long projectId, Long deviceId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        if (deviceId != null) {
            DeviceConfigView device = getDevice(deviceId);
            if (projectId != null && !device.projectId().equals(projectId)) {
                throw new IllegalArgumentException("设备不属于当前项目：" + deviceId);
            }
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(repository.findAccessRecords(tenantId, scope, projectId, deviceId, offset(pageNo, pageSize), pageSize),
                repository.countAccessRecords(tenantId, scope, projectId, deviceId), pageNo, pageSize);
    }

    @Transactional
    public Long createAccessRecord(AccessRecordRequest request) {
        ensureProjectAllowed(request.projectId());
        if (request.deviceId() != null) {
            ensureDeviceAllowed(request.projectId(), request.deviceId());
        }
        if (request.memberId() != null && !repository.memberExists(tenantId(), request.memberId())) {
            throw new IllegalArgumentException("会员不存在：" + request.memberId());
        }
        if (!repository.visitorExists(tenantId(), request.projectId(), request.visitorId())) {
            throw new IllegalArgumentException("访客不存在或不属于项目：" + request.visitorId());
        }
        Long id = newId();
        repository.insertAccessRecord(id, tenantId(), request);
        return id;
    }

    @Transactional
    public DeviceSyncResultView syncAccessPermissions(AccessSyncRequest request) {
        Long projectId = request == null ? null : request.projectId();
        Long deviceId = request == null ? null : request.deviceId();
        int limit = request == null || request.limit() == null ? 50 : Math.max(1, Math.min(request.limit(), 200));
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        if (deviceId != null) {
            DeviceConfigView device = getDevice(deviceId);
            if (projectId != null && !device.projectId().equals(projectId)) {
                throw new IllegalArgumentException("设备不属于当前项目：" + deviceId);
            }
        }
        Long tenantId = tenantId();
        List<AccessPermissionView> pending = repository.findPendingPermissions(tenantId, projectScope(tenantId), projectId, deviceId, limit);
        int success = 0;
        int failure = 0;
        for (AccessPermissionView permission : pending) {
            DeviceConfigView device = repository.getDevice(tenantId, permission.deviceId());
            String payload = syncPayload(device, permission);
            AdapterResult result = adapter.syncAccessPermission(device, permission, payload);
            repository.insertInterfaceLog(newId(), tenantId, permission.projectId(), "ACCESS_PERMISSION_SYNC", device.vendorCode(),
                    "ACCESS-" + permission.permissionId(), result.requestUrl(), payload, result.responseBody(),
                    result.success(), result.errorMessage(), result.costMs());
            if (result.success()) {
                repository.updatePermissionSyncStatus(tenantId, permission.permissionId(), "SYNCED");
                success++;
            } else {
                repository.updatePermissionSyncStatus(tenantId, permission.permissionId(), "FAILED");
                failure++;
            }
        }
        operationLogService.record(new OperationLogWrite(tenantId, projectId, "device", "ACCESS_PERMISSION_SYNC",
                "access_permission", deviceId, Map.of("pendingCount", pending.size()),
                Map.of("success", success, "failure", failure, "limit", limit), null));
        return new DeviceSyncResultView(pending.size(), success, failure);
    }

    private void validateDevice(DeviceConfigRequest request) {
        ensureProjectAllowed(request.projectId());
        validate(normalize(request.deviceType(), null), DEVICE_TYPES, "设备类型");
        validate(normalize(request.status(), "ACTIVE"), DEVICE_STATUSES, "设备状态");
        if (request.configJson() != null && !request.configJson().isBlank()) {
            readJson(request.configJson(), "设备配置JSON格式错误");
        }
    }

    private void validateVisitor(VisitorInviteRequest request) {
        ensureProjectAllowed(request.projectId());
        if (request.inviterMemberId() != null && !repository.memberExists(tenantId(), request.inviterMemberId())) {
            throw new IllegalArgumentException("邀请人会员不存在：" + request.inviterMemberId());
        }
        if (!request.validEndAt().isAfter(request.validStartAt())) {
            throw new IllegalArgumentException("访客有效结束时间必须晚于开始时间");
        }
    }

    private void validatePermission(AccessPermissionRequest request) {
        ensureProjectAllowed(request.projectId());
        ensureDeviceAllowed(request.projectId(), request.deviceId());
        validate(normalize(request.permissionType(), null), PERMISSION_TYPES, "门禁权限类型");
        validate(normalize(request.status(), "ACTIVE"), PERMISSION_STATUSES, "门禁权限状态");
        if (request.memberId() == null && request.userId() == null && request.visitorId() == null) {
            throw new IllegalArgumentException("门禁权限必须关联会员、员工或访客");
        }
        if (request.memberId() != null && !repository.memberExists(tenantId(), request.memberId())) {
            throw new IllegalArgumentException("会员不存在：" + request.memberId());
        }
        if (request.userId() != null && !repository.userExists(tenantId(), request.userId())) {
            throw new IllegalArgumentException("员工不存在：" + request.userId());
        }
        if (!repository.visitorExists(tenantId(), request.projectId(), request.visitorId())) {
            throw new IllegalArgumentException("访客不存在或不属于项目：" + request.visitorId());
        }
        if (request.endAt() != null && !request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("权限结束时间必须晚于开始时间");
        }
    }

    private DeviceConfigRequest normalizeDevice(DeviceConfigRequest request) {
        return new DeviceConfigRequest(request.projectId(), normalize(request.deviceType(), null), normalize(request.vendorCode(), null),
                request.deviceCode().trim(), request.deviceName().trim(), request.location(), request.configJson(),
                normalize(request.status(), "ACTIVE"));
    }

    private AccessPermissionRequest normalizePermission(AccessPermissionRequest request) {
        return new AccessPermissionRequest(request.projectId(), request.memberId(), request.userId(), request.visitorId(),
                request.deviceId(), normalize(request.permissionType(), null), request.startAt(), request.endAt(),
                normalize(request.status(), "ACTIVE"));
    }

    private void ensureDeviceAllowed(Long projectId, Long deviceId) {
        if (!repository.deviceExists(tenantId(), projectId, deviceId)) {
            throw new IllegalArgumentException("设备不存在或不属于项目：" + deviceId);
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

    private String syncPayload(DeviceConfigView device, AccessPermissionView permission) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId());
        payload.put("projectId", permission.projectId());
        payload.put("vendorCode", device.vendorCode());
        payload.put("deviceCode", device.deviceCode());
        payload.put("permissionId", permission.permissionId());
        payload.put("permissionType", permission.permissionType());
        payload.put("memberId", permission.memberId());
        payload.put("userId", permission.userId());
        payload.put("visitorId", permission.visitorId());
        payload.put("startAt", permission.startAt());
        payload.put("endAt", permission.endAt());
        payload.put("status", permission.status());
        return toJson(payload);
    }

    private void readJson(String json, String message) {
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("设备同步数据序列化失败", ex);
        }
    }

    private List<Long> projectScope(Long tenantId) {
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private void validateIfPresent(String value, Set<String> allowed, String label) {
        if (value != null && !value.isBlank()) {
            validate(normalize(value, null), allowed, label);
        }
    }

    private void validate(String value, Set<String> allowed, String label) {
        if (value == null || !allowed.contains(value)) {
            throw new IllegalArgumentException("非法" + label + "：" + value);
        }
    }

    private String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("分页参数错误");
        }
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
    }

    private long offset(long pageNo, long pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
