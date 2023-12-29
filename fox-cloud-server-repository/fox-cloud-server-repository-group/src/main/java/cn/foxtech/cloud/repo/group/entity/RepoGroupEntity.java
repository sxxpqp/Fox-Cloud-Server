package cn.foxtech.cloud.repo.group.entity;

import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoGroupEntity extends BaseModel {
    /**
     * 所有人：若依的登录userName
     */
    @Indexed
    private String ownerId;

    /**
     * 模块名称
     */
    @Indexed
    private String groupName;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否有效
     */
    private boolean valid = false;

    /**
     * 描述
     */
    private List<Map<String, Object>> members = new ArrayList<>();
}
