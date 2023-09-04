package cn.foxtech.cloud.common.mongo.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class EdgeEntityFlag extends EdgeEntity {
    /**
     * 表名称
     */
    private String tableName;


    /**
     * 标记类型
     */
    private String flagType;

    /**
     * 标记数值
     */
    private String flagValue;

}