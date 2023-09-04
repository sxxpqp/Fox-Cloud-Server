package cn.foxtech.cloud.common.mongo.entity;


import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * Fox-Edge智能网关设备信息
 */
@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class EdgeServer extends BaseModel {
    /**
     * 设备ID：Fox-Edge网关的CPU序列号
     */
    @Indexed
    private String edgeId;
    /**
     * 名称
     */
    @Indexed
    private String name;
}
