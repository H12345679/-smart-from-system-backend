package com.edu.smartfarm.scheduler;

import com.edu.smartfarm.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务: 设备心跳检测
 * 超过90秒未收到数据的设备标记为离线
 * 每60秒执行一次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceHeartbeatTask {

    private final JdbcTemplate jdbcTemplate;
    private final WebSocketServer webSocketServer;

    @Scheduled(fixedRate = 60000)
    public void checkHeartbeat() {
        // 将90秒无心跳的设备标记为离线
        int affected = jdbcTemplate.update(
                "UPDATE t_iot_device SET online_status = 0 " +
                "WHERE online_status = 1 AND last_heartbeat < DATE_SUB(NOW(), INTERVAL 90 SECOND)");

        if (affected > 0) {
            log.warn("{}台设备因心跳超时被标记为离线", affected);
            webSocketServer.sendToAll("{\"type\":\"device_offline\",\"count\":" + affected +
                    ",\"message\":\"" + affected + "台设备已离线\"}");
        }
    }
}
