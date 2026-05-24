package com.edu.smartfarm.scheduler;

import com.edu.smartfarm.service.SensorDataService;
import com.edu.smartfarm.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 传感器数据模拟器
 * 每10秒为每个水池生成一组模拟的DO/pH/温度/氨氮数据
 * 模拟真实传感器上报行为，用于演示和测试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataSimulator {

    private final JdbcTemplate jdbcTemplate;
    private final WebSocketServer webSocketServer;

    private final Random random = new Random();

    // 各池的基准值（模拟不同池子有不同的水质状况）
    private final double[][] tankBaselines = {
        // tankId=1~9: {DO基准, pH基准, 温度基准, 氨氮基准}
        {7.2, 7.8, 20.0, 0.20},  // 1号池 - 正常
        {6.8, 7.5, 19.8, 0.25},  // 2号池 - 正常
        {7.0, 8.0, 20.5, 0.18},  // 3号池 - 正常
        {6.5, 7.6, 21.0, 0.30},  // 4号池 - 略低
        {5.5, 7.2, 22.3, 0.55},  // 5号池 - 异常！DO低+氨氮高
        {6.9, 7.9, 19.5, 0.22},  // 6号池 - 正常
        {8.1, 7.7, 20.0, 0.10},  // 7号育苗池 - 优秀
        {9.0, 7.4, 19.0, 0.05},  // 8号过滤池
        {8.8, 7.3, 19.2, 0.06},  // 9号过滤池
    };

    /**
     * 每10秒生成一组模拟数据
     */
    @Scheduled(fixedRate = 10000)
    public void generateSensorData() {
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < tankBaselines.length; i++) {
            long tankId = i + 1;
            double[] baseline = tankBaselines[i];

            // 在基准值附近加随机波动
            double doVal = baseline[0] + (random.nextDouble() - 0.5) * 0.6;
            double phVal = baseline[1] + (random.nextDouble() - 0.5) * 0.3;
            double tempVal = baseline[2] + (random.nextDouble() - 0.5) * 0.8;
            double nh4Val = baseline[3] + (random.nextDouble() - 0.3) * 0.1;

            // 确保值在合理范围内
            doVal = Math.max(3.0, Math.min(12.0, doVal));
            phVal = Math.max(6.0, Math.min(9.0, phVal));
            tempVal = Math.max(15.0, Math.min(28.0, tempVal));
            nh4Val = Math.max(0.0, Math.min(1.5, nh4Val));

            // 写入数据库
            insertSensorLog(tankId, "DO", doVal, now);
            insertSensorLog(tankId, "PH", phVal, now);
            insertSensorLog(tankId, "TEMP", tempVal, now);
            if (tankId <= 6) { // 只有养成池有氨氮传感器
                insertSensorLog(tankId, "NH4", nh4Val, now);
            }

            // 更新设备心跳
            jdbcTemplate.update(
                "UPDATE t_iot_device SET online_status = 1, last_heartbeat = NOW() WHERE tank_id = ? AND device_type = 'SENSOR'",
                tankId);
        }

        // 通过WebSocket推送刷新信号给前端
        webSocketServer.sendToAll("{\"type\":\"refresh\",\"timestamp\":" + System.currentTimeMillis() + "}");

        log.debug("传感器模拟数据已生成: {} 个水池 x 4 参数", tankBaselines.length);
    }

    private void insertSensorLog(long tankId, String parameterType, double value, LocalDateTime recordedAt) {
        try {
            jdbcTemplate.update(
                "INSERT INTO t_water_quality_log (tank_id, device_id, parameter_type, value, recorded_at) VALUES (?, ?, ?, ?, ?)",
                tankId, getDeviceId(tankId, parameterType), parameterType,
                Math.round(value * 100.0) / 100.0, recordedAt);
        } catch (Exception e) {
            // 忽略插入失败（可能device_id不存在）
        }
    }

    private long getDeviceId(long tankId, String parameterType) {
        // 简单映射：tankId * 10 + 参数序号
        int offset = switch (parameterType) {
            case "DO" -> 1;
            case "PH" -> 2;
            case "TEMP" -> 3;
            case "NH4" -> 4;
            default -> 0;
        };
        return tankId * 10 + offset;
    }
}
