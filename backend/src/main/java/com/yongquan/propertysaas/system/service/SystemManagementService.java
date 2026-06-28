package com.yongquan.propertysaas.system.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.system.domain.DeptView;
import com.yongquan.propertysaas.system.domain.MenuView;
import com.yongquan.propertysaas.system.domain.RoleView;
import com.yongquan.propertysaas.system.domain.UserView;
import com.yongquan.propertysaas.system.dto.DeptRequest;
import com.yongquan.propertysaas.system.dto.RoleMenuRequest;
import com.yongquan.propertysaas.system.dto.RoleRequest;
import com.yongquan.propertysaas.system.dto.UserPasswordRequest;
import com.yongquan.propertysaas.system.dto.UserProjectRequest;
import com.yongquan.propertysaas.system.dto.UserRequest;
import com.yongquan.propertysaas.system.dto.UserStatusRequest;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.system.repository.SystemManagementRepository;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemManagementService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> ROLE_LEVELS = Set.of("TENANT", "PROJECT");
    private static final Set<String> DATA_SCOPES = Set.of("ALL_TENANT", "PROJECT", "DEPT", "SELF", "CUSTOM");

    private final SystemManagementRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public SystemManagementService(SystemManagementRepository repository, PasswordEncoder passwordEncoder,
                                   OperationLogService operationLogService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    public List<DeptView> listDepts(String status) {
        return repository.findDepts(tenantId(), status);
    }

    @Transactional
    public Long createDept(DeptRequest request) {
        Long tenantId = tenantId();
        validateStatus(request.status());
        validateDeptParent(tenantId, request.parentId());
        Long deptId = newId();
        repository.insertDept(tenantId, deptId, request);
        return deptId;
    }

    @Transactional
    public void updateDept(Long deptId, DeptRequest request) {
        Long tenantId = tenantId();
        validateStatus(request.status());
        validateDeptParent(tenantId, request.parentId());
        repository.updateDept(tenantId, deptId, request);
    }

    public PageResult<UserView> pageUsers(String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        Long tenantId = tenantId();
        return new PageResult<>(
                repository.findUsers(tenantId, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countUsers(tenantId, keyword, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createUser(UserRequest request) {
        Long tenantId = tenantId();
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("初始密码长度不能小于8位");
        }
        validateStatus(request.status());
        validateDept(tenantId, request.deptId());
        validateRoles(tenantId, request.roleIds());
        validateProjects(tenantId, request.projectIds());
        Long userId = newId();
        repository.insertUser(tenantId, userId, request, passwordEncoder.encode(request.password()));
        repository.replaceUserRoles(tenantId, userId, request.roleIds());
        repository.replaceUserProjects(tenantId, userId, emptyIfNull(request.projectIds()));
        return userId;
    }

    @Transactional
    public void updateUser(Long userId, UserRequest request) {
        Long tenantId = tenantId();
        ensureUser(tenantId, userId);
        validateStatus(request.status());
        validateDept(tenantId, request.deptId());
        validateRoles(tenantId, request.roleIds());
        validateProjects(tenantId, request.projectIds());
        repository.updateUser(tenantId, userId, request);
        repository.replaceUserRoles(tenantId, userId, request.roleIds());
        repository.replaceUserProjects(tenantId, userId, emptyIfNull(request.projectIds()));
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 8) {
                throw new IllegalArgumentException("密码长度不能小于8位");
            }
            repository.resetPassword(tenantId, userId, passwordEncoder.encode(request.password()));
        }
    }

    @Transactional
    public void resetUserPassword(Long userId, UserPasswordRequest request) {
        Long tenantId = tenantId();
        ensureUser(tenantId, userId);
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("新密码长度不能小于8位");
        }
        repository.resetPassword(tenantId, userId, passwordEncoder.encode(request.password()));
        operationLogService.record(new OperationLogWrite(tenantId, null, "system", "USER_PASSWORD_RESET",
                "sys_user", userId, null, Map.of("reset", true), null));
    }

    @Transactional
    public void updateUserStatus(Long userId, UserStatusRequest request) {
        Long tenantId = tenantId();
        ensureUser(tenantId, userId);
        validateStatus(request.status());
        UserView user = repository.getUser(tenantId, userId);
        repository.updateUserStatus(tenantId, userId, request.status());
        operationLogService.record(new OperationLogWrite(tenantId, null, "system", "USER_STATUS_CHANGE",
                "sys_user", userId, Map.of("status", user.status()),
                Map.of("status", request.status()), null));
    }

    @Transactional
    public void replaceUserProjects(Long userId, UserProjectRequest request) {
        Long tenantId = tenantId();
        ensureUser(tenantId, userId);
        validateProjects(tenantId, request.projectIds());
        List<Long> beforeProjectIds = repository.findProjectIdsByUser(tenantId, userId);
        repository.replaceUserProjects(tenantId, userId, emptyIfNull(request.projectIds()));
        operationLogService.record(new OperationLogWrite(tenantId, null, "system", "USER_PROJECT_AUTHORIZE",
                "sys_user", userId, Map.of("projectIds", beforeProjectIds),
                Map.of("projectIds", emptyIfNull(request.projectIds())), null));
    }

    public PageResult<RoleView> pageRoles(String keyword, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        Long tenantId = tenantId();
        return new PageResult<>(
                repository.findRoles(tenantId, keyword, status, offset(pageNo, pageSize), pageSize),
                repository.countRoles(tenantId, keyword, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createRole(RoleRequest request) {
        Long tenantId = tenantId();
        validateRole(request);
        Long roleId = newId();
        repository.insertRole(tenantId, roleId, autoRoleCode(request.roleName(), roleId), request);
        return roleId;
    }

    @Transactional
    public void updateRole(Long roleId, RoleRequest request) {
        Long tenantId = tenantId();
        ensureRole(tenantId, roleId);
        validateRole(request);
        repository.updateRole(tenantId, roleId, autoRoleCode(request.roleName(), roleId), request);
    }

    @Transactional
    public void replaceRoleMenus(Long roleId, RoleMenuRequest request) {
        Long tenantId = tenantId();
        ensureRole(tenantId, roleId);
        validateMenus(request.menuIds());
        List<Long> beforeMenuIds = repository.findRoleMenuIds(tenantId, roleId);
        repository.replaceRoleMenus(tenantId, roleId, request.menuIds());
        operationLogService.record(new OperationLogWrite(tenantId, null, "system", "ROLE_MENU_AUTHORIZE",
                "sys_role", roleId, Map.of("menuIds", beforeMenuIds),
                Map.of("menuIds", request.menuIds()), null));
    }

    public List<Long> listRoleMenuIds(Long roleId) {
        Long tenantId = tenantId();
        ensureRole(tenantId, roleId);
        return repository.findRoleMenuIds(tenantId, roleId);
    }

    public List<MenuView> listMenus(String moduleCode) {
        return repository.findMenus(moduleCode);
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
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

    private void validateRole(RoleRequest request) {
        validateStatus(request.status());
        String roleLevel = request.roleLevel() == null || request.roleLevel().isBlank() ? "TENANT" : request.roleLevel();
        if (!ROLE_LEVELS.contains(roleLevel)) {
            throw new IllegalArgumentException("非法角色级别：" + roleLevel);
        }
        String dataScope = request.dataScope() == null || request.dataScope().isBlank() ? "SELF" : request.dataScope();
        if (!DATA_SCOPES.contains(dataScope)) {
            throw new IllegalArgumentException("非法数据范围：" + dataScope);
        }
    }

    private String autoRoleCode(String roleName, Long roleId) {
        String prefix = roleCodePrefix(roleName);
        return "%s_%06d".formatted(prefix, Math.abs(roleId % 1_000_000));
    }

    private String roleCodePrefix(String roleName) {
        String name = roleName == null ? "" : roleName.trim();
        if (name.contains("管理员") || name.contains("管理")) return "ADMIN_ROLE";
        if (name.contains("财务") || name.contains("收费") || name.contains("会计")) return "FINANCE_ROLE";
        if (name.contains("客服") || name.contains("前台")) return "SERVICE_ROLE";
        if (name.contains("工程") || name.contains("维修")) return "ENGINEERING_ROLE";
        if (name.contains("保安") || name.contains("安保") || name.contains("秩序")) return "SECURITY_ROLE";
        String asciiPrefix = name.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return asciiPrefix.isBlank() ? "ROLE" : asciiPrefix;
    }

    private void validateStatus(String status) {
        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法状态：" + status);
        }
    }

    private void validateDeptParent(Long tenantId, Long parentId) {
        if (parentId != null && parentId != 0 && !repository.deptExists(tenantId, parentId)) {
            throw new IllegalArgumentException("上级部门不存在：" + parentId);
        }
    }

    private void validateDept(Long tenantId, Long deptId) {
        if (!repository.deptExists(tenantId, deptId)) {
            throw new IllegalArgumentException("部门不存在：" + deptId);
        }
    }

    private void validateRoles(Long tenantId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("用户至少需要一个角色");
        }
        roleIds.forEach(roleId -> {
            if (!repository.roleExists(tenantId, roleId)) {
                throw new IllegalArgumentException("角色不存在：" + roleId);
            }
        });
    }

    private void validateMenus(List<Long> menuIds) {
        if (menuIds == null) {
            throw new IllegalArgumentException("菜单权限不能为空");
        }
        menuIds.forEach(menuId -> {
            if (!repository.menuExists(menuId)) {
                throw new IllegalArgumentException("菜单权限不存在：" + menuId);
            }
        });
    }

    private void validateProjects(Long tenantId, List<Long> projectIds) {
        emptyIfNull(projectIds).forEach(projectId -> {
            if (!repository.projectExists(tenantId, projectId)) {
                throw new IllegalArgumentException("项目不存在：" + projectId);
            }
        });
    }

    private void ensureUser(Long tenantId, Long userId) {
        if (!repository.userExists(tenantId, userId)) {
            throw new IllegalArgumentException("用户不存在：" + userId);
        }
    }

    private void ensureRole(Long tenantId, Long roleId) {
        if (!repository.roleExists(tenantId, roleId)) {
            throw new IllegalArgumentException("角色不存在：" + roleId);
        }
    }

    private List<Long> emptyIfNull(List<Long> values) {
        return values == null ? List.of() : values;
    }
}
