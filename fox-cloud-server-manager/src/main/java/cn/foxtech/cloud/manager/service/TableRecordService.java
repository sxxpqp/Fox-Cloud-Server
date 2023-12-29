package cn.foxtech.cloud.manager.service;

import cn.craccd.mongoHelper.bean.Page;
import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityField;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.constants.Constant;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 将redis的数据持久化到mongo中
 */
@Component
public class TableRecordService {
    @Autowired
    private MongoExHelper mongoHelper;


    /**
     * 构造过滤条件
     *
     * @param table
     * @param param
     * @return
     */
    private CriteriaAndWrapper buildWrapper(EdgeEntityTable table, Map<String, Object> param) {
        // 一级过滤条件
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        if (param.containsKey(Constant.field_edge_id)) {
            criteriaAndWrapper.eq(Constant.field_edge_id, param.get(Constant.field_edge_id));
        }

        // 二级过滤条件
        for (String key : table.getFields().keySet()) {
            EdgeEntityField field = table.getFields().get(key);

            Map<String, Object> values = (Map<String, Object>) param.get("values");
            if (values == null) {
                continue;
            }

            Object value = this.makeValue(field, values.get(key));
            if (value != null) {
                criteriaAndWrapper.eq("values." + key, value);
            }
        }

        return criteriaAndWrapper;
    }

    private Object makeValue(EdgeEntityField field, Object value) {
        try {
            if (value == null) {
                return null;
            }

            if ("long".equals(field.getType())) {
                return Long.valueOf(value.toString());
            }
            if ("string".equals(field.getType())) {
                return value.toString();
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> queryPageList(EdgeEntityTable table, Map<String, Object> body) {
        Integer pageNum = (Integer) body.get(Constant.field_page_num);
        Integer pageSize = (Integer) body.get(Constant.field_page_size);

        // 检查：是否至少包含以下几个参数
        if (MethodUtils.hasEmpty(pageNum, pageSize)) {
            throw new ServiceException("body参数缺失:entityType, pageNum, pageSize");
        }


        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = this.buildWrapper(table, body);

        // 分页查询
        Page<EdgeEntity> page = new Page<>();
        page.setQueryCount(true);
        page.setCurr(pageNum);
        page.setLimit(pageSize);
        SortBuilder sortBuilder = new SortBuilder(EdgeEntity::getId, Sort.Direction.ASC);
        Page<EdgeEntity> result = this.mongoHelper.findPage(criteriaAndWrapper, sortBuilder, page, table.getCollectionName(), EdgeEntity.class);

        // 将结果返回
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getCount());
        data.put("list", result.getList());

        return data;
    }

    public Long queryCount(EdgeEntityTable table) {
        return this.mongoHelper.findAllCount(table.getCollectionName(), EdgeEntity.class);
    }
}
