package com.edu.smartfarm.interceptor;

import com.edu.smartfarm.common.BusinessException;
import com.edu.smartfarm.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 角色权限拦截器 — 配合 @RequireRole 注解使用
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;

        HandlerMethod method = (HandlerMethod) handler;
        RequireRole annotation = method.getMethodAnnotation(RequireRole.class);
        if (annotation == null) return true; // 无注解则放行

        String currentRole = UserContext.getRole();
        if (currentRole == null) {
            throw new BusinessException(401, "未登录");
        }

        String[] allowedRoles = annotation.value();
        boolean hasPermission = Arrays.asList(allowedRoles).contains(currentRole);
        if (!hasPermission) {
            throw new BusinessException(403, "权限不足，当前角色[" + currentRole + "]无法访问此接口");
        }
        return true;
    }
}
