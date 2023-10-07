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
     * 协议版本
     * 对于解码器来说，就是解码器协议版本，也就是名空间"cn.foxtech.device.protocol.v1.cjt188"中的这个v1
     */
    private String modelVersion;

    /**
     * 厂商信息：解码器的厂商信息
     */
    private String manufacturer;

    /**
     * 设备型号：解码器的厂商信息
     */
    private String deviceType;
    /**
     * 名空间信息：解码器占用的名空间信息
     */
    private String namespace;

    /**
     * JAR包上的信息
     */
    private RepoJarEntity jarEntity;
    /**
     * 软件版本：最新版本
     */
    private RepoCompVerEntity lastVersion;

    /**
     * 软件版本：历史版本
     */
    private List<RepoCompVerEntity> versions = new ArrayList<>();

    /**
     * 描述
     */
    private String description;

    /**
     * 权重：排序使用，让某些比较重要的服务，优先置顶
     */
    private Integer weight = 0;

}
