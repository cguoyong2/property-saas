package com.yongquan.propertysaas.security.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.security.domain.AuthPrincipal;
import com.yongquan.propertysaas.security.domain.CurrentUser;
import com.yongquan.propertysaas.security.domain.LoginResult;
import com.yongquan.propertysaas.security.domain.MenuPermission;
import com.yongquan.propertysaas.security.dto.LoginRequest;
import com.yongquan.propertysaas.security.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, servletRequest));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUser> me(Authentication authentication) {
        return ApiResponse.success(((AuthPrincipal) authentication).currentUser());
    }

    @GetMapping("/menus")
    public ApiResponse<List<MenuPermission>> menus(Authentication authentication) {
        CurrentUser currentUser = ((AuthPrincipal) authentication).currentUser();
        return ApiResponse.success(authService.getMenus(currentUser.userId()));
    }
}
