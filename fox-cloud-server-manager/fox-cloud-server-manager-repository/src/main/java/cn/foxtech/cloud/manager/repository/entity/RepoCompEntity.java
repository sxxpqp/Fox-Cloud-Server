package cn.foxtech.cloud.manager.repository.entity;


import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.List;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoCompEntity extends BaseModel {
    /**
     * 所有人：对该组件所有权的人，通常为开发者或者其团队的Leader
     */
    @Indexed
    private String ownerId;

    /**
     * 归属的分组：owner创建的分组，并允许加入分组的人，使用该组件
     */
    @Indexed
    private String groupName;

    /**
     * 模块类型
     */
    @Indexed
    private String modelType;

    /**
     * 模块名称
     */
    @Indexed
    private String modelName;

    /**
     * 组件类型：缺省的组件类型，新增版本的适合，从这边复制缺省值
     */
    private String component;

    /**
     * 最新版本
     */
    private RepoCompVerEntity lastVersion;

    /**
     * 历史版本
     */
    private List<RepoCompVerEntity> versions = new ArrayList<>();

    /**
     * 描述
     */
    private String description;

}
