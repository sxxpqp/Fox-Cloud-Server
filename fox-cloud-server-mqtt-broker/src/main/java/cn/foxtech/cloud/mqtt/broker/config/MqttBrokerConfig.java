package cn.foxtech.cloud.mqtt.broker.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 告知Spring框架去扫描其他包中的Component
 */
@Configuration
@ComponentScan(basePackages = {"net.dreamlu.iot.mqtt.broker.*"})
public class MqttBrokerConfig {
}


