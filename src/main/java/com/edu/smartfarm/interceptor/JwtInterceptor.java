package com.edu.smartfarm.interceptor;

import com.edu.smartfarm.common.BusinessException;
import com.edu.smartfarm.utils.JwtUtil;
import com.edu.smartfarm.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或Token无效");
        }

        token = token.substring(7);
        if (jwtUtil.isTokenExpired(token)) {
            throw new BusinessException(401, "Token已过期，请重新登录");
        }

        // 将用户信息存入线程上下文
        UserContext.setUserId(jwtUtil.getUserId(token));
        UserContext.setUsername(jwtUtil.getUsername(token));
        UserContext.setRole(jwtUtil.getRole(token));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
