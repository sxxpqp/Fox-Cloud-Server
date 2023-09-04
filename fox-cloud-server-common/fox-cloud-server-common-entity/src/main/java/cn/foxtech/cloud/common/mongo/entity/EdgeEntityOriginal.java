package cn.foxtech.cloud.common.mongo.entity;

import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实体的MySQL结构化信息
 * 备注：它是通过HeidiSQL工具在MySQL查询SHOW COLUMNS FROM xxx后，用“复制为”JSON格式，导出的表结构数据
 */
@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class EdgeEntityOriginal extends BaseModel {
    /**
     * 数据库表名称
     */
    private String table;

    /**
     * 模式
     */
    private String mode;

    /**
     * 业务key
     */
    private List<String> serviceKey = new ArrayList<>();

    /**
     * 行信息
     */
    private List<Map<String,Object>> rows = new ArrayList<>();
}
