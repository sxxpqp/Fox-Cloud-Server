package cn.foxtech.cloud.common.mongo.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表结构信息
 */
@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
@Document
public class EdgeEntityTable {
    /**
     * 表名称
     */
    @Indexed
    private String name = "";

    /**
     * 模式
     */
    @Indexed
    private String model = "";

    /**
     * 业务key
     */
    private List<String> serviceKey = new ArrayList<>();

    /**
     * 一组字段:key为字段名
     */
    private Map<String, EdgeEntityField> fields = new ConcurrentHashMap<>();

    /**
     * 集合名为和实体名的差别是首字母大小写
     * 例如：DeviceEntity是业务上的实体名，deviceEntity是Mongodb的集合名
     * @return
     */
    public String getCollectionName() {
        if (this.name == null || this.name.isEmpty()) {
            return this.name;
        }

        return this.name.toLowerCase().charAt(0) + this.name.substring(1);
    }
}
