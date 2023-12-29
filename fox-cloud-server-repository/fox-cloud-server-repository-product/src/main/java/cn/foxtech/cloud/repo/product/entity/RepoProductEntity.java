package cn.foxtech.cloud.repo.product.entity;

import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.List;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoProductEntity extends BaseModel {
    /**
     * 产品ID
     */
    @Indexed
    private String uuid;
    /**
     * 厂商
     */
    private String manufacturer;
    /**
     * 产品型号
     */
    private String deviceType;
    /**
     * 产品型号
     */
    private String model;
    /**
     * 产品图片
     */
    private String image;
    /**
     * 产品描述
     */
    private String description;
    /**
     * 产品外部链接
     */
    private String url;
    /**
     * 产品标签：标签方便用户查找
     */
    private String tags;

    /**
     * 权重：排序使用，某些产品被优先推荐
     */
    private int weight = 0;

    /**
     * 产品的配套组件
     */
    private List<RepoProductComp> comps = new ArrayList<>();

}
