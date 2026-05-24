package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_breeding_batch")
public class BreedingBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchId;
    private Long tankId;
    private String speciesName;
    private Integer initialCount;
    private Integer currentCount;
    private BigDecimal initialAvgWeight;
    private BigDecimal totalFeedKg;
    private BigDecimal harvestWeightKg;
    private BigDecimal fcr;
    private String supplier;
    private String quarantineCert;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
