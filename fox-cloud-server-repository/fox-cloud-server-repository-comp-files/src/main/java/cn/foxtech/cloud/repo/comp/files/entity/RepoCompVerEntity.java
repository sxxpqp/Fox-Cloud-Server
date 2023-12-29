package cn.foxtech.cloud.repo.comp.files.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoCompVerEntity {
    /**
     * 版本号：例如 1.0.1
     */
    private String version;
    /**
     * 版本阶段：例如 master、release、develop、feature
     */
    private String stage;
    /**
     * 组件类型
     */
    private String component;

    /**
     * 下载路径
     */
    private String pathName;

    /**
     * 描述
     */
    private long fileSize;

    /**
     * 描述
     */
    private String description;

    /**
     * MD5校验码
     */
    private String md5;

    private Long createTime;

    private Long updateTime;
}
