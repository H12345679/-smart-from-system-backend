package com.edu.smartfarm.mqtt;

import com.edu.smartfarm.service.SensorDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT 消息回调处理 — 接收传感器数据
 * Topic格式: /ras/farm_{id}/tank_{id}/sensor_{type}
 * Payload格式: {"mac":"AA:BB:CC:01:01:01","value":7.2,"timestamp":1713081600}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageHandler implements MqttCallback {

    private final SensorDataService sensorDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload());
            log.debug("MQTT收到消息 Topic={}, Payload={}", topic, payload);

            // 解析Topic: /ras/farm_1/tank_3/sensor_do
            String[] parts = topic.split("/");
            if (parts.length >= 5 && parts[4].startsWith("sensor_")) {
                String tankIdStr = parts[3].replace("tank_", "");
                String paramType = parts[4].replace("sensor_", "").toUpperCase();
                Long tankId = Long.parseLong(tankIdStr);

                // 解析Payload
                JsonNode json = objectMapper.readTree(payload);
                double value = json.get("value").asDouble();
                long timestamp = json.has("timestamp") ? json.get("timestamp").asLong() : System.currentTimeMillis() / 1000;

                // 写入数据库 + 触发规则检测
                sensorDataService.processSensorData(tankId, paramType, value, timestamp);
            }
        } catch (Exception e) {
            log.error("MQTT消息处理异常: {}", e.getMessage());
        }
    }

    @Override
    public void disconnected(MqttDisconnectResponse response) {
        log.warn("MQTT连接断开: {}", response.getReasonString());
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        log.error("MQTT错误: {}", exception.getMessage());
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT{}连接成功: {}", reconnect ? "重新" : "", serverURI);
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {}

    @Override
    public void deliveryComplete(IMqttToken token) {}
}
