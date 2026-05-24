package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_mortality_record")
public class MortalityRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Integer deathCount;
    private String deathCause;
    private Long operatorId;
    private LocalDateTime recordTime;
    private String remark;
}
