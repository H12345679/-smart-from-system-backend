package com.edu.smartfarm.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT消息发布器 — 向设备下发控制指令
 */
@Slf4j
@Component
public class MqttPublisher {

    @Autowired(required = false)
    private MqttClient mqttClient;

    /**
     * 下发指令到设备
     * @param topic 设备Topic (如 /ras/farm_1/device_xxx/cmd)
     * @param payload JSON指令内容
     */
    public boolean publish(String topic, String payload) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("MQTT未连接，指令无法通过MQTT下发: topic={}", topic);
            return false;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            mqttClient.publish(topic, message);
            log.info("MQTT指令已下发: topic={}, payload={}", topic, payload);
            return true;
        } catch (Exception e) {
            log.error("MQTT发布失败: {}", e.getMessage());
            return false;
        }
    }
}
