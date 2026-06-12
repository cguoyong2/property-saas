package com.yongquan.propertysaas.security.service;

import com.yongquan.propertysaas.security.domain.CurrentUser;
import com.yongquan.propertysaas.security.domain.LoginResult;
import com.yongquan.propertysaas.security.domain.MenuPermission;
import com.yongquan.propertysaas.security.domain.SysUserAccount;
import com.yongquan.propertysaas.security.dto.LoginRequest;
import com.yongquan.propertysaas.security.repository.AuthRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest servletRequest) {
        SysUserAccount user = authRepository.findActiveUserByUsername(request.username())
                .orElseThrow(() -> {
                    authRepository.insertLoginLog(null, null, request.username(), null,
                            "FAILED", "USER_NOT_FOUND", clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
                    return new BadCredentialsException("账号或密码错误");
                });

        if (!"ACTIVE".equals(user.status())) {
            authRepository.insertLoginLog(user.tenantId(), user.userId(), user.username(), user.userType(),
                    "FAILED", "USER_DISABLED", clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
            throw new BadCredentialsException("账号已停用");
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            authRepository.insertLoginLog(user.tenantId(), user.userId(), user.username(), user.userType(),
                    "FAILED", "BAD_PASSWORD", clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
            throw new BadCredentialsException("账号或密码错误");
        }

        CurrentUser currentUser = toCurrentUser(user);
        String token = jwtService.createToken(currentUser);
        authRepository.updateLastLoginAt(user.userId());
        authRepository.insertLoginLog(user.tenantId(), user.userId(), user.username(), user.userType(),
                "SUCCESS", null, clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
        return new LoginResult(token, user.userId(), user.tenantId(), user.userType(), currentUser.permissions());
    }

    public CurrentUser loadCurrentUser(Long userId) {
        SysUserAccount user = authRepository.findActiveUserById(userId)
                .orElseThrow(() -> new BadCredentialsException("用户不存在"));
        if (!"ACTIVE".equals(user.status())) {
            throw new BadCredentialsException("账号已停用");
        }
        return toCurrentUser(user);
    }

    public List<MenuPermission> getMenus(Long userId) {
        return authRepository.findMenusByUserId(userId);
    }

    private CurrentUser toCurrentUser(SysUserAccount user) {
        List<String> permissions = authRepository.findMenusByUserId(user.userId()).stream()
                .map(MenuPermission::permissionCode)
                .filter(permission -> permission != null && !permission.isBlank())
                .distinct()
                .toList();
        return new CurrentUser(
                user.userId(),
                user.tenantId(),
                user.username(),
                user.realName(),
                user.userType(),
                user.status(),
                permissions
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
