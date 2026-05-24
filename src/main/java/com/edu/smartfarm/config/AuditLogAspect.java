package com.edu.smartfarm.config;

import com.edu.smartfarm.entity.AuditLog;
import com.edu.smartfarm.mapper.AuditLogMapper;
import com.edu.smartfarm.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志AOP - 自动记录所有写操作到 t_audit_log
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;

    @Pointcut("execution(* com.edu.smartfarm.controller..*.*(..)) && " +
              "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void writeOperations() {}

    @Around("writeOperations()")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return result;
            HttpServletRequest request = attrs.getRequest();

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(UserContext.getUserId());
            auditLog.setUsername(UserContext.getUsername());
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setRequestUrl(request.getRequestURI());
            auditLog.setIp(getClientIp(request));
            auditLog.setCreateTime(LocalDateTime.now());

            // 从URL推断模块和操作
            String uri = request.getRequestURI();
            if (uri.contains("/auth")) {
                auditLog.setModule("AUTH");
                auditLog.setOperation(uri.contains("login") ? "LOGIN" : "REGISTER");
            } else if (uri.contains("/batch")) {
                auditLog.setModule("BATCH");
                auditLog.setOperation(uri.contains("feed") ? "FEED" : uri.contains("mortality") ? "MORTALITY" :
                        uri.contains("medication") ? "MEDICATION" : uri.contains("harvest") ? "HARVEST" : "CREATE");
            } else if (uri.contains("/device")) {
                auditLog.setModule("DEVICE");
                auditLog.setOperation(uri.contains("command") ? "COMMAND" : uri.contains("register") ? "REGISTER" : "UPDATE");
            } else if (uri.contains("/rule")) {
                auditLog.setModule("RULE");
                auditLog.setOperation("CONFIG");
            } else if (uri.contains("/alert")) {
                auditLog.setModule("ALERT");
                auditLog.setOperation("UPDATE");
            } else {
                auditLog.setModule("OTHER");
                auditLog.setOperation(request.getMethod());
            }

            auditLog.setDescription(request.getMethod() + " " + uri);
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.warn("审计日志记录失败: {}", e.getMessage());
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
