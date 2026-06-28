package com.yongquan.propertysaas.system.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
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
import com.yongquan.propertysaas.system.service.SystemManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemManagementController {

    private final SystemManagementService service;

    public SystemManagementController(SystemManagementService service) {
        this.service = service;
    }

    @GetMapping("/depts")
    @RequiresPermission("system:dept:list")
    public ApiResponse<List<DeptView>> listDepts(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.listDepts(status));
    }

    @PostMapping("/depts")
    @RequiresPermission("system:dept:create")
    public ApiResponse<Map<String, Long>> createDept(@Valid @RequestBody DeptRequest request) {
        return ApiResponse.success(Map.of("deptId", service.createDept(request)));
    }

    @PutMapping("/depts/{deptId}")
    @RequiresPermission("system:dept:update")
    public ApiResponse<Void> updateDept(@PathVariable Long deptId, @Valid @RequestBody DeptRequest request) {
        service.updateDept(deptId, request);
        return ApiResponse.success();
    }

    @GetMapping("/users")
    @RequiresPermission("system:user:list")
    public ApiResponse<PageResult<UserView>> pageUsers(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "1") long pageNo,
                                                       @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageUsers(keyword, status, pageNo, pageSize));
    }

    @PostMapping("/users")
    @RequiresPermission("system:user:create")
    public ApiResponse<Map<String, Long>> createUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success(Map.of("userId", service.createUser(request)));
    }

    @PutMapping("/users/{userId}")
    @RequiresPermission("system:user:update")
    public ApiResponse<Void> updateUser(@PathVariable Long userId, @Valid @RequestBody UserRequest request) {
        service.updateUser(userId, request);
        return ApiResponse.success();
    }

    @PutMapping("/users/{userId}/password")
    @RequiresPermission("system:user:update")
    public ApiResponse<Void> resetUserPassword(@PathVariable Long userId,
                                               @Valid @RequestBody UserPasswordRequest request) {
        service.resetUserPassword(userId, request);
        return ApiResponse.success();
    }

    @PutMapping("/users/{userId}/status")
    @RequiresPermission("system:user:status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusRequest request) {
        service.updateUserStatus(userId, request);
        return ApiResponse.success();
    }

    @PutMapping("/users/{userId}/projects")
    @RequiresPermission("system:user:project")
    public ApiResponse<Void> replaceUserProjects(@PathVariable Long userId, @Valid @RequestBody UserProjectRequest request) {
        service.replaceUserProjects(userId, request);
        return ApiResponse.success();
    }

    @GetMapping("/roles")
    @RequiresPermission("system:role:list")
    public ApiResponse<PageResult<RoleView>> pageRoles(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "1") long pageNo,
                                                       @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageRoles(keyword, status, pageNo, pageSize));
    }

    @PostMapping("/roles")
    @RequiresPermission("system:role:create")
    public ApiResponse<Map<String, Long>> createRole(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.success(Map.of("roleId", service.createRole(request)));
    }

    @PutMapping("/roles/{roleId}")
    @RequiresPermission("system:role:update")
    public ApiResponse<Void> updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleRequest request) {
        service.updateRole(roleId, request);
        return ApiResponse.success();
    }

    @GetMapping("/roles/{roleId}/menus")
    @RequiresPermission("system:role:menu:view")
    public ApiResponse<List<Long>> listRoleMenuIds(@PathVariable Long roleId) {
        return ApiResponse.success(service.listRoleMenuIds(roleId));
    }

    @PutMapping("/roles/{roleId}/menus")
    @RequiresPermission("system:role:menu:update")
    public ApiResponse<Void> replaceRoleMenus(@PathVariable Long roleId, @Valid @RequestBody RoleMenuRequest request) {
        service.replaceRoleMenus(roleId, request);
        return ApiResponse.success();
    }

    @GetMapping("/menus")
    @RequiresPermission("system:menu:list")
    public ApiResponse<List<MenuView>> listMenus(@RequestParam(required = false) String moduleCode) {
        return ApiResponse.success(service.listMenus(moduleCode));
    }
}
