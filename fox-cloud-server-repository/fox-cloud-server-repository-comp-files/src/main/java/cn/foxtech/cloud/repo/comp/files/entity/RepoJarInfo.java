package cn.foxtech.cloud.repo.comp.files.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoJarInfo {
    private String groupId = "";
    private String artifactId = "";
    private String version = "";
}
