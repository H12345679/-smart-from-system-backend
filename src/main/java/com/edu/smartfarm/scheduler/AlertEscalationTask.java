package com.edu.smartfarm.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.smartfarm.entity.AlertHistory;
import com.edu.smartfarm.mapper.AlertHistoryMapper;
import com.edu.smartfarm.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务: 报警5分钟未响应自动升级
 * 每30秒检查一次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEscalationTask {

    private final AlertHistoryMapper alertHistoryMapper;
    private final WebSocketServer webSocketServer;

    @Scheduled(fixedRate = 30000)
    public void checkEscalation() {
        // 查找5分钟前触发且仍处于ACTIVE状态的3级报警
        LocalDateTime fiveMinAgo = LocalDateTime.now().minusMinutes(5);
        List<AlertHistory> overdueAlerts = alertHistoryMapper.selectList(
                new LambdaQueryWrapper<AlertHistory>()
                        .eq(AlertHistory::getStatus, "ACTIVE")
                        .eq(AlertHistory::getAlertLevel, 3)
                        .le(AlertHistory::getCreateTime, fiveMinAgo));

        for (AlertHistory alert : overdueAlerts) {
            alert.setStatus("ESCALATED");
            alert.setEscalatedTime(LocalDateTime.now());
            alertHistoryMapper.updateById(alert);
            log.warn("报警自动升级: id={}, 水池={}, 已超时5分钟无人响应", alert.getId(), alert.getTankId());

            webSocketServer.sendToAll("{\"type\":\"escalation\",\"alertId\":" + alert.getId() +
                    ",\"message\":\"3级报警已超时升级，请立即处理！\"}");
        }
    }
}
