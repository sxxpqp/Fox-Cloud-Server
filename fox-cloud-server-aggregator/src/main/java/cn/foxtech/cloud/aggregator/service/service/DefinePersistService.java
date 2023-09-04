package cn.foxtech.cloud.aggregator.service.service;

import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.aggregator.service.constants.Constant;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntitySchema;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 将redis的数据持久化到mongo中
 */
@Component
public class DefinePersistService {
    @Autowired
    private MongoExHelper mongoHelper;

    @Autowired
    private EdgeEntitySchema entitySchema;


    public void resetEntity(String edgeId, String entityType, String timeStamp) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_define.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }


        // 清空数据
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntity::getEdgeId, edgeId);
        this.mongoHelper.deleteByQuery(criteriaAndWrapper, entityTable.getCollectionName(), EdgeEntity.class);
    }

    public void insertEntity(String edgeId, String entityType, Map<String, Object> entityMap, String timeStamp, String status) {
        // 找到该实体类型的信息
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_define.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        List<Object> insertList = new ArrayList<>();
        insertList.addAll(entityMap.values());

        for (Object data : insertList) {
            ((EdgeEntity) data).setEdgeId(edgeId);
        }

        this.mongoHelper.bulkInsert(insertList, entityTable.getCollectionName(), EdgeEntity.class);
    }
}
