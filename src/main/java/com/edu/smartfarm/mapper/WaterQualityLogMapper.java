package com.edu.smartfarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WaterQualityLogMapper extends BaseMapper<Object> {

    @Select("SELECT DATE_FORMAT(recorded_at, '%H:%i') as time, value " +
            "FROM t_water_quality_log " +
            "WHERE tank_id = #{tankId} AND parameter_type = #{parameterType} " +
            "ORDER BY recorded_at ASC LIMIT 100")
    List<Map<String, Object>> findHistoryByTankAndType(@Param("tankId") Long tankId, @Param("parameterType") String parameterType);

    @Select("SELECT wl.tank_id, wl.parameter_type, wl.value, wl.recorded_at " +
            "FROM t_water_quality_log wl " +
            "INNER JOIN (SELECT tank_id, parameter_type, MAX(recorded_at) as max_time " +
            "FROM t_water_quality_log GROUP BY tank_id, parameter_type) latest " +
            "ON wl.tank_id = latest.tank_id AND wl.parameter_type = latest.parameter_type AND wl.recorded_at = latest.max_time")
    List<Map<String, Object>> findLatestByAllTanks();
}
