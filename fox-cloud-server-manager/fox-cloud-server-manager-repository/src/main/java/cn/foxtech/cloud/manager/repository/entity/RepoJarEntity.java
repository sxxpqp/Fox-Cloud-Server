package cn.foxtech.cloud.manager.repository.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoJarEntity {
    /**
     * jar文件的pom.properties部分的信息
     */
    private RepoJarInfo property = new RepoJarInfo();
    /**
     * jar文件的pom.xml的dependencies部分的信息
     */
    private List<RepoJarInfo> dependencies = new ArrayList<>();
}
