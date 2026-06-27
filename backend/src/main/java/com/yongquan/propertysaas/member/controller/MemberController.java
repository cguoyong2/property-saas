package com.yongquan.propertysaas.member.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.member.domain.MemberHouseBindingView;
import com.yongquan.propertysaas.member.domain.MemberView;
import com.yongquan.propertysaas.member.dto.FamilyMemberInviteRequest;
import com.yongquan.propertysaas.member.dto.HouseBindingApplyRequest;
import com.yongquan.propertysaas.member.dto.MemberAuditRequest;
import com.yongquan.propertysaas.member.dto.MemberRequest;
import com.yongquan.propertysaas.member.dto.UnbindRequest;
import com.yongquan.propertysaas.member.dto.WxLoginRequest;
import com.yongquan.propertysaas.member.service.MemberService;
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
public class MemberController {

    private final MemberService service;

    public MemberController(MemberService service) {
        this.service = service;
    }

    @PostMapping("/api/app/auth/wx-login")
    public ApiResponse<Map<String, Object>> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.success(service.wxLogin(request));
    }

    @PostMapping("/api/app/house-bindings")
    @RequiresPermission("app:house:bind")
    public ApiResponse<Map<String, Long>> applyBinding(@Valid @RequestBody HouseBindingApplyRequest request) {
        return ApiResponse.success(Map.of("bindId", service.applyBinding(request)));
    }

    @PutMapping("/api/app/house-bindings/{bindId}/unbind")
    @RequiresPermission("app:house:unbind")
    public ApiResponse<Void> appUnbind(@PathVariable Long bindId, @RequestBody(required = false) UnbindRequest request) {
        service.appUnbind(bindId, request);
        return ApiResponse.success();
    }

    @GetMapping("/api/app/family-members")
    @RequiresPermission("app:family:list")
    public ApiResponse<PageResult<MemberHouseBindingView>> familyMembers(@RequestParam Long houseId) {
        return ApiResponse.success(service.familyMembers(houseId));
    }

    @PostMapping("/api/app/family-members")
    @RequiresPermission("app:family:manage")
    public ApiResponse<Map<String, Long>> inviteFamilyMember(@Valid @RequestBody FamilyMemberInviteRequest request) {
        return ApiResponse.success(Map.of("bindId", service.inviteFamilyMember(request)));
    }

    @GetMapping("/api/base/members")
    @RequiresPermission("base:member:list")
    public ApiResponse<PageResult<MemberView>> pageMembers(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String realName,
                                                           @RequestParam(required = false) Long projectId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "1") long pageNo,
                                                           @RequestParam(defaultValue = "20") long pageSize) {
        if ((keyword == null || keyword.isBlank()) && realName != null && !realName.isBlank()) {
            keyword = realName;
        }
        return ApiResponse.success(service.pageMembers(keyword, projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/base/members")
    @RequiresPermission("base:member:create")
    public ApiResponse<Map<String, Long>> createMember(@Valid @RequestBody MemberRequest request) {
        return ApiResponse.success(Map.of("memberId", service.createMember(request)));
    }

    @PutMapping("/api/base/members/{memberId}")
    @RequiresPermission("base:member:update")
    public ApiResponse<Void> updateMember(@PathVariable Long memberId, @Valid @RequestBody MemberRequest request) {
        service.updateMember(memberId, request);
        return ApiResponse.success();
    }

    @GetMapping("/api/base/member-bindings")
    @RequiresPermission("base:memberBinding:list")
    public ApiResponse<PageResult<MemberHouseBindingView>> pageBindings(@RequestParam(required = false) Long projectId,
                                                                        @RequestParam(required = false) Long memberId,
                                                                        @RequestParam(required = false) String realName,
                                                                        @RequestParam(required = false) String status,
                                                                        @RequestParam(defaultValue = "1") long pageNo,
                                                                        @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageBindings(projectId, memberId, realName, status, pageNo, pageSize));
    }

    @PutMapping("/api/base/member-bindings/{bindId}/audit")
    @RequiresPermission("base:memberBinding:audit")
    public ApiResponse<Void> auditBinding(@PathVariable Long bindId, @Valid @RequestBody MemberAuditRequest request) {
        service.auditBinding(bindId, request);
        return ApiResponse.success();
    }

    @PutMapping("/api/base/member-bindings/{bindId}/unbind")
    @RequiresPermission("base:memberBinding:unbind")
    public ApiResponse<Void> unbind(@PathVariable Long bindId, @RequestBody(required = false) UnbindRequest request) {
        service.unbind(bindId, request);
        return ApiResponse.success();
    }
}
