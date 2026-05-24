package com.edu.smartfarm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String operation;
    private String module;
    private String description;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String ip;
    private LocalDateTime createTime;
}
