package com.edu.smartfarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.smartfarm.common.BusinessException;
import com.edu.smartfarm.entity.IotDevice;
import com.edu.smartfarm.mapper.IotDeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final IotDeviceMapper deviceMapper;

    public IotDevice register(String deviceName, String macAddress, String deviceType, String parameterType, Long tankId) {
        IotDevice existing = deviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>().eq(IotDevice::getMacAddress, macAddress));
        if (existing != null) throw new BusinessException("MAC地址已注册");

        IotDevice device = new IotDevice();
        device.setDeviceId(UUID.randomUUID().toString().replace("-", ""));
        device.setDeviceName(deviceName);
        device.setMacAddress(macAddress);
        device.setDeviceType(deviceType);
        device.setParameterType(parameterType);
        device.setTankId(tankId);
        // 生成MQTT Topic
        String topic = "SENSOR".equals(deviceType)
                ? "/ras/farm_1/tank_" + tankId + "/sensor_" + (parameterType != null ? parameterType.toLowerCase() : "unknown")
                : "/ras/farm_1/device_" + device.getDeviceId() + "/cmd";
        device.setMqttTopic(topic);
        device.setOnlineStatus(0);
        device.setStatus(1);
        deviceMapper.insert(device);
        return device;
    }

    public Page<IotDevice> list(String deviceType, Long tankId, Integer onlineStatus, Integer page, Integer size) {
        LambdaQueryWrapper<IotDevice> wrapper = new LambdaQueryWrapper<>();
        if (deviceType != null && !deviceType.isEmpty()) wrapper.eq(IotDevice::getDeviceType, deviceType);
        if (tankId != null) wrapper.eq(IotDevice::getTankId, tankId);
        if (onlineStatus != null) wrapper.eq(IotDevice::getOnlineStatus, onlineStatus);
        wrapper.orderByDesc(IotDevice::getCreateTime);
        return deviceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public IotDevice getDetail(Long id) {
        IotDevice device = deviceMapper.selectById(id);
        if (device == null) throw new BusinessException("设备不存在");
        return device;
    }

    public void bindTank(Long id, Long tankId) {
        IotDevice device = deviceMapper.selectById(id);
        if (device == null) throw new BusinessException("设备不存在");
        device.setTankId(tankId);
        deviceMapper.updateById(device);
    }

    public void remove(Long id) {
        deviceMapper.deleteById(id);
    }
}
