package com.edu.smartfarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.smartfarm.common.BusinessException;
import com.edu.smartfarm.entity.AlertRule;
import com.edu.smartfarm.mapper.AlertRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final AlertRuleMapper ruleMapper;

    public AlertRule create(String ruleName, Long sensorDeviceId, String operator,
                           BigDecimal thresholdValue, Integer durationSeconds, Integer alertLevel,
                           Long actuatorDeviceId, String actuatorAction) {
        // 冲突检测
        Map<String, Object> conflict = checkConflict(sensorDeviceId, operator, thresholdValue, actuatorDeviceId, actuatorAction, null);
        if ((Boolean) conflict.get("hasConflict")) {
            throw new BusinessException("规则冲突: " + conflict.get("message"));
        }

        AlertRule rule = new AlertRule();
        rule.setRuleName(ruleName);
        rule.setSensorDeviceId(sensorDeviceId);
        rule.setOperator(operator);
        rule.setThresholdValue(thresholdValue);
        rule.setDurationSeconds(durationSeconds != null ? durationSeconds : 0);
        rule.setAlertLevel(alertLevel);
        rule.setActuatorDeviceId(actuatorDeviceId);
        rule.setActuatorAction(actuatorAction);
        rule.setEnabled(1);
        ruleMapper.insert(rule);
        return rule;
    }

    public List<AlertRule> list(Long sensorDeviceId, Integer enabled) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        if (sensorDeviceId != null) wrapper.eq(AlertRule::getSensorDeviceId, sensorDeviceId);
        if (enabled != null) wrapper.eq(AlertRule::getEnabled, enabled);
        return ruleMapper.selectList(wrapper);
    }

    public void update(Long id, String ruleName, Long sensorDeviceId, String operator,
                       BigDecimal thresholdValue, Integer durationSeconds, Integer alertLevel,
                       Long actuatorDeviceId, String actuatorAction) {
        AlertRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new BusinessException("规则不存在");
        rule.setRuleName(ruleName);
        rule.setSensorDeviceId(sensorDeviceId);
        rule.setOperator(operator);
        rule.setThresholdValue(thresholdValue);
        rule.setDurationSeconds(durationSeconds);
        rule.setAlertLevel(alertLevel);
        rule.setActuatorDeviceId(actuatorDeviceId);
        rule.setActuatorAction(actuatorAction);
        ruleMapper.updateById(rule);
    }

    public void toggle(Long id, Integer enabled) {
        AlertRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new BusinessException("规则不存在");
        rule.setEnabled(enabled);
        ruleMapper.updateById(rule);
    }

    public void delete(Long id) {
        ruleMapper.deleteById(id);
    }

    public Map<String, Object> checkConflict(Long sensorDeviceId, String operator, BigDecimal thresholdValue,
                                              Long actuatorDeviceId, String actuatorAction, Long excludeRuleId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasConflict", false);
        result.put("message", "");

        if (actuatorDeviceId == null || actuatorAction == null) return result;

        // 查找同一执行器的已有规则
        List<AlertRule> existingRules = ruleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>()
                        .eq(AlertRule::getActuatorDeviceId, actuatorDeviceId)
                        .eq(AlertRule::getEnabled, 1)
                        .ne(excludeRuleId != null, AlertRule::getId, excludeRuleId)
        );

        for (AlertRule existing : existingRules) {
            // 同一执行器存在相反动作则判定冲突
            if (!actuatorAction.equals(existing.getActuatorAction())
                    && existing.getSensorDeviceId().equals(sensorDeviceId)) {
                // 检查阈值区间是否重叠
                boolean overlaps = isThresholdOverlap(operator, thresholdValue,
                        existing.getOperator(), existing.getThresholdValue());
                if (overlaps) {
                    result.put("hasConflict", true);
                    result.put("message", "与规则[" + existing.getRuleName() + "]在同一传感器/阈值区间内存在" + existing.getActuatorAction() + "冲突");
                    return result;
                }
            }
        }
        return result;
    }

    private boolean isThresholdOverlap(String op1, BigDecimal val1, String op2, BigDecimal val2) {
        // 简化的重叠判定: 如果一个是 < X 另一个也是 < Y 或 > Y
        // 当同方向且数值接近时视为重叠
        if ((op1.contains("<") && op2.contains("<")) || (op1.contains(">") && op2.contains(">"))) {
            return val1.subtract(val2).abs().compareTo(BigDecimal.valueOf(2)) <= 0;
        }
        // 反方向的运算符 (一个<，一个>) 如果区间有交叉
        if (op1.contains("<") && op2.contains(">")) {
            return val1.compareTo(val2) > 0; // < 6 和 > 5 有交叉
        }
        if (op1.contains(">") && op2.contains("<")) {
            return val2.compareTo(val1) > 0;
        }
        return false;
    }
}
