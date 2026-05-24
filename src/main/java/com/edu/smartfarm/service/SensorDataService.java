package com.edu.smartfarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.smartfarm.entity.AlertHistory;
import com.edu.smartfarm.entity.AlertRule;
import com.edu.smartfarm.entity.IotDevice;
import com.edu.smartfarm.mapper.AlertHistoryMapper;
import com.edu.smartfarm.mapper.AlertRuleMapper;
import com.edu.smartfarm.mapper.IotDeviceMapper;
import com.edu.smartfarm.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 传感器数据处理服务
 * 职责: 写入水质日志 → 规则引擎判定 → 触发报警 → WebSocket推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataService {

    private final JdbcTemplate jdbcTemplate;
    private final AlertRuleMapper alertRuleMapper;
    private final AlertHistoryMapper alertHistoryMapper;
    private final IotDeviceMapper iotDeviceMapper;
    private final WebSocketServer webSocketServer;

    /**
     * 处理传感器上报的数据
     */
    public void processSensorData(Long tankId, String parameterType, double value, long timestamp) {
        // 1. 写入水质日志表
        LocalDateTime recordedAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
        jdbcTemplate.update(
                "INSERT INTO t_water_quality_log (tank_id, device_id, parameter_type, value, recorded_at) VALUES (?, ?, ?, ?, ?)",
                tankId, findDeviceId(tankId, parameterType), parameterType, value, recordedAt);

        // 2. 更新设备心跳
        updateDeviceHeartbeat(tankId, parameterType);

        // 3. 规则引擎判定
        evaluateRules(tankId, parameterType, value);

        // 4. WebSocket推送实时数据到前端
        webSocketServer.sendToAll("{\"type\":\"sensor\",\"tankId\":" + tankId +
                ",\"param\":\"" + parameterType + "\",\"value\":" + value + "}");
    }

    /**
     * 规则引擎 - 判定是否触发报警
     */
    private void evaluateRules(Long tankId, String parameterType, double value) {
        // 查找该水池该参数相关的启用规则
        List<IotDevice> sensors = iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>()
                        .eq(IotDevice::getTankId, tankId)
                        .eq(IotDevice::getParameterType, parameterType)
                        .eq(IotDevice::getDeviceType, "SENSOR"));

        for (IotDevice sensor : sensors) {
            List<AlertRule> rules = alertRuleMapper.selectList(
                    new LambdaQueryWrapper<AlertRule>()
                            .eq(AlertRule::getSensorDeviceId, sensor.getId())
                            .eq(AlertRule::getEnabled, 1));

            for (AlertRule rule : rules) {
                boolean triggered = evaluateCondition(value, rule.getOperator(), rule.getThresholdValue().doubleValue());
                if (triggered) {
                    triggerAlert(rule, tankId, sensor.getId(), parameterType, value);
                }
            }
        }
    }

    private boolean evaluateCondition(double value, String operator, double threshold) {
        return switch (operator) {
            case "<" -> value < threshold;
            case ">" -> value > threshold;
            case "<=" -> value <= threshold;
            case ">=" -> value >= threshold;
            case "==" -> Math.abs(value - threshold) < 0.01;
            default -> false;
        };
    }

    private void triggerAlert(AlertRule rule, Long tankId, Long deviceId, String parameterType, double value) {
        // 防止重复报警（同一规则5分钟内只报一次）
        Long existingCount = alertHistoryMapper.selectCount(
                new LambdaQueryWrapper<AlertHistory>()
                        .eq(AlertHistory::getRuleId, rule.getId())
                        .eq(AlertHistory::getStatus, "ACTIVE"));
        if (existingCount > 0) return;

        AlertHistory alert = new AlertHistory();
        alert.setRuleId(rule.getId());
        alert.setTankId(tankId);
        alert.setDeviceId(deviceId);
        alert.setAlertLevel(rule.getAlertLevel());
        alert.setParameterType(parameterType);
        alert.setCurrentValue(BigDecimal.valueOf(value));
        alert.setThresholdValue(rule.getThresholdValue());
        alert.setMessage(String.format("%s异常: 当前值%.2f, 阈值%s%.2f",
                parameterType, value, rule.getOperator(), rule.getThresholdValue().doubleValue()));
        alert.setStatus("ACTIVE");
        alert.setCreateTime(LocalDateTime.now());
        alertHistoryMapper.insert(alert);

        log.warn("触发{}级报警: 水池{} {} = {} (阈值{}{})",
                rule.getAlertLevel(), tankId, parameterType, value, rule.getOperator(), rule.getThresholdValue());

        // WebSocket推送报警
        webSocketServer.sendToAll("{\"type\":\"alert\",\"level\":" + rule.getAlertLevel() +
                ",\"tankId\":" + tankId + ",\"message\":\"" + alert.getMessage() + "\"}");
    }

    private Long findDeviceId(Long tankId, String parameterType) {
        IotDevice device = iotDeviceMapper.selectOne(
                new LambdaQueryWrapper<IotDevice>()
                        .eq(IotDevice::getTankId, tankId)
                        .eq(IotDevice::getParameterType, parameterType)
                        .last("LIMIT 1"));
        return device != null ? device.getId() : 0L;
    }

    private void updateDeviceHeartbeat(Long tankId, String parameterType) {
        jdbcTemplate.update(
                "UPDATE t_iot_device SET online_status = 1, last_heartbeat = NOW() WHERE tank_id = ? AND parameter_type = ?",
                tankId, parameterType);
    }
}
