package com.edu.smartfarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.smartfarm.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
