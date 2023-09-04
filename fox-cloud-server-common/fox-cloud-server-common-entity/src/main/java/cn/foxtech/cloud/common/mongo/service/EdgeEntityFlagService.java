package cn.foxtech.cloud.common.mongo.service;


import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.common.mongo.constants.Constant;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityFlag;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EdgeEntityFlagService {
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
        this.mongoHelper.createCollection(Constant.field_flag_name, indexFields);
    }


    public Map<String, EdgeEntityFlag> getFlag(String edgeId, List<String> entityTypeList, String flagType) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntityFlag::getEdgeId, edgeId);
        List<EdgeEntityFlag> entityList = this.mongoHelper.findListByQuery(criteriaAndWrapper, Constant.field_flag_name, EdgeEntityFlag.class);

        Map<String, EdgeEntityFlag> result = new HashMap<>();
        for (String entityType : entityTypeList) {
            EdgeEntityFlag find = null;
            for (EdgeEntityFlag entity : entityList) {
                if (entityType.equals(entity.getTableName())) {
                    find = entity;
                    break;
                }
            }
            if (find != null) {
                result.put(find.getTableName(), find);
            } else {
                // 如果还没有记录，那么插入一个缺省的时间戳
                EdgeEntityFlag entity = new EdgeEntityFlag();
                entity.setEdgeId(edgeId);
                entity.setTableName(entityType);
                entity.setFlagType(flagType);
                entity.setFlagValue("");
                this.mongoHelper.insert(Constant.field_flag_name, entity);

                result.put(entity.getTableName(), entity);
            }
        }

        return result;
    }

    public EdgeEntityFlag updateFlag(String edgeId, String tableName, String flagType, String flagValue) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeEntityFlag::getEdgeId, edgeId);
        criteriaAndWrapper.eq(EdgeEntityFlag::getTableName, tableName);
        EdgeEntityFlag entity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, Constant.field_flag_name, EdgeEntityFlag.class);
        if (entity == null) {
            // 如果还没有记录，那么插入一个缺省的时间戳
            entity = new EdgeEntityFlag();
            entity.setEdgeId(edgeId);
            entity.setTableName(tableName);
            entity.setFlagType(flagType);
            entity.setFlagValue(flagValue);
            this.mongoHelper.insert(Constant.field_flag_name, entity);
        } else {
            entity.setFlagType(flagType);
            entity.setFlagValue(flagValue);
            this.mongoHelper.updateById(Constant.field_flag_name, entity);
        }

        return entity;
    }
}
