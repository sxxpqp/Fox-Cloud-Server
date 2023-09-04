package cn.foxtech.cloud.common.mongo.service;

import cn.craccd.mongoHelper.bean.UpdateBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.common.mongo.constants.EdgeServerConstant;
import cn.foxtech.cloud.common.mongo.entity.EdgeServer;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.common.utils.DifferUtils;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EdgeServerService {
    @Autowired
    public RedisTemplate redisTemplate;

    @Autowired
    private MongoExHelper mongoHelper;

    public EdgeServer getEdgeServer(String edgeId) {
        // 查询数据
        Map<String, Object> map = (Map<String, Object>) this.redisTemplate.opsForHash().get(EdgeServerConstant.field_redis_key, edgeId);

        // 填充数据
        EdgeServer edgeServer = new EdgeServer();
        BeanUtil.fillBeanWithMap(map, edgeServer, CopyOptions.create().setIgnoreError(true));
        return edgeServer;
    }


    public void syncMongo2Redis() {
        // 预创建表:确保数据库表的存在
        this.createCollection();

        // 从mongo查询数据
        List<EdgeServer> mongoList = this.mongoHelper.findAll(EdgeServerConstant.field_table_name, EdgeServer.class);
        Map<String, Map<String, Object>> mongoMap = new HashMap<>();
        for (EdgeServer edgeServer : mongoList) {
            Map<String, Object> map = BeanUtil.beanToMap(edgeServer);
            mongoMap.put(edgeServer.getEdgeId(), map);
        }

        // 从redis查询数据
        Map<String, Map<String, Object>> redisMap = this.redisTemplate.opsForHash().entries(EdgeServerConstant.field_redis_key);

        // 比对两边的差异
        Set<String> addList = new HashSet<>();
        Set<String> delList = new HashSet<>();
        Set<String> eqlList = new HashSet<>();
        DifferUtils.differByValue(redisMap.keySet(), mongoMap.keySet(), addList, delList, eqlList);

        Set<String> mdyList = new HashSet<>();
        for (String key : eqlList) {
            if (mongoMap.get(key).equals(redisMap.get(key))) {
                continue;
            }

            mdyList.add(key);
        }

        Set<String> update = new HashSet<>();
        update.addAll(addList);
        update.addAll(mdyList);

        if (!update.isEmpty()) {
            Map<String, Map<String, Object>> map = new HashMap<>();
            for (String key : update) {
                map.put(key, mongoMap.get(key));
            }
            this.redisTemplate.opsForHash().putAll(EdgeServerConstant.field_redis_key, map);
        }
        if (!delList.isEmpty()) {
            for (String key : delList) {
                this.redisTemplate.opsForHash().delete(EdgeServerConstant.field_redis_key, key);
            }
        }
    }

    public void insert(EdgeServer edgeServer) {
        Map<String, Object> map = new HashMap<>();
        map.put(edgeServer.getEdgeId(), edgeServer);

        this.mongoHelper.insert(EdgeServerConstant.field_table_name, edgeServer);
        this.redisTemplate.opsForHash().putAll(EdgeServerConstant.field_redis_key, map);
    }

    public void delete(String edgeId) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeServerConstant.field_index_edge_id, edgeId);

        this.redisTemplate.opsForHash().delete(EdgeServerConstant.field_redis_key, edgeId);
        this.mongoHelper.deleteByQuery(criteriaAndWrapper, EdgeServerConstant.field_table_name, EdgeServer.class);
    }

    public void update(EdgeServer edgeServer) {
        Map<String, Object> map = new HashMap<>();
        map.put(edgeServer.getEdgeId(), edgeServer);

        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(EdgeServerConstant.field_index_edge_id, edgeServer.getEdgeId());
        UpdateBuilder updateBuilder = new UpdateBuilder();
        updateBuilder.set(EdgeServerConstant.field_index_name, edgeServer.getName());

        this.redisTemplate.opsForHash().putAll(EdgeServerConstant.field_redis_key, map);
        this.mongoHelper.updateFirst(criteriaAndWrapper, updateBuilder, EdgeServerConstant.field_table_name, EdgeServer.class);
    }

    private void createCollection() {
        Set<String> index = new HashSet<>();
        index.add(EdgeServerConstant.field_index_name);
        index.add(EdgeServerConstant.field_index_edge_id);
        this.mongoHelper.createCollection(EdgeServerConstant.field_table_name, index);
    }
}
