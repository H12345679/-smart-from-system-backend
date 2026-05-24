package com.edu.smartfarm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.entity.AuditLog;
import com.edu.smartfarm.mapper.AuditLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "审计日志", description = "操作日志查询")
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogMapper auditLogMapper;

    @Operation(summary = "查询审计日志列表")
    @GetMapping("/list")
    public Result<?> listAuditLogs(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "15") Integer size,
                                    @RequestParam(required = false) String module,
                                    @RequestParam(required = false) String username) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isEmpty()) wrapper.eq(AuditLog::getModule, module);
        if (username != null && !username.isEmpty()) wrapper.like(AuditLog::getUsername, username);
        wrapper.orderByDesc(AuditLog::getCreateTime);
        Page<AuditLog> result = auditLogMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(result);
    }
}
