package com.edu.smartfarm.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.smartfarm.common.BusinessException;
import com.edu.smartfarm.entity.User;
import com.edu.smartfarm.mapper.UserMapper;
import com.edu.smartfarm.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        // 密码校验: 支持BCrypt格式，也兼容明文(开发阶段)
        boolean passwordMatch = false;
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")) {
            passwordMatch = BCrypt.checkpw(password, user.getPassword());
        } else {
            passwordMatch = password.equals(user.getPassword());
        }
        if (!passwordMatch) {
            throw new BusinessException(401, "密码错误");
        }
        String role = userMapper.findRoleByUserId(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role != null ? role : "TECHNICIAN");

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", role);
        return result;
    }

    public void register(String username, String password, String realName, String phone) {
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);
    }

    public Map<String, Object> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        String role = userMapper.findRoleByUserId(userId);
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("phone", user.getPhone());
        info.put("role", role);
        return info;
    }

    public List<Map<String, Object>> listAllUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        Map<String, String> roleNameMap = Map.of("ADMIN", "系统管理员", "MANAGER", "运营经理", "TECHNICIAN", "现场技术员");
        for (User user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("realName", user.getRealName());
            map.put("phone", user.getPhone());
            map.put("status", user.getStatus());
            map.put("createTime", user.getCreateTime());
            String role = userMapper.findRoleByUserId(user.getId());
            map.put("role", role);
            map.put("roleName", roleNameMap.getOrDefault(role, "未分配"));
            result.add(map);
        }
        return result;
    }

    public void updateStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void resetPassword(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword("admin123"); // 开发阶段明文存储
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        boolean match;
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")) {
            match = BCrypt.checkpw(oldPassword, user.getPassword());
        } else {
            match = oldPassword.equals(user.getPassword());
        }
        if (!match) throw new BusinessException("旧密码错误");
        user.setPassword(newPassword);
        userMapper.updateById(user);
    }
}
