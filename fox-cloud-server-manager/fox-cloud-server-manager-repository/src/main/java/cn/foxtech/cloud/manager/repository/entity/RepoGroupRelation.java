package cn.foxtech.cloud.manager.repository.entity;

import cn.craccd.mongoHelper.bean.BaseModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.HashSet;
import java.util.Set;

/**
 * Group和User的双向关系
 */
@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoGroupRelation  extends BaseModel {
    /**
     * 方向：user2group 或者 group2user
     */
    @Indexed
    private String direct;

    @Indexed
    private String name;

    private Set<String> objects = new HashSet<>();
}
