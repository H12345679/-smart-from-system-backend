package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_tank")
public class Tank {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tankCode;
    private String tankName;
    private String tankType;
    private Long facilityId;
    private BigDecimal volumeM3;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
