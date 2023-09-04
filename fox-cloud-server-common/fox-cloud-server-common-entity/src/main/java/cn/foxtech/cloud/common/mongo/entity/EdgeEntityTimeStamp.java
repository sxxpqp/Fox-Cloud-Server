package cn.foxtech.cloud.common.mongo.entity;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class EdgeEntityTimeStamp extends EdgeEntity {
    /**
     * 表名称
     */
    private String tableName;

    /**
     * 时间戳
     */
    private String timeStamp;

    /**
     * 状态
     */
    private String status;

}