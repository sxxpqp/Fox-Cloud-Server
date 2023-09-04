package net.dreamlu.iot.mqtt.broker.auth;

import org.springframework.beans.factory.annotation.Autowired;

public class MqttHandlerInitializer {
    @Autowired
    private MqttAuthHandler mqttAuthHandler;

    @Autowired
    private MqttHttpAuthFilter mqttHttpAuthFilter;

    @Autowired
    private MqttPublishPermission mqttPublishPermission;

    @Autowired
    private MqttSubscribeValidator mqttSubscribeValidator;

    @Autowired
    private MqttUniqueIdService mqttUniqueIdService;

    /**
     * 绑定一个自定义的handlr
     *
     * @param handler
     */
    public void Initialize(MqttAbstractHandler handler) {
        this.mqttAuthHandler.setHandler(handler);
        this.mqttHttpAuthFilter.setHandler(handler);
        this.mqttPublishPermission.setHandler(handler);
        this.mqttSubscribeValidator.setHandler(handler);
        this.mqttUniqueIdService.setHandler(handler);
    }

}
