package cn.foxtech.cloud.repository.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 告知Spring框架去扫描其他包中的Component
 */
@Configuration
@ComponentScan(basePackages = {"cn.foxtech.cloud.common.mongo.config", "cn.foxtech.cloud.common.mongo"})
public class RepoEntityConfig {
}
