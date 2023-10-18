package cn.foxtech.cloud.aggregator.service.mqtt;

import cn.foxtech.cloud.aggregator.service.vo.RestfulLikeRequestVO;
import cn.foxtech.cloud.aggregator.service.vo.RestfulLikeRespondVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息队列
 */
@Component
public class MqttMessageMapping {
    public static final String TYPE_REQUEST = "request";
    public static final String TYPE_RESPOND = "respond";

    /**
     * request的发送队列
     */
    private final Map<String, RestfulLikeRequestVO> requestList = new HashMap<>();
    /**
     * respond的发送队列
     */
    private final List<RestfulLikeRespondVO> respondVOList = new ArrayList<>();

    public synchronized void insertRespondVO(RestfulLikeRespondVO respondVO) {
        this.respondVOList.add(respondVO);
    }

    public synchronized List<RestfulLikeRespondVO> removeRespondVOList() {
        List<RestfulLikeRespondVO> requestVOList = new ArrayList<>();
        requestVOList.addAll(this.respondVOList);
        this.respondVOList.clear();
        return requestVOList;
    }

    private boolean isEmptyRespond() {
        return this.respondVOList.isEmpty();
    }

    public synchronized void insertRequestVO(RestfulLikeRequestVO requestVO) {
        requestVO.setExecuteTime(0);
        this.requestList.put(requestVO.getMessageId(), requestVO);
    }

    public synchronized boolean isEmpty() {
        return (this.isEmptyRequest() && this.isEmptyRespond());
    }

    public synchronized boolean isEmpty(String type) {
        if (MqttMessageMapping.TYPE_REQUEST.equals(type)) {
            return this.isEmptyRequest();
        }
        if (MqttMessageMapping.TYPE_RESPOND.equals(type)) {
            return this.isEmptyRespond();
        }


        return (this.isEmptyRequest() && this.isEmptyRespond());
    }


    private boolean isEmptyRequest() {
        // 检查：是否有数据到达
        if (this.requestList.isEmpty()) {
            return true;
        }

        // 检查：是否有尚未处理的数据
        for (Map.Entry<String, RestfulLikeRequestVO> entry : this.requestList.entrySet()) {
            RestfulLikeRequestVO requestVO = entry.getValue();
            if (requestVO.getExecuteTime() == 0) {
                return false;
            }
        }

        // 此时：全是发送过的数据，处于等待响应阶段
        return true;
    }

    public synchronized List<RestfulLikeRequestVO> queryRequestVOList() {
        List<RestfulLikeRequestVO> requestVOList = new ArrayList<>();

        for (Map.Entry<String, RestfulLikeRequestVO> entry : this.requestList.entrySet()) {
            RestfulLikeRequestVO requestVO = entry.getValue();
            if (requestVO.getExecuteTime() == 0) {
                requestVOList.add(requestVO);
            }
        }

        return requestVOList;
    }

    public synchronized void updateRequestVO(String messageId, long time) {
        RestfulLikeRequestVO requestVO = this.requestList.get(messageId);
        if (requestVO == null) {
            return;
        }

        requestVO.setExecuteTime(time);
    }

    /**
     * 删除处理完成的任务
     *
     * @param messageId messageId
     */
    public synchronized void deleteRequestVO(String messageId) {
        this.requestList.remove(messageId);
    }

    public synchronized void deleteRequestVO(long timeout) {
        List<String> messageIdList = new ArrayList<>();
        long time = System.currentTimeMillis() + timeout;

        // 查找过期没响应的数据
        for (Map.Entry<String, RestfulLikeRequestVO> entry : this.requestList.entrySet()) {
            RestfulLikeRequestVO requestVO = entry.getValue();
            if (requestVO.getExecuteTime() > time) {
                messageIdList.add(requestVO.getUuid());
            }
        }

        // 删除数据
        for (String messageId : messageIdList) {
            this.requestList.remove(messageId);
        }
    }
}
