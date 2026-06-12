package com.yongquan.propertysaas.security.web;

import com.yongquan.propertysaas.security.domain.AuthPrincipal;
import com.yongquan.propertysaas.security.domain.CurrentUser;
import com.yongquan.propertysaas.security.service.AuthService;
import com.yongquan.propertysaas.security.service.JwtService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthService authService;

    public JwtAuthenticationFilter(JwtService jwtService, AuthService authService) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveBearerToken(request);
            if (token != null) {
                CurrentUser tokenUser = jwtService.parseToken(token);
                CurrentUser currentUser = "MEMBER".equals(tokenUser.userType())
                        ? tokenUser
                        : authService.loadCurrentUser(tokenUser.userId());
                TenantContext.set(currentUser.userId(), currentUser.tenantId(), resolveProjectId(request), currentUser.userType());
                AuthPrincipal authentication = new AuthPrincipal(
                        currentUser,
                        currentUser.permissions().stream().map(SimpleGrantedAuthority::new).toList()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    private Long resolveProjectId(HttpServletRequest request) {
        String projectId = request.getHeader("X-Project-Id");
        if (projectId == null || projectId.isBlank()) {
            return null;
        }
        return Long.valueOf(projectId);
    }
}
