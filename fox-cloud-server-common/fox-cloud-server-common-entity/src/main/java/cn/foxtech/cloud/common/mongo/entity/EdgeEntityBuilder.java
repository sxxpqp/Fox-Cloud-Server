package cn.foxtech.cloud.common.mongo.entity;


import com.fasterxml.jackson.core.JsonParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将边缘节点那边送来的Entity格式，转换为云端Mongo的Entity格式
 * 特点：将原来的id/createTime/updateTime，改名为tableId/tableCreateTime/tableUpdateTime
 */
@Component
public class EdgeEntityBuilder {
    public static final String serviceKey = "serviceKey";

    public static final String tableId = "id";

    @Autowired
    private EdgeEntitySchema schema;

    private static Long getLong(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long) {
            return ((Long) value).longValue();
        }

        return 0L;
    }

    /**
     * 对象转换
     *
     * @param dataList
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> Map<String, Object> buildEntity(String entityType, Collection<Map<String, Object>> dataList, String keyType) throws JsonParseException {
        EdgeEntityTable entityTable = this.schema.getTables().get(entityType);
        if (entityTable == null) {
            throw new RuntimeException("不支持的实体类型：" + entityType);
        }

        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> data : dataList) {
            // 提取schema中定义的字段数据：防止攻击措施
            EdgeEntity entity = new EdgeEntity();
            for (String key : data.keySet()) {
                if (!entityTable.getFields().containsKey(key)) {
                    continue;
                }

                entity.getValues().put(key, data.get(key));
            }

            if (EdgeEntityBuilder.serviceKey.equals(keyType)) {
                String serviceKey = this.makeServiceKeyList(entityTable.getServiceKey(), entity.getValues());
                result.put(serviceKey, entity);
            }
            if (EdgeEntityBuilder.tableId.equals(keyType) && entity.getValues().containsKey(EdgeEntityBuilder.tableId)) {
                String id = entity.getValues().get(tableId).toString();
                result.put(id, entity);
            }

        }

        return result;
    }

    public String makeServiceKeyList(List<String> serviceKey, Map<String, Object> map) {
        List<Object> keys = new ArrayList<>();
        for (String key : serviceKey) {
            keys.add(map.get(key));
        }

        return keys.toString();
    }
}