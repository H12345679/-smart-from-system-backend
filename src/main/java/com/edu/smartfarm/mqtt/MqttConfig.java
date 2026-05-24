package com.edu.smartfarm.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 客户端配置
 */
@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.subscribe-topic}")
    private String subscribeTopic;

    @Bean
    public MqttClient mqttClient(MqttMessageHandler messageHandler) {
        try {
            MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(30);

            client.setCallback(messageHandler);
            client.connect(options);
            client.subscribe(subscribeTopic, 1);
            log.info("MQTT连接成功，已订阅Topic: {}", subscribeTopic);
            return client;
        } catch (Exception e) {
            log.warn("MQTT连接失败(EMQX可能未启动): {}，系统将以无MQTT模式运行", e.getMessage());
            return null;
        }
    }
}
