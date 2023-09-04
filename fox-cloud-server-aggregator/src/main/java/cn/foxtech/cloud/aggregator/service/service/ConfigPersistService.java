package cn.foxtech.cloud.aggregator.service.service;

import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.aggregator.service.constants.Constant;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityBuilder;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntitySchema;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
import cn.foxtech.cloud.common.mongo.service.EdgeEntityTimeStampService;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.common.utils.DifferUtils;
import cn.foxtech.core.exception.ServiceException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将redis的数据持久化到mongo中
 */
@Component
public class ConfigPersistService {
    private static final Logger logger = Logger.getLogger(ConfigPersistService.class);

    @Autowired
    private MongoExHelper mongoHelper;

    @Autowired
    private EdgeEntitySchema entitySchema;

    @Autowired
    private EdgeEntityBuilder entityBuilder;

    @Autowired
    private EdgeEntityTimeStampService timeStampService;


    public List<EdgeEntity> queryEntity(String edgeId, String entityType, List<Map<String, Object>> updateList, String timeStamp) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntity::getEdgeId, edgeId);
        return this.mongoHelper.findListByQuery(criteriaAndWrapper, entityTable.getCollectionName(), EdgeEntity.class);
    }

    /**
     * 插入新增数据：已经存在的部分不插入
     */
    public void insertEntity(String edgeId, String entityType, List<Map<String, Object>> resetList, String timeStamp) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntity::getEdgeId, edgeId);
        List<EdgeEntity> mongoList = this.mongoHelper.findListByQuery(criteriaAndWrapper, entityTable.getCollectionName(), EdgeEntity.class);

        Map<String, Map<String, Object>> compareMap = this.compareEntity(edgeId, entityType, mongoList, resetList);
        Map<String, Object> insertMap = compareMap.get("insert");

        // 根据差异，进行增删改
        this.insert(edgeId, insertMap, entityType);

        // 更新时间戳
        this.timeStampService.updateTimeSpan(edgeId, entityType, timeStamp, "");
    }

    public void resetEntity(String edgeId, String entityType, List<Map<String, Object>> resetList, String timeStamp) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntity::getEdgeId, edgeId);
        List<EdgeEntity> mongoList = this.mongoHelper.findListByQuery(criteriaAndWrapper, entityTable.getCollectionName(), EdgeEntity.class);

        Map<String, Map<String, Object>> compareMap = this.compareEntity(edgeId, entityType, mongoList, resetList);
        Map<String, Object> insertMap = compareMap.get("insert");
        Map<String, Object> deleteMap = compareMap.get("delete");
        Map<String, Object> updateMap = compareMap.get("update");

        // 根据差异，进行增删改
        this.insert(edgeId, insertMap, entityType);
        this.delete(edgeId, deleteMap, entityType);
        this.update(edgeId, updateMap, entityType);

        // 更新时间戳
        this.timeStampService.updateTimeSpan(edgeId, entityType, timeStamp, "");
    }

    public void updateEntity(String edgeId, String entityType, List<Map<String, Object>> updateList, String timeStamp) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntity::getEdgeId, edgeId);
        List<EdgeEntity> mongoList = this.mongoHelper.findListByQuery(criteriaAndWrapper, entityTable.getCollectionName(), EdgeEntity.class);

        Map<String, Map<String, Object>> compareMap = this.compareEntity(edgeId, entityType, mongoList, updateList);
        Map<String, Object> insertMap = compareMap.get("insert");
        Map<String, Object> updateMap = compareMap.get("update");

        // 根据差异，进行增删改
        this.insert(edgeId, insertMap, entityType);
        this.update(edgeId, updateMap, entityType);

        // 更新时间戳
        this.timeStampService.updateTimeSpan(edgeId, entityType, timeStamp, "");
    }

    public void deleteEntity(String edgeId, String entityType, List<String> deleteList, String timeStamp) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        this.mongoHelper.deleteByIds(deleteList, entityTable.getCollectionName(), EdgeEntity.class);

        // 更新时间戳
        this.timeStampService.updateTimeSpan(edgeId, entityType, timeStamp, "");
    }

    private Map<String, Map<String, Object>> compareEntity(String edgeId, String entityType, List<EdgeEntity> mongoList, List<Map<String, Object>> resetList) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        Map<String, Object> mongoEntityMap = new HashMap<>();
        for (EdgeEntity entity : mongoList) {
            String serviceKey = this.entityBuilder.makeServiceKeyList(entityTable.getServiceKey(), entity.getValues());
            mongoEntityMap.put(serviceKey, entity);
        }

        Map<String, Map<String, Object>> updateEntityMap = new HashMap<>();
        for (Map<String, Object> values : resetList) {
            String serviceKey = this.entityBuilder.makeServiceKeyList(entityTable.getServiceKey(), values);
            updateEntityMap.put(serviceKey, values);
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
                EdgeEntity entity = new EdgeEntity();
                entity.setEdgeId(edgeId);
                entity.getValues().putAll(updateEntityMap.get(key));
                insertMap.put(key, entity);
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

                Map<String, Object> update = updateEntityMap.get(key);
                EdgeEntity mongo = (EdgeEntity) mongoEntityMap.get(key);

                // 比较两边的业务值是否相同
                if (mongo.getValues().equals(update)) {
                    continue;
                }

                mongo.setValues(update);

                updateMap.put(key, mongo);
            }
        }
        result.put("update", updateMap);

        return result;
    }

    public void insert(String edgeId, Map<String, Object> insertMap, String entityType) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
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
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
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
        if (!Constant.field_mode_config.equals(entityTable.getModel())) {
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
}
