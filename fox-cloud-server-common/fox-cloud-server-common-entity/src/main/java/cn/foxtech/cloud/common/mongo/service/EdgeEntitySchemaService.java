package cn.foxtech.cloud.common.mongo.service;

import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.common.mongo.constants.Constant;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityField;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityOriginal;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntitySchema;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.common.utils.DifferUtils;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.string.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EdgeEntitySchemaService {
    private static final Logger logger = Logger.getLogger(EdgeEntitySchemaService.class);

    @Autowired
    private MongoExHelper mongoHelper;


    @Autowired
    private EdgeEntitySchema schema;

    public void initialize() {
        // 创建edgeEntitySchema表：如果不存在则创建，存在则跳过
        this.mongoHelper.createCollection(Constant.field_schema_name, Constant.value_schema_index);

        // 查询各配置并创建实体表：如果不存在则创建，存在则跳过
        Map<String, EdgeEntityTable> tables = this.querySchemaTable();
        this.createCollection(tables);

        // 将配置信息缓存起来
        this.schema.getTables().clear();
        this.schema.getTables().putAll(tables);
    }

    public void reloadSchemaTable() {
        try {
            // 查询数据库信息
            Map<String, EdgeEntityTable> tables = this.querySchemaTable();

            // 检查：配置是否发生了变化
            if (!DifferUtils.differByValue(this.schema.getTables().values(), tables.values())) {
                return;
            }

            // 重新创建实体表
            this.createCollection(tables);

            //更新数据到本地缓存
            this.schema.getTables().clear();
            this.schema.getTables().putAll(tables);

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public Map<String, EdgeEntityTable> querySchemaTable() {
        Map<String, EdgeEntityTable> tables = new ConcurrentHashMap<>();

        // 查询记录
        List<EdgeEntityOriginal> list = this.mongoHelper.findAll(Constant.field_schema_name, EdgeEntityOriginal.class);


        for (EdgeEntityOriginal doc : list) {
            EdgeEntityTable table = this.buildTable(doc);
            if (table == null) {
                continue;
            }

            tables.put(table.getName(), table);
        }

        return tables;
    }

    public EdgeEntityTable querySchemaTable(String entityType) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(Constant.value_schema_index, entityType);


        // 查询记录
        EdgeEntityOriginal doc = this.mongoHelper.findOneByQuery(criteriaAndWrapper, Constant.field_schema_name, EdgeEntityOriginal.class);
        if (doc == null) {
            return null;
        }


        return this.buildTable(doc);
    }


    public Map<String, EdgeEntityTable> createCollection(Map<String, EdgeEntityTable> tables) {
        for (EdgeEntityTable table : tables.values()) {
            Set<String> index = new HashSet<>();

            // 一级索引
            index.add("edgeId");

            // 二级索引：ServiceKey上的索引
            for (String key : table.getServiceKey()) {
                if (key == null || key.isEmpty()) {
                    continue;
                }

                index.add("values." + key);
            }

            // 二级索引：field上的索引
            for (String key : table.getFields().keySet()) {
                EdgeEntityField field = table.getFields().get(key);
                if (field == null) {
                    continue;
                }
                if (field.getKey().isEmpty()) {
                    continue;
                }

                index.add("values." + key);
            }

            // 创建表和索引
            this.mongoHelper.createCollection(table.getCollectionName(), index);
        }

        return tables;
    }


    /**
     * 数据格式转换
     *
     * @param doc
     * @return
     */
    private EdgeEntityTable buildTable(EdgeEntityOriginal doc) {
        try {
            EdgeEntityTable entityTable = new EdgeEntityTable();

            /**
             * 检测：是否
             */
            if (MethodUtils.hasEmpty(doc.getTable(), doc.getMode(), doc.getServiceKey())) {
                throw new RuntimeException("table, mode, serviceKey为空");
            }

            entityTable.setName(doc.getTable());
            entityTable.setModel(doc.getMode());
            for (String key : doc.getServiceKey()) {
                entityTable.getServiceKey().add(StringUtils.camelName(key));
            }


            for (Map<String, Object> row : doc.getRows()) {
                String field = (String) row.get("Field");
                String type = (String) row.get("Type");
                String key = (String) row.get("Key");
                String format = (String) row.get("Format");

                if (MethodUtils.hasEmpty(field, type)) {
                    throw new RuntimeException(doc.getTable() + "的field, type为空");
                }

                EdgeEntityField entityFiled = new EdgeEntityField();
                entityFiled.setName(StringUtils.camelName(field));
                entityFiled.setType(this.getType(type));
                entityFiled.setKey(this.getKey(key));
                entityFiled.setFormat(this.getFormat(entityFiled.getName(), entityFiled.getType(), format));

                entityTable.getFields().put(entityFiled.getName(), entityFiled);
            }

            return entityTable;
        } catch (Exception e) {
            logger.error(Constant.field_schema_name + "数据异常：" + e.getMessage());
            return null;
        }
    }

    private String getType(String original) {
        if (original.indexOf("char") >= 0) {
            return "string";
        }
        if (original.equals("text")) {
            return "string";
        }
        if (original.indexOf("json") >= 0) {
            return "json";
        }
        if (original.equals("int")) {
            return "int";
        }
        if (original.equals("bigint")) {
            return "long";
        }

        throw new RuntimeException("未识别的字段类型：" + original);
    }

    private String getKey(String original) {
        if (original.isEmpty()) {
            return "";
        } else {
            return "index";
        }
    }

    private String getFormat(String fieldName, String type, String original) {
        // 如果已经配置：那么返回用户配置的数据
        if (original != null && !original.isEmpty()) {
            return original;
        }

        // 如果没有配置：默认值方式1，createTime/updateTime，约定为日期格式
        if ("createTime".equals(fieldName) || "updateTime".equals(fieldName)) {
            return "dateTime";
        }
        // 如果没有配置：默认值方式2，json，约定为json对象
        if ("json".equals(type)) {
            return "json";
        }

        return original;
    }
}
