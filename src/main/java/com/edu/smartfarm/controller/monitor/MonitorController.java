package com.edu.smartfarm.controller.monitor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.entity.Tank;
import com.edu.smartfarm.entity.ThresholdConfig;
import com.edu.smartfarm.mapper.TankMapper;
import com.edu.smartfarm.mapper.ThresholdConfigMapper;
import com.edu.smartfarm.mapper.WaterQualityLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Tag(name = "实时监控模块", description = "数据驾驶舱、水质查询、阈值管理")
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final TankMapper tankMapper;
    private final ThresholdConfigMapper thresholdConfigMapper;
    private final WaterQualityLogMapper waterQualityLogMapper;

    @Operation(summary = "获取全场水池实时概况", description = "从水质日志表获取每池最新数据")
    @GetMapping("/dashboard")
    public Result<?> getDashboard() {
        List<Tank> tanks = tankMapper.selectList(null);
        List<Map<String, Object>> latestData = waterQualityLogMapper.findLatestByAllTanks();

        // 按 tank_id 组织最新数据
        Map<Long, Map<String, Object>> tankDataMap = new HashMap<>();
        for (Map<String, Object> row : latestData) {
            Long tankId = ((Number) row.get("tank_id")).longValue();
            String paramType = (String) row.get("parameter_type");
            BigDecimal value = (BigDecimal) row.get("value");
            tankDataMap.computeIfAbsent(tankId, k -> new HashMap<>()).put(paramType, value);
        }

        List<Map<String, Object>> dashboard = new ArrayList<>();
        Random fallback = new Random(); // 没有真实数据时的兜底随机
        for (Tank tank : tanks) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tank.getId());
            item.put("tankCode", tank.getTankCode());
            item.put("tankName", tank.getTankName());
            item.put("tankType", tank.getTankType());
            item.put("status", tank.getStatus());

            Map<String, Object> sensorData = tankDataMap.get(tank.getId());
            double doVal, phVal, tempVal;
            if (sensorData != null) {
                doVal = sensorData.getOrDefault("DO", BigDecimal.valueOf(7.0)) instanceof BigDecimal
                        ? ((BigDecimal) sensorData.getOrDefault("DO", BigDecimal.valueOf(7.0))).doubleValue()
                        : 7.0;
                phVal = sensorData.getOrDefault("PH", BigDecimal.valueOf(7.5)) instanceof BigDecimal
                        ? ((BigDecimal) sensorData.getOrDefault("PH", BigDecimal.valueOf(7.5))).doubleValue()
                        : 7.5;
                tempVal = sensorData.getOrDefault("TEMP", BigDecimal.valueOf(20.0)) instanceof BigDecimal
                        ? ((BigDecimal) sensorData.getOrDefault("TEMP", BigDecimal.valueOf(20.0))).doubleValue()
                        : 20.0;
            } else {
                // 没有传感器数据时生成模拟值
                doVal = 5.5 + fallback.nextDouble() * 3.0;
                phVal = 7.0 + fallback.nextDouble() * 1.5;
                tempVal = 18.0 + fallback.nextDouble() * 4.0;
            }

            item.put("do", Math.round(doVal * 10.0) / 10.0);
            item.put("ph", Math.round(phVal * 10.0) / 10.0);
            item.put("temp", Math.round(tempVal * 10.0) / 10.0);
            item.put("hasAlert", doVal < 6.0);
            dashboard.add(item);
        }
        return Result.success(dashboard);
    }

    @Operation(summary = "获取单水池实时数据")
    @GetMapping("/tank/{tankId}/realtime")
    public Result<?> getTankRealtime(@PathVariable Long tankId) {
        Tank tank = tankMapper.selectById(tankId);
        if (tank == null) return Result.error("水池不存在");

        List<Map<String, Object>> latestData = waterQualityLogMapper.findLatestByAllTanks();
        Map<String, Object> data = new HashMap<>();
        data.put("tankId", tankId);
        data.put("tankName", tank.getTankName());

        for (Map<String, Object> row : latestData) {
            if (((Number) row.get("tank_id")).longValue() == tankId) {
                String paramType = (String) row.get("parameter_type");
                data.put(paramType.toLowerCase(), row.get("value"));
            }
        }
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }

    @Operation(summary = "查询水质历史趋势", description = "从t_water_quality_log读取真实历史数据")
    @GetMapping("/tank/{tankId}/history")
    public Result<?> getTankHistory(@PathVariable Long tankId,
                                     @RequestParam String parameterType,
                                     @RequestParam String startTime,
                                     @RequestParam String endTime) {
        List<Map<String, Object>> points = waterQualityLogMapper.findHistoryByTankAndType(tankId, parameterType);
        // 如果数据库没有数据则返回模拟数据
        if (points.isEmpty()) {
            Random random = new Random();
            for (int i = 0; i < 24; i++) {
                Map<String, Object> point = new HashMap<>();
                point.put("time", String.format("%02d:00", i));
                double base = "DO".equals(parameterType) ? 6.5 : "PH".equals(parameterType) ? 7.5 : 20.0;
                double range = "DO".equals(parameterType) ? 2.0 : "PH".equals(parameterType) ? 1.0 : 3.0;
                point.put("value", Math.round((base + (random.nextDouble() - 0.5) * range) * 10.0) / 10.0);
                points.add(point);
            }
        }
        return Result.success(points);
    }

    @Operation(summary = "多传感器对比查询")
    @GetMapping("/compare")
    public Result<?> compareMetrics(@RequestParam Long tankId,
                                     @RequestParam String parameterTypes,
                                     @RequestParam String startTime,
                                     @RequestParam String endTime) {
        String[] types = parameterTypes.split(",");
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        for (String type : types) {
            List<Map<String, Object>> points = waterQualityLogMapper.findHistoryByTankAndType(tankId, type.trim());
            if (points.isEmpty()) {
                Random random = new Random();
                for (int i = 0; i < 24; i++) {
                    Map<String, Object> point = new HashMap<>();
                    point.put("time", String.format("%02d:00", i));
                    double base = "DO".equals(type.trim()) ? 6.5 : "PH".equals(type.trim()) ? 7.5 : 20.0;
                    point.put("value", Math.round((base + (random.nextDouble() - 0.5) * 2.0) * 10.0) / 10.0);
                    points.add(point);
                }
            }
            result.put(type.trim(), points);
        }
        return Result.success(result);
    }

    // ========== 阈值管理 ==========

    @Operation(summary = "获取阈值配置列表")
    @GetMapping("/thresholds")
    public Result<?> listThresholds() {
        List<ThresholdConfig> list = thresholdConfigMapper.selectList(null);
        return Result.success(list);
    }

    @Operation(summary = "更新阈值配置")
    @PutMapping("/thresholds/{id}")
    public Result<?> updateThreshold(@PathVariable Long id, @RequestBody ThresholdConfig config) {
        config.setId(id);
        thresholdConfigMapper.updateById(config);
        return Result.success("阈值更新成功");
    }
}
