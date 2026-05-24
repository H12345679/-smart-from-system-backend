package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_iot_device")
public class IotDevice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String deviceName;
    private String macAddress;
    private String deviceType;
    private String parameterType;
    private Long tankId;
    private String mqttTopic;
    private Integer onlineStatus;
    private LocalDateTime lastHeartbeat;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
