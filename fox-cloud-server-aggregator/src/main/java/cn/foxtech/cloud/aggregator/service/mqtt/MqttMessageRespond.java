package cn.foxtech.cloud.aggregator.service.mqtt;


import cn.foxtech.cloud.aggregator.service.vo.RestfulLikeRequestVO;
import cn.foxtech.cloud.aggregator.service.vo.RestfulLikeRespondVO;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.common.utils.Maps;
import cn.foxtech.common.utils.json.JsonUtils;
import cn.foxtech.common.utils.scheduler.singletask.PeriodTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MqttMessage的执行者
 */
@Component
public class MqttMessageRespond extends PeriodTaskService {
//    @Autowired
//    private HttpRestfulProxyService httpRestfulProxyService;
//
//    @Autowired
//    private RedisTopicProxyService redisTopicProxyService;

    private final Map<String, Object> controllerMethod = new HashMap<>();
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private MqttMessageMapping mqttMessageQueue;
    @Autowired
    private MqttClientService clientService;
    @Autowired
    private MqttConfigService configService;

    public void initialize() {
        String[] beanDefinitionNames = this.applicationContext.getBeanDefinitionNames();
        for (String name : beanDefinitionNames) {
            Object bean = this.applicationContext.getBean(name);
            if (bean == null) {
                continue;
            }

            Class clazz = bean.getClass();

            // 检测：是否为Controller，根据该Bean对象是否包含RequestMapping注解判断
            if (!clazz.isAnnotationPresent(RequestMapping.class)) {
                continue;
            }

            // 从类注解上取出path信息
            RequestMapping requestAnnotation = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
            String[] classPaths = requestAnnotation.value();
            if (classPaths == null || classPaths.length == 0) {
                continue;
            }

            String classPath = classPaths[0];


            // 检测方法：是否包含PostMapping等注解
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(PostMapping.class)) {
                    PostMapping operAnnotation = method.getAnnotation(PostMapping.class);
                    String[] methodPaths = operAnnotation.value();
                    if (methodPaths == null || methodPaths.length == 0) {
                        continue;
                    }

                    String res = classPath + "/" + methodPaths[0];
                    String methodKey = res + ":" + "POST";
                    Maps.setValue(this.controllerMethod, methodKey, "bean", bean);
                    Maps.setValue(this.controllerMethod, methodKey, "method", method);
                    continue;
                }
            }
        }


    }

    @Override
    public void execute(long threadId) throws Exception {
        if (this.mqttMessageQueue.isEmpty()) {
            Thread.sleep(250);
            return;
        }

        // 场景1：发送前面预处理阶段中的拒绝报文
        if (!this.mqttMessageQueue.isEmpty(MqttMessageMapping.TYPE_RESPOND)) {
            List<RestfulLikeRespondVO> respondVOList = this.mqttMessageQueue.removeRespondVOList();

            for (RestfulLikeRespondVO respondVO : respondVOList) {

                // 返回响应消息
                String rspContext = JsonUtils.buildJson(respondVO);

                String pubTopic = this.configService.getPublish();
                this.clientService.getMqttClient().publish(pubTopic, rspContext.getBytes(StandardCharsets.UTF_8));
            }
        }

        // 场景2：通过预处理的待执行报文
        if (!this.mqttMessageQueue.isEmpty(MqttMessageMapping.TYPE_REQUEST)) {
            List<RestfulLikeRequestVO> requestVOList = this.mqttMessageQueue.queryRequestVOList();

            for (RestfulLikeRequestVO requestVO : requestVOList) {
                // 记录处理时间
                this.mqttMessageQueue.updateRequestVO(requestVO.getUuid(), System.currentTimeMillis());

                // 执行请求
                RestfulLikeRespondVO respondVO = this.execute(requestVO);

                // 剔除内部信息
                Map map = JsonUtils.buildObject(respondVO, Map.class);
                if (map != null) {
                    map.remove("messageTopic");
                    map.remove("messageId");

                    // 返回信息的topic
                    String pubTopic = this.configService.getPublish();
                    if (respondVO.getMessageTopic() != null && respondVO.getMessageTopic().startsWith(this.configService.getSubscribe() + "/")) {
                        String edgeId = respondVO.getMessageTopic().substring(this.configService.getSubscribe().length() + 1);
                        pubTopic = this.configService.getPublish().replace("{edgeId}", edgeId);
                    }

                    // 返回响应消息
                    String rspContext = JsonUtils.buildJson(map);
                    this.clientService.getMqttClient().publish(pubTopic, rspContext.getBytes(StandardCharsets.UTF_8));
                }


                // 删除任务
                this.mqttMessageQueue.deleteRequestVO(requestVO.getMessageId());
            }

            // 删除超时响应的数据
            this.mqttMessageQueue.deleteRequestVO(60 * 1000);
        }

    }

    /**
     * 执行请求
     *
     * @param requestVO
     * @return
     */
    private RestfulLikeRespondVO execute(RestfulLikeRequestVO requestVO) {
        try {

            String methodKey = requestVO.getResource() + ":" + requestVO.getMethod().toUpperCase();
            Object bean = Maps.getValue(this.controllerMethod, methodKey, "bean");
            Object method = Maps.getValue(this.controllerMethod, methodKey, "method");
            if (method == null || bean == null) {
                throw new ServiceException("尚未支持的方法");
            }

            // 执行controller的bean函数
            Object value = ((Method) method).invoke(bean, requestVO.getBody());

            RestfulLikeRespondVO respondVO = new RestfulLikeRespondVO();
            respondVO.bindVO(requestVO);
            respondVO.setBody(value);

            return respondVO;
        } catch (Exception e) {
            RestfulLikeRespondVO respondVO = RestfulLikeRespondVO.error(e.getMessage());
            if (requestVO != null) {
                respondVO.bindVO(requestVO);
            }

            return respondVO;
        }
    }
}
