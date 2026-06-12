package com.yongquan.propertysaas.security.permission;

import com.yongquan.propertysaas.security.domain.AuthPrincipal;
import com.yongquan.propertysaas.security.domain.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequiresPermission permission = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (permission == null) {
            permission = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        }
        if (permission == null) {
            return true;
        }
        CurrentUser currentUser = currentUser();
        if (permission.platformOnly() && !"PLATFORM".equals(currentUser.userType())) {
            throw new AccessDeniedException("仅平台用户可访问");
        }
        if (!currentUser.permissions().contains(permission.value())) {
            throw new AccessDeniedException("缺少权限：" + permission.value());
        }
        return true;
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AuthPrincipal authPrincipal) {
            return authPrincipal.currentUser();
        }
        throw new AccessDeniedException("未登录");
    }
}
