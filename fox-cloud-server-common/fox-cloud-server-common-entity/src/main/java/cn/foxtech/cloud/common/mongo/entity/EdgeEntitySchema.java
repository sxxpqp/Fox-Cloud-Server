package cn.foxtech.cloud.common.mongo.entity;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 字典表:一堆表定义的集合，这是缓存版本，需要由EdgeEntitySchemaService来重新装载缓存
 */
@Getter(value = AccessLevel.PUBLIC)
@Component
public class EdgeEntitySchema {
    /**
     * 一组表的结构定义：key为表名称
     */
    private final Map<String, EdgeEntityTable> tables = new ConcurrentHashMap<>();
}
