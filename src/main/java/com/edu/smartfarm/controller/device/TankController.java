package com.edu.smartfarm.controller.device;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.entity.Tank;
import com.edu.smartfarm.mapper.TankMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "水池管理", description = "水池CRUD")
@RestController
@RequestMapping("/tank")
@RequiredArgsConstructor
public class TankController {

    private final TankMapper tankMapper;

    @Data
    public static class CreateTankRequest {
        private String tankCode;
        private String tankName;
        private String tankType; // BREEDING / FILTER / NURSERY
        private Long facilityId;
        private BigDecimal volumeM3;
    }

    @Operation(summary = "获取水池列表")
    @GetMapping("/list")
    public Result<?> listTanks(@RequestParam(required = false) String tankType,
                                @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Tank> wrapper = new LambdaQueryWrapper<>();
        if (tankType != null && !tankType.isEmpty()) wrapper.eq(Tank::getTankType, tankType);
        if (status != null && !status.isEmpty()) wrapper.eq(Tank::getStatus, status);
        wrapper.orderByAsc(Tank::getId);
        List<Tank> list = tankMapper.selectList(wrapper);
        return Result.success(list);
    }

    @Operation(summary = "新增水池")
    @PostMapping("/create")
    public Result<?> createTank(@RequestBody CreateTankRequest request) {
        // 检查编号唯一
        Tank exist = tankMapper.selectOne(new LambdaQueryWrapper<Tank>().eq(Tank::getTankCode, request.getTankCode()));
        if (exist != null) return Result.error("水池编号已存在");

        Tank tank = new Tank();
        tank.setTankCode(request.getTankCode());
        tank.setTankName(request.getTankName());
        tank.setTankType(request.getTankType() != null ? request.getTankType() : "BREEDING");
        tank.setFacilityId(request.getFacilityId() != null ? request.getFacilityId() : 1L);
        tank.setVolumeM3(request.getVolumeM3());
        tank.setStatus("IDLE");
        tankMapper.insert(tank);
        return Result.success(tank);
    }

    @Operation(summary = "更新水池状态")
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Tank tank = tankMapper.selectById(id);
        if (tank == null) return Result.error("水池不存在");
        tank.setStatus(status);
        tankMapper.updateById(tank);
        return Result.success("状态已更新");
    }

    @Operation(summary = "删除水池")
    @DeleteMapping("/{id}")
    public Result<?> deleteTank(@PathVariable Long id) {
        tankMapper.deleteById(id);
        return Result.success("已删除");
    }
}
