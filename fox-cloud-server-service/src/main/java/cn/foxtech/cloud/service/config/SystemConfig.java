package cn.foxtech.cloud.service.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 告知Spring框架去扫描其他包中的Component
 */
@Configuration
@ComponentScan(basePackages = {"cn.foxtech.cloud.manager.*"})
// 必须填写此包路径:指明JAR包里有@Component注解的子包
public class SystemConfig {
}
