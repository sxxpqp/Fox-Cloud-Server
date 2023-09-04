package cn.foxtech.cloud.mqtt.broker.service;

import net.dreamlu.iot.mqtt.broker.auth.MqttAbstractHandler;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.http.api.code.ResultCode;
import net.dreamlu.iot.mqtt.core.server.http.api.result.Result;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;

@Component
public class MqttBrokerHandler extends MqttAbstractHandler {
    /**
     * 客户端认证逻辑实现
     */
    @Override
    public boolean authAuthenticate(ChannelContext context, String uniqueId, String clientId, String userName, String password) {
        return true;
    }

    /**
     * 过滤器
     */
    @Override
    public boolean httpAuthFilter(HttpRequest request) throws Exception {
        return true;
    }

    /**
     * 认证不通过时的响应
     */
    @Override
    public HttpResponse httpAuthResponse(HttpRequest request) {
        return Result.fail(request, ResultCode.E103);
    }

    /**
     * 可自定义业务，判断客户端是否有发布的权限
     */
    @Override
    public boolean hasPublishPermission(ChannelContext context, String clientId, String topic, MqttQoS qoS, boolean isRetain) {
        return true;
    }

    /**
     * 校验客户端订阅的 topic，校验成功返回 true，失败返回 false
     */
    @Override
    public boolean subscribeValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
        return true;
    }

    /**
     * 返回的 uniqueId 会替代 mqtt client 传过来的 clientId，请保证返回的 uniqueId 唯一。
     */
    @Override
    public String getUniqueId(ChannelContext context, String clientId, String userName, String password) {
        return clientId;
    }
}
