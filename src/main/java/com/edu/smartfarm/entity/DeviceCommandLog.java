package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_device_command_log")
public class DeviceCommandLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private String commandType;
    private String commandPayload;
    private String triggeredBy;
    private Long ruleId;
    private String ackStatus;
    private LocalDateTime ackTime;
    private LocalDateTime createTime;
}
