package cn.foxtech.cloud.aggregator.service.service;

import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.aggregator.service.constants.Constant;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntitySchema;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.common.utils.number.NumberUtils;
import cn.foxtech.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将redis的数据持久化到mongo中
 */
@Component
public class RecordPersistService {
    @Autowired
    private EdgeEntitySchema entitySchema;


    @Autowired
    private MongoExHelper mongoHelper;


    /**
     * 查询最新的ID
     *
     * @param edgeId
     * @param entityTypeList
     * @return
     */
    public Map<String, Object> queryMaxTableId(String edgeId, List<String> entityTypeList) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntity::getEdgeId, edgeId);

        SortBuilder sortBuilder = new SortBuilder();
        sortBuilder.add("values.id", Sort.Direction.DESC);

        Map<String, Object> result = new HashMap<>();
        for (String entityType : entityTypeList) {
            EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
            if (entityTable == null) {
                continue;
            }
            if (!Constant.field_mode_logger.equals(entityTable.getModel()) && !Constant.field_mode_record.equals(entityTable.getModel())) {
                continue;
            }

            // 查询最近的数据
            EdgeEntity entity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, sortBuilder, entityTable.getCollectionName(), EdgeEntity.class);
            if (entity == null) {
                continue;
            }


            Long tableId = NumberUtils.makeLong(entity.getValues().get("id"));
            if (tableId == null) {
                continue;
            }

            result.put(entityType, tableId);
        }


        return result;
    }

    public void resetEntity(String edgeId, String entityType, Map<String, Object> entityMap) {
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_logger.equals(entityTable.getModel()) && !Constant.field_mode_record.equals(entityTable.getModel())) {
            throw new ServiceException("Cloud和Edge模式不匹配：" + entityType);
        }

        // 清空数据
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntity::getEdgeId, edgeId);
        this.mongoHelper.deleteByQuery(criteriaAndWrapper, entityTable.getCollectionName(), EdgeEntity.class);

        List<Object> insertList = new ArrayList<>();
        insertList.addAll(entityMap.values());

        for (Object data : insertList) {
            ((EdgeEntity) data).setEdgeId(edgeId);
        }

        this.mongoHelper.bulkInsert(insertList, entityTable.getCollectionName(), EdgeEntity.class);
    }

    public void insertEntity(String edgeId, String entityType, Map<String, Object> entityMap) {
        EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
        if (entityTable == null) {
            throw new ServiceException("不支持该实体类型：" + entityType);
        }
        if (!Constant.field_mode_logger.equals(entityTable.getModel()) && !Constant.field_mode_record.equals(entityTable.getModel())) {
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
