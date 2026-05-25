package com.edu.smartfarm.controller.device;

import com.edu.smartfarm.interceptor.RequireRole;
import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.entity.DeviceCommandLog;
import com.edu.smartfarm.entity.IotDevice;
import com.edu.smartfarm.mapper.DeviceCommandLogMapper;
import com.edu.smartfarm.mqtt.MqttPublisher;
import com.edu.smartfarm.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "设备管理模块", description = "设备注册、拓扑绑定、远程控制")
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceCommandLogMapper commandLogMapper;
    private final MqttPublisher mqttPublisher;

    @Data
    public static class RegisterDeviceRequest {
        private String deviceName;
        private String macAddress;
        private String deviceType;
        private String parameterType;
        private Long tankId;
    }

    @Operation(summary = "注册新设备")
    @PostMapping("/register")
    @RequireRole({"ADMIN"})
    public Result<?> registerDevice(@RequestBody RegisterDeviceRequest request) {
        IotDevice device = deviceService.register(request.getDeviceName(), request.getMacAddress(),
                request.getDeviceType(), request.getParameterType(), request.getTankId());
        return Result.success(device);
    }

    @Operation(summary = "查询设备列表")
    @GetMapping("/list")
    public Result<?> listDevices(@RequestParam(required = false) String deviceType,
                                  @RequestParam(required = false) Long tankId,
                                  @RequestParam(required = false) Integer onlineStatus,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(deviceService.list(deviceType, tankId, onlineStatus, page, size));
    }

    @Operation(summary = "查询设备详情")
    @GetMapping("/{id}")
    public Result<?> getDeviceDetail(@PathVariable Long id) {
        return Result.success(deviceService.getDetail(id));
    }

    @Operation(summary = "更新设备绑定关系")
    @PutMapping("/{id}/bindTank")
    public Result<?> bindTank(@PathVariable Long id, @RequestParam Long tankId) {
        deviceService.bindTank(id, tankId);
        return Result.success("绑定成功");
    }

    @Operation(summary = "注销设备")
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<?> removeDevice(@PathVariable Long id) {
        deviceService.remove(id);
        return Result.success("设备已注销");
    }

    @Data
    public static class CommandRequest {
        private Long deviceId;
        private String action;
    }

    @Operation(summary = "下发设备控制指令")
    @PostMapping("/command")
    public Result<?> sendCommand(@RequestBody CommandRequest request) {
        // 查找设备获取MQTT Topic
        IotDevice device = deviceService.getDetail(request.getDeviceId());

        // 记录指令日志
        DeviceCommandLog cmdLog = new DeviceCommandLog();
        cmdLog.setDeviceId(request.getDeviceId());
        cmdLog.setCommandType("ON".equals(request.getAction()) ? "START" : "STOP");
        cmdLog.setCommandPayload("{\"action\":\"" + request.getAction() + "\"}");
        cmdLog.setTriggeredBy("MANUAL");
        cmdLog.setAckStatus("PENDING");
        cmdLog.setCreateTime(java.time.LocalDateTime.now());
        commandLogMapper.insert(cmdLog);

        // 通过MQTT下发指令
        String topic = device.getMqttTopic() != null ? device.getMqttTopic() :
                "/ras/farm_1/device_" + device.getDeviceId() + "/cmd";
        boolean sent = mqttPublisher.publish(topic, cmdLog.getCommandPayload());

        // 更新指令状态
        cmdLog.setAckStatus(sent ? "SUCCESS" : "TIMEOUT");
        if (sent) cmdLog.setAckTime(java.time.LocalDateTime.now());
        commandLogMapper.updateById(cmdLog);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("commandId", cmdLog.getId());
        result.put("status", cmdLog.getAckStatus());
        result.put("mqttSent", sent);
        result.put("message", sent ? "指令已通过MQTT下发" : "MQTT未连接，指令记录已保存");
        return Result.success(result);
    }

    @Operation(summary = "查询指令执行状态")
    @GetMapping("/command/{commandId}/status")
    public Result<?> getCommandStatus(@PathVariable Long commandId) {
        DeviceCommandLog log = commandLogMapper.selectById(commandId);
        if (log == null) return Result.error("指令记录不存在");
        return Result.success(log);
    }
}
