package cn.foxtech.cloud.repo.comp.script.entity;

import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本软件包，它包含了一组操作的定义
 * 每个脚本实体，都代表了某个厂家的一个具体的设备型号，所以它的唯一性Key：manufacturer+deviceType
 */
@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoCompScriptEntity extends BaseModel {
    /**
     * 所有人：对该组件所有权的人，通常为开发者或者其团队的Leader
     */
    @Indexed
    private String ownerId;

    /**
     * 归属的分组：owner创建的分组，并允许加入分组的人，使用该组件
     */
    @Indexed
    private String groupId;

    /**
     * 归属的分组：owner创建的分组，并允许加入分组的人，使用该组件
     */
    @Indexed
    private String groupName;

    /**
     * 厂商信息：解码器的厂商信息
     */
    @Indexed
    private String manufacturer;
    /**
     * 设备型号：解码器的厂商信息
     */
    @Indexed
    private String deviceType;
    /**
     * 描述
     */
    private String description;
    /**
     * 提交版本的Key
     */
    private String commitKey;
    /**
     * 排序权重
     */
    private Integer weight;
}
