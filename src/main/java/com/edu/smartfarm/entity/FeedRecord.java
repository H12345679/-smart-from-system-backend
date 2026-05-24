package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_feed_record")
public class FeedRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private BigDecimal feedWeightKg;
    private String feedType;
    private Long operatorId;
    private LocalDateTime feedTime;
    private String remark;
}
