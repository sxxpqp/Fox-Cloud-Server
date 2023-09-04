package cn.foxtech.cloud.common.mongo.service;

import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.common.mongo.constants.Constant;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTimeStamp;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EdgeEntityTimeStampService {
    /**
     * 说明：为什么不用Autowired呢？
     * 每一个组件实力EntityServiceImpl需要都有一个IEntityService实例副本
     * 用了Autowired后就变成了所有EntityServiceImpl公用一个EntityService实例了
     */
    @Autowired
    private MongoExHelper mongoHelper;

    public void initialize() {
        List<String> indexFields = new ArrayList<>();
        indexFields.add("edgeId");
        indexFields.add("tableName");

        // 创建数据库表：如果不存在则创建，存在则跳过
        this.mongoHelper.createCollection(Constant.field_timestamp_name, indexFields);
    }


    public Map<String, Object> getTimeSpan(String edgeId, List<String> entityTypeList) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntityTimeStamp::getEdgeId, edgeId);
        List<EdgeEntityTimeStamp> entityList = this.mongoHelper.findListByQuery(criteriaAndWrapper, Constant.field_timestamp_name, EdgeEntityTimeStamp.class);

        Map<String, Object> result = new HashMap<>();
        for (String entityType : entityTypeList) {
            EdgeEntityTimeStamp find = null;
            for (EdgeEntityTimeStamp entity : entityList) {
                if (entityType.equals(entity.getTableName())) {
                    find = entity;
                    break;
                }
            }
            if (find != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("timeStamp", find.getTimeStamp());
                data.put("status", find.getStatus());
                result.put(find.getTableName(), data);
            } else {
                // 如果还没有记录，那么插入一个缺省的时间戳
                EdgeEntityTimeStamp entity = new EdgeEntityTimeStamp();
                entity.setEdgeId(edgeId);
                entity.setTableName(entityType);
                entity.setTimeStamp("");
                entity.setStatus("");
                this.mongoHelper.insert(Constant.field_timestamp_name, entity);

                Map<String, Object> data = new HashMap<>();
                data.put("timeStamp", entity.getTimeStamp());
                data.put("status", entity.getStatus());
                result.put(entity.getTableName(), data);
            }
        }

        return result;
    }

    public EdgeEntityTimeStamp updateTimeSpan(String edgeId, String tableName, String timeStamp, String status) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntityTimeStamp::getEdgeId, edgeId);
        criteriaAndWrapper.eq(EdgeEntityTimeStamp::getTableName, tableName);
        EdgeEntityTimeStamp timeStampEntity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, Constant.field_timestamp_name, EdgeEntityTimeStamp.class);
        if (timeStampEntity == null) {
            // 如果还没有记录，那么插入一个缺省的时间戳
            timeStampEntity = new EdgeEntityTimeStamp();
            timeStampEntity.setEdgeId(edgeId);
            timeStampEntity.setTableName(tableName);
            timeStampEntity.setTimeStamp(timeStamp);
            timeStampEntity.setStatus(status);
            this.mongoHelper.insert(Constant.field_timestamp_name, timeStampEntity);
        } else {
            timeStampEntity.setTimeStamp(timeStamp);
            timeStampEntity.setStatus(status);
            this.mongoHelper.updateById(Constant.field_timestamp_name, timeStampEntity);
        }

        return timeStampEntity;
    }
}
