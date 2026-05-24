package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_alert_history")
public class AlertHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private Long tankId;
    private Long deviceId;
    private Integer alertLevel;
    private String parameterType;
    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private String message;
    private String status;
    private Long acknowledgedBy;
    private LocalDateTime acknowledgedTime;
    private LocalDateTime resolvedTime;
    private Long escalatedTo;
    private LocalDateTime escalatedTime;
    private LocalDateTime createTime;
}
