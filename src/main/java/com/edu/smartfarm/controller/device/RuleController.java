package com.edu.smartfarm.controller.device;

import com.edu.smartfarm.interceptor.RequireRole;
import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.entity.AlertRule;
import com.edu.smartfarm.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "规则引擎模块", description = "自动化规则配置(IF-THEN)、冲突检测")
@RestController
@RequestMapping("/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @Data
    public static class CreateRuleRequest {
        private String ruleName;
        private Long sensorDeviceId;
        private String operator;
        private BigDecimal thresholdValue;
        private Integer durationSeconds;
        private Integer alertLevel;
        private Long actuatorDeviceId;
        private String actuatorAction;
    }

    @Operation(summary = "创建自动化规则")
    @PostMapping("/create")
    @RequireRole({"ADMIN"})
    public Result<?> createRule(@RequestBody CreateRuleRequest request) {
        AlertRule rule = ruleService.create(request.getRuleName(), request.getSensorDeviceId(),
                request.getOperator(), request.getThresholdValue(), request.getDurationSeconds(),
                request.getAlertLevel(), request.getActuatorDeviceId(), request.getActuatorAction());
        return Result.success(rule);
    }

    @Operation(summary = "查询规则列表")
    @GetMapping("/list")
    public Result<?> listRules(@RequestParam(required = false) Long sensorDeviceId,
                                @RequestParam(required = false) Integer enabled) {
        return Result.success(ruleService.list(sensorDeviceId, enabled));
    }

    @Operation(summary = "更新规则")
    @PutMapping("/{id}")
    public Result<?> updateRule(@PathVariable Long id, @RequestBody CreateRuleRequest request) {
        ruleService.update(id, request.getRuleName(), request.getSensorDeviceId(),
                request.getOperator(), request.getThresholdValue(), request.getDurationSeconds(),
                request.getAlertLevel(), request.getActuatorDeviceId(), request.getActuatorAction());
        return Result.success("更新成功");
    }

    @Operation(summary = "启用/禁用规则")
    @PutMapping("/{id}/toggle")
    public Result<?> toggleRule(@PathVariable Long id, @RequestParam Integer enabled) {
        ruleService.toggle(id, enabled);
        return Result.success();
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<?> deleteRule(@PathVariable Long id) {
        ruleService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "冲突检测")
    @PostMapping("/conflict-check")
    public Result<?> checkConflict(@RequestBody CreateRuleRequest request) {
        Map<String, Object> result = ruleService.checkConflict(request.getSensorDeviceId(),
                request.getOperator(), request.getThresholdValue(),
                request.getActuatorDeviceId(), request.getActuatorAction(), null);
        return Result.success(result);
    }
}
