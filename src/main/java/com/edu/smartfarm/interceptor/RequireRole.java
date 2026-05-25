package com.edu.smartfarm.interceptor;

import java.lang.annotation.*;

/**
 * 接口权限注解 — 标记在Controller方法上，限制允许访问的角色
 * 示例: @RequireRole({"ADMIN", "MANAGER"})
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value();
}
