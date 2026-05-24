package com.edu.smartfarm.controller.auth;

import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.service.AuthService;
import com.edu.smartfarm.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "认证模块", description = "登录、注册、验证码")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final StringRedisTemplate redisTemplate;

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
        private String phone;
    }

    @Operation(summary = "用户登录", description = "返回JWT Token")
    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        Map<String, Object> result = authService.login(request.getUsername(), request.getPassword());
        return Result.success(result);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest request) {
        authService.register(request.getUsername(), request.getPassword(), request.getRealName(), request.getPhone());
        return Result.success("注册成功");
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/userinfo")
    public Result<?> getUserInfo() {
        Long userId = UserContext.getUserId();
        return Result.success(authService.getUserInfo(userId));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/change-password")
    public Result<?> changePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.error("请输入旧密码和新密码");
        }
        authService.changePassword(UserContext.getUserId(), oldPassword, newPassword);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "获取图形验证码", description = "返回captchaKey + 4位数字验证码(开发模式直接返回明文)")
    @GetMapping("/captcha")
    public Result<?> getCaptcha() {
        String key = "captcha:" + UUID.randomUUID().toString().substring(0, 8);
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 1000));
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("code", code);
        return Result.success(result);
    }

    @Operation(summary = "获取用户列表(管理员)")
    @GetMapping("/users")
    public Result<?> listUsers() {
        return Result.success(authService.listAllUsers());
    }

    @Operation(summary = "更新用户状态(启用/禁用)")
    @PutMapping("/users/{id}/status")
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        authService.updateStatus(id, body.get("status"));
        return Result.success("状态已更新");
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/users/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id) {
        authService.resetPassword(id);
        return Result.success("密码已重置为admin123");
    }
}
