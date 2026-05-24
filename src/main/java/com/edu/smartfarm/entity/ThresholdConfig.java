package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_threshold_config")
public class ThresholdConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String parameterType;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal criticalMin;
    private BigDecimal criticalMax;
    private String unit;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
