package cn.foxtech.cloud.repo.comp.script.entity;

import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本软件版本信息
 */
@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoCompScriptVersionEntity extends BaseModel {
    /**
     * 作者：主要是开发者
     */
    @Indexed
    private String author;

    /**
     * 父节点RepoCompScriptEntity的ID
     */
    @Indexed
    private String scriptId;
    /**
     * 版本描述
     */
    private String description;
    /**
     * 操作信息
     */
    private List<RepoCompScriptOperateEntity> operates = new ArrayList<>();

}
