package cn.foxtech.cloud.common.mongo.entity;

import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.HashMap;
import java.util.Map;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class EdgeEntity extends BaseModel {
    @Indexed
    private String edgeId;

    /**
     * 属性：[字段-数值表]的哈希表
     */
    private Map<String, Object> values = new HashMap<>();
}
