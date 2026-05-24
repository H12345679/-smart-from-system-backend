package com.edu.smartfarm.controller.monitor;

import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.service.AlertService;
import com.edu.smartfarm.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "报警管理模块", description = "报警历史、确认、升级")
@RestController
@RequestMapping("/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "查询报警历史列表")
    @GetMapping("/list")
    public Result<?> listAlerts(@RequestParam(required = false) Integer alertLevel,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) Long tankId,
                                 @RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(alertService.list(alertLevel, status, tankId, page, size));
    }

    @Operation(summary = "获取当前活跃报警数量")
    @GetMapping("/active-count")
    public Result<?> getActiveAlertCount() {
        return Result.success(alertService.getActiveCount());
    }

    @Operation(summary = "确认报警")
    @PutMapping("/{id}/acknowledge")
    public Result<?> acknowledgeAlert(@PathVariable Long id) {
        alertService.acknowledge(id, UserContext.getUserId());
        return Result.success("已确认");
    }

    @Operation(summary = "解除报警")
    @PutMapping("/{id}/resolve")
    public Result<?> resolveAlert(@PathVariable Long id) {
        alertService.resolve(id);
        return Result.success("已解除");
    }

    @Operation(summary = "手动升级报警")
    @PutMapping("/{id}/escalate")
    public Result<?> escalateAlert(@PathVariable Long id) {
        alertService.escalate(id, null);
        return Result.success("已升级");
    }

    @Operation(summary = "报警统计", description = "按级别统计近7天报警数量")
    @GetMapping("/statistics")
    public Result<?> getAlertStatistics() {
        return Result.success(alertService.getStatistics());
    }
}
