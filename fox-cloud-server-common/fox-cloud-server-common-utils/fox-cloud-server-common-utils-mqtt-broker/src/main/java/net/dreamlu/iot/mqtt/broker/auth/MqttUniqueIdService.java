package net.dreamlu.iot.mqtt.broker.auth;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerUniqueIdService;
import org.springframework.context.annotation.Configuration;
import org.tio.core.ChannelContext;

/**
 * 示例自定义 clientId，请按照自己的需求和业务进行扩展
 *
 * @author L.cm
 */
@Configuration(proxyBeanMethods = false)
public class MqttUniqueIdService implements IMqttServerUniqueIdService {
	@Getter(value = AccessLevel.PUBLIC)
	@Setter(value = AccessLevel.PUBLIC)
	private MqttAbstractHandler handler;

	@Override
	public String getUniqueId(ChannelContext context, String clientId, String userName, String password) {
		if (this.handler != null) {
			return handler.getUniqueId(context, clientId, userName, password);
		}

		// 返回的 uniqueId 会替代 mqtt client 传过来的 clientId，请保证返回的 uniqueId 唯一。
		return clientId;
	}

}
