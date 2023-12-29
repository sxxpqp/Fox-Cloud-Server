package cn.foxtech.cloud.repo.product.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 组件的五要素，它可被用来确定组件仓库总的具体某个组件版本
 */
@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoProductComp {
    /**
     * 组件ID
     */
    private String uuid;
    /**
     * 模块类型：例如service
     */
    private String modelType = "service";

    /**
     * 模块名称：例如 device-service
     */
    private String modelName = "";

    /**
     * 接口版本：例如 v1
     */
    private String modelVersion = "v1";
}
