package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_alert_rule")
public class AlertRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private Long sensorDeviceId;
    private String operator;
    private BigDecimal thresholdValue;
    private Integer durationSeconds;
    private Integer alertLevel;
    private Long actuatorDeviceId;
    private String actuatorAction;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
