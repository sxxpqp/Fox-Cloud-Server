package cn.foxtech.cloud.common.mongo.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 字段信息：对应MySql的SHOW COLUMNS FROM xxx查询语句
 */
@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class EdgeEntityField {
    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 是否为key：要么为index，要么为空
     */
    private String key;

    /**
     * 格式
     */
    private String format;
}
