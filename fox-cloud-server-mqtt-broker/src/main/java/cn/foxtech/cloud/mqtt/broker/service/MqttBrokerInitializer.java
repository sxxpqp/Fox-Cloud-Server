package cn.foxtech.cloud.mqtt.broker.service;

import net.dreamlu.iot.mqtt.broker.auth.MqttHandlerInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MqttBrokerInitializer extends MqttHandlerInitializer {
    @Autowired
    private MqttBrokerHandler mqttBrokerHandler;

    /**
     * 绑定handler
     */
    public void Initialize() {
        this.Initialize(this.mqttBrokerHandler);
    }
}
