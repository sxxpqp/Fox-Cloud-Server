package cn.foxtech.cloud.mqtt.broker.initialize;


import cn.foxtech.cloud.mqtt.broker.service.MqttBrokerInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化
 */
@Component
public class Initialize implements CommandLineRunner {
    @Autowired
    private MqttBrokerInitializer mqttBrokerInitializer;


    @Override
    public void run(String... args) {
        this.mqttBrokerInitializer.Initialize();
    }
}
