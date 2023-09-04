package cn.foxtech.cloud.aggregator.service.service;

import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.aggregator.service.constants.Constant;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityBuilder;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntitySchema;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.common.utils.DifferUtils;
import cn.foxtech.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将redis的数据持久化到mongo中
 */
@Component
public class ValuePersistService {
    @Autowired
    private MongoExHelper mongoHelper;

    @Autowired
    private EdgeEntitySchema entitySchema;


    @Autowired
    private EdgeEntityBuilder entityBuilder;

    /**
     * 查询Mongo上的数据
     */
    public Map<String, Object> queryMongoEntityList(String edgeId, String entityType) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_value.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntity::getEdgeId, edgeId);


        List<EdgeEntity> list = this.mongoHelper.findListByQuery(criteriaAndWrapper, entityTable.getCollectionName(), EdgeEntity.class);

        Map<String, Object> result = new HashMap<>();
        for (EdgeEntity entity : list) {
            String serviceKey = this.entityBuilder.makeServiceKeyList(entityTable.getServiceKey(), entity.getValues());
            result.put(serviceKey, entity);
        }
        return result;
    }

    public void insert(String edgeId, Map<String, Object> insertMap, String entityType) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_value.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        if (insertMap.isEmpty()) {
            return;
        }

        // 根据差异，进行增删改
        List<Object> dataList = new ArrayList<>(insertMap.values());

        for (Object data : dataList) {
            ((EdgeEntity) data).setEdgeId(edgeId);
        }

        this.mongoHelper.bulkInsert(dataList, entityTable.getCollectionName(), EdgeEntity.class);
    }

    public void delete(String edgeId, Map<String, Object> deleteMap, String entityType) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_value.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        if (deleteMap.isEmpty()) {
            return;
        }

        // 根据差异，进行增删改
        List<String> dataList = new ArrayList<>();
        for (String key : deleteMap.keySet()) {
            EdgeEntity entity = (EdgeEntity) deleteMap.get(key);
            dataList.add(entity.getId());
        }

        this.mongoHelper.deleteByIds(dataList, entityTable.getCollectionName(), EdgeEntity.class);
    }

    public void update(String edgeId, Map<String, Object> updateMap, String entityType) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_value.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        if (updateMap.isEmpty()) {
            return;
        }

        // 根据差异，进行增删改
        Map<String, Object> dataList = new HashMap<>();
        for (String key : updateMap.keySet()) {
            EdgeEntity entity = (EdgeEntity) updateMap.get(key);
            dataList.put(entity.getId(), entity);
        }

        this.mongoHelper.bulkUpdate(dataList, entityTable.getCollectionName(), EdgeEntity.class);
    }

    public Map<String, Map<String, Object>> compareUpdate2Mongo(Map<String, Object> updateEntityMap, Map<String, Object> mongoEntityMap, String entityType) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }

        Set<String> addList = new HashSet<>();
        Set<String> delList = new HashSet<>();
        Set<String> eqlList = new HashSet<>();
        DifferUtils.differByValue(mongoEntityMap.keySet(), updateEntityMap.keySet(), addList, delList, eqlList);

        Map<String, Map<String, Object>> result = new HashMap<>();


        // 新增
        Map<String, Object> insertMap = new HashMap<>();
        if (!addList.isEmpty()) {
            for (String key : addList) {
                insertMap.put(key, updateEntityMap.get(key));
            }
        }
        result.put("insert", insertMap);

        // 删除
        Map<String, Object> deleteMap = new HashMap<>();
        if (!delList.isEmpty()) {
            for (String key : delList) {
                deleteMap.put(key, mongoEntityMap.get(key));
            }
        }
        result.put("delete", deleteMap);

        // 修改
        Map<String, Object> updateMap = new HashMap<>();
        if (!eqlList.isEmpty()) {
            for (String key : eqlList) {

                EdgeEntity update = (EdgeEntity) updateEntityMap.get(key);
                EdgeEntity mongo = (EdgeEntity) mongoEntityMap.get(key);

                update.setEdgeId(mongo.getEdgeId());
                update.setId(mongo.getId());
                update.setCreateTime(mongo.getCreateTime());

                // 比较两边的业务值是否相同
                if (update.getValues().equals(mongo.getValues())) {
                    continue;
                }

                updateMap.put(key, update);
            }
        }
        result.put("update", updateMap);

        return result;
    }
}
