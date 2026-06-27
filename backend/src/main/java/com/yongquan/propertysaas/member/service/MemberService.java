package com.yongquan.propertysaas.member.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.member.domain.MemberHouseBindingView;
import com.yongquan.propertysaas.member.domain.MemberView;
import com.yongquan.propertysaas.member.dto.HouseBindingApplyRequest;
import com.yongquan.propertysaas.member.dto.MemberAuditRequest;
import com.yongquan.propertysaas.member.dto.MemberRequest;
import com.yongquan.propertysaas.member.dto.UnbindRequest;
import com.yongquan.propertysaas.member.dto.WxLoginRequest;
import com.yongquan.propertysaas.member.repository.MemberRepository;
import com.yongquan.propertysaas.security.domain.CurrentUser;
import com.yongquan.propertysaas.security.service.JwtService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private static final Set<String> BIND_ROLES = Set.of("OWNER", "FAMILY", "TENANT", "RESIDENT");
    private static final Set<String> AUDIT_RESULTS = Set.of("APPROVED", "REJECTED");
    private static final Set<String> MEMBER_STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final List<String> APP_PERMISSIONS = List.of(
            "app:home:view",
            "app:house:list",
            "app:house:bind",
            "app:house:unbind",
            "app:bill:list",
            "app:bill:view",
            "app:pay:create",
            "app:workorder:create",
            "app:workorder:list",
            "app:workorder:view",
            "app:workorder:evaluate",
            "app:complaint:create",
            "app:file:upload",
            "app:file:download",
            "app:notice:list",
            "app:visitor:create",
            "app:vehicle:list",
            "app:lease:contract:list",
            "app:mine:view"
    );

    private final MemberRepository repository;
    private final JwtService jwtService;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public MemberService(MemberRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @Transactional
    public Map<String, Object> wxLogin(WxLoginRequest request) {
        ensureTenant(request.tenantId());
        MemberView member = repository.findMemberByOpenid(request.tenantId(), request.openid())
                .map(existing -> {
                    validateMobileUnique(request.tenantId(), request.mobile(), existing.memberId());
                    repository.updateMemberLogin(request.tenantId(), existing.memberId(), request);
                    return existing;
                })
                .orElseGet(() -> {
                    validateMobileUnique(request.tenantId(), request.mobile(), null);
                    Long memberId = newId();
                    repository.insertMember(request.tenantId(), memberId, request);
                    return repository.findMemberByOpenid(request.tenantId(), request.openid()).orElseThrow();
                });
        String token = jwtService.createToken(new CurrentUser(
                member.memberId(),
                request.tenantId(),
                member.openid(),
                member.realName(),
                "MEMBER",
                member.status(),
                APP_PERMISSIONS
        ));
        return Map.of(
                "token", token,
                "memberId", member.memberId(),
                "tenantId", request.tenantId(),
                "openid", request.openid(),
                "mobile", member.mobile() == null ? "" : member.mobile(),
                "realName", member.realName() == null ? "" : member.realName(),
                "permissions", APP_PERMISSIONS
        );
    }

    public PageResult<MemberView> pageMembers(String keyword, Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        Long tenantId = tenantId();
        return new PageResult<>(
                repository.findMembers(tenantId, keyword, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countMembers(tenantId, keyword, projectId, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createMember(MemberRequest request) {
        Long tenantId = tenantId();
        validateMember(request, null);
        Long memberId = newId();
        repository.insertBackofficeMember(tenantId, memberId, request, normalizedOpenid(tenantId, memberId, request.openid()));
        repository.insertBackofficeBindingForAudit(tenantId, memberId, newId(), userId(), request);
        return memberId;
    }

    @Transactional
    public void updateMember(Long memberId, MemberRequest request) {
        Long tenantId = tenantId();
        validateMember(request, memberId);
        if (!repository.memberRecordExists(tenantId, memberId)) {
            throw new IllegalArgumentException("业主/住户不存在：" + memberId);
        }
        repository.updateBackofficeMember(tenantId, memberId, request, blankToNull(request.openid()));
        if (!repository.approvedBindingExists(tenantId, memberId, request.houseId())
                && !repository.pendingBindingExists(tenantId, memberId, request.houseId())) {
            repository.insertBackofficeBindingForAudit(tenantId, memberId, newId(), userId(), request);
        }
    }

    public PageResult<MemberHouseBindingView> pageBindings(Long projectId, Long memberId, String realName, String status,
                                                           long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findBindings(tenantId, scope, projectId, memberId, realName, status, offset(pageNo, pageSize), pageSize),
                repository.countBindings(tenantId, scope, projectId, memberId, realName, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long applyBinding(HouseBindingApplyRequest request) {
        ensureCurrentMember(request.tenantId(), request.memberId());
        ensureTenant(request.tenantId());
        validateBindingApply(request);
        Long bindId = newId();
        repository.insertBinding(bindId, request);
        return bindId;
    }

    @Transactional
    public void auditBinding(Long bindId, MemberAuditRequest request) {
        Long tenantId = tenantId();
        if (!AUDIT_RESULTS.contains(request.auditResult())) {
            throw new IllegalArgumentException("非法审核结果：" + request.auditResult());
        }
        MemberHouseBindingView binding = repository.getBinding(tenantId, bindId);
        ensureProjectAllowed(binding.projectId());
        if (!"PENDING".equals(binding.status())) {
            throw new IllegalArgumentException("仅待审核绑定可审核");
        }
        repository.auditBinding(tenantId, bindId, request.auditResult(), userId(), request.auditRemark());
    }

    @Transactional
    public void unbind(Long bindId, UnbindRequest request) {
        Long tenantId = tenantId();
        MemberHouseBindingView binding = repository.getBinding(tenantId, bindId);
        ensureProjectAllowed(binding.projectId());
        if (!"APPROVED".equals(binding.status())) {
            throw new IllegalArgumentException("仅已通过绑定可解绑");
        }
        repository.unbind(tenantId, bindId, userId(), request == null ? null : request.reason());
    }

    @Transactional
    public void appUnbind(Long bindId, UnbindRequest request) {
        Long tenantId = tenantId();
        MemberHouseBindingView binding = repository.getBinding(tenantId, bindId);
        ensureCurrentMember(tenantId, binding.memberId());
        if (!"APPROVED".equals(binding.status())) {
            throw new IllegalArgumentException("仅已通过绑定可解绑");
        }
        repository.unbind(tenantId, bindId, userId(), request == null ? null : request.reason());
    }

    private void validateBindingApply(HouseBindingApplyRequest request) {
        if (!BIND_ROLES.contains(request.bindRole())) {
            throw new IllegalArgumentException("非法绑定角色：" + request.bindRole());
        }
        if (!repository.memberExists(request.tenantId(), request.memberId())) {
            throw new IllegalArgumentException("会员不存在：" + request.memberId());
        }
        if (!repository.houseExists(request.tenantId(), request.projectId(), request.houseId())) {
            throw new IllegalArgumentException("房屋不存在或不属于项目：" + request.houseId());
        }
        if (repository.approvedBindingExists(request.tenantId(), request.memberId(), request.houseId())) {
            throw new IllegalArgumentException("该会员已绑定该房屋");
        }
        if (repository.pendingBindingExists(request.tenantId(), request.memberId(), request.houseId())) {
            throw new IllegalArgumentException("该会员已有待审核绑定申请");
        }
    }

    private void validateMember(MemberRequest request, Long excludeMemberId) {
        if (request.status() != null && !request.status().isBlank() && !MEMBER_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("非法状态：" + request.status());
        }
        if (!BIND_ROLES.contains(bindRole(request))) {
            throw new IllegalArgumentException("非法住户类型：" + request.bindRole());
        }
        Long tenantId = tenantId();
        ensureProjectAllowed(request.projectId());
        validateMobileUnique(tenantId, request.mobile(), excludeMemberId);
        if (!repository.houseHierarchyExists(tenantId, request.projectId(), request.buildingId(), request.unitId(), request.houseId())) {
            throw new IllegalArgumentException("房屋不存在或不属于所选小区、楼栋、单元");
        }
    }

    private void validateMobileUnique(Long tenantId, String mobile, Long excludeMemberId) {
        String value = blankToNull(mobile);
        if (value != null && repository.mobileExists(tenantId, value, excludeMemberId)) {
            throw new IllegalArgumentException("手机号已存在：" + value);
        }
    }

    private String bindRole(MemberRequest request) {
        return request.bindRole() == null || request.bindRole().isBlank() ? "OWNER" : request.bindRole();
    }

    private String normalizedOpenid(Long tenantId, Long memberId, String openid) {
        String value = blankToNull(openid);
        return value == null ? "BACKOFFICE-" + tenantId + "-" + memberId : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void ensureTenant(Long tenantId) {
        if (!repository.tenantExists(tenantId)) {
            throw new IllegalArgumentException("租户不存在或不可用：" + tenantId);
        }
    }

    private void ensureCurrentMember(Long tenantId, Long memberId) {
        if (!"MEMBER".equals(TenantContext.getUserType())) {
            throw new AccessDeniedException("仅微信会员可提交房屋绑定申请");
        }
        if (!tenantId.equals(TenantContext.requiredTenantId())) {
            throw new AccessDeniedException("绑定申请租户与当前登录租户不一致");
        }
        if (!memberId.equals(TenantContext.getUserId())) {
            throw new AccessDeniedException("绑定申请会员与当前登录会员不一致");
        }
    }

    private void ensureProjectAllowed(Long projectId) {
        List<Long> scope = projectScope(tenantId());
        if (scope != null && !scope.contains(projectId)) {
            throw new AccessDeniedException("无项目数据权限：" + projectId);
        }
    }

    private List<Long> projectScope(Long tenantId) {
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
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

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
