package com.edu.smartfarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.smartfarm.common.BusinessException;
import com.edu.smartfarm.entity.AlertHistory;
import com.edu.smartfarm.mapper.AlertHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertHistoryMapper alertMapper;

    public Page<AlertHistory> list(Integer alertLevel, String status, Long tankId, Integer page, Integer size) {
        LambdaQueryWrapper<AlertHistory> wrapper = new LambdaQueryWrapper<>();
        if (alertLevel != null) wrapper.eq(AlertHistory::getAlertLevel, alertLevel);
        if (status != null && !status.isEmpty()) wrapper.eq(AlertHistory::getStatus, status);
        if (tankId != null) wrapper.eq(AlertHistory::getTankId, tankId);
        wrapper.orderByDesc(AlertHistory::getCreateTime);
        return alertMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public long getActiveCount() {
        return alertMapper.selectCount(new LambdaQueryWrapper<AlertHistory>().eq(AlertHistory::getStatus, "ACTIVE"));
    }

    public void acknowledge(Long id, Long userId) {
        AlertHistory alert = alertMapper.selectById(id);
        if (alert == null) throw new BusinessException("报警记录不存在");
        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedTime(LocalDateTime.now());
        alertMapper.updateById(alert);
    }

    public void resolve(Long id) {
        AlertHistory alert = alertMapper.selectById(id);
        if (alert == null) throw new BusinessException("报警记录不存在");
        alert.setStatus("RESOLVED");
        alert.setResolvedTime(LocalDateTime.now());
        alertMapper.updateById(alert);
    }

    public void escalate(Long id, Long escalateTo) {
        AlertHistory alert = alertMapper.selectById(id);
        if (alert == null) throw new BusinessException("报警记录不存在");
        alert.setStatus("ESCALATED");
        alert.setEscalatedTo(escalateTo);
        alert.setEscalatedTime(LocalDateTime.now());
        alertMapper.updateById(alert);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("level1Active", alertMapper.selectCount(new LambdaQueryWrapper<AlertHistory>().eq(AlertHistory::getAlertLevel, 1).eq(AlertHistory::getStatus, "ACTIVE")));
        stats.put("level2Active", alertMapper.selectCount(new LambdaQueryWrapper<AlertHistory>().eq(AlertHistory::getAlertLevel, 2).eq(AlertHistory::getStatus, "ACTIVE")));
        stats.put("level3Active", alertMapper.selectCount(new LambdaQueryWrapper<AlertHistory>().eq(AlertHistory::getAlertLevel, 3).eq(AlertHistory::getStatus, "ACTIVE")));
        stats.put("totalResolved", alertMapper.selectCount(new LambdaQueryWrapper<AlertHistory>().eq(AlertHistory::getStatus, "RESOLVED")));
        stats.put("totalEscalated", alertMapper.selectCount(new LambdaQueryWrapper<AlertHistory>().eq(AlertHistory::getStatus, "ESCALATED")));
        return stats;
    }
}
