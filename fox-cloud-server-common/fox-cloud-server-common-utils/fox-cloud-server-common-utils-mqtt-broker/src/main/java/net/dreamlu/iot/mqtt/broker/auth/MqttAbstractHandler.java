package net.dreamlu.iot.mqtt.broker.auth;

import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.http.api.code.ResultCode;
import net.dreamlu.iot.mqtt.core.server.http.api.result.Result;
import org.tio.core.ChannelContext;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;

/**
 * 外部抽象接口
 */
public abstract class MqttAbstractHandler {
    /**
     * 客户端认证逻辑实现
     */
    public boolean authAuthenticate(ChannelContext context, String uniqueId, String clientId, String userName, String password) {
        return true;
    }

    /**
     * 过滤器
     */
    public boolean httpAuthFilter(HttpRequest request) throws Exception {
        return true;
    }

    /**
     * 认证不通过时的响应
     */
    public HttpResponse httpAuthResponse(HttpRequest request) {
        return Result.fail(request, ResultCode.E103);
    }

    /**
     * 可自定义业务，判断客户端是否有发布的权限
     */
    public boolean hasPublishPermission(ChannelContext context, String clientId, String topic, MqttQoS qoS, boolean isRetain) {
        return true;
    }

    /**
     * 校验客户端订阅的 topic，校验成功返回 true，失败返回 false
     */
    public boolean subscribeValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
        return true;
    }

    /**
     * 返回的 uniqueId 会替代 mqtt client 传过来的 clientId，请保证返回的 uniqueId 唯一。
     */
    public String getUniqueId(ChannelContext context, String clientId, String userName, String password) {
        return clientId;
    }
}
