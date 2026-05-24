package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_medication_record")
public class MedicationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private String drugName;
    private String dosage;
    private Integer withdrawalDays;
    private LocalDate withdrawalEndDate;
    private Long operatorId;
    private LocalDateTime medicationTime;
    private String remark;
}
