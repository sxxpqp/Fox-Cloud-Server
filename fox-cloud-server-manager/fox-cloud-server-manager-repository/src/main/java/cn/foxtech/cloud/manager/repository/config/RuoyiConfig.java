package cn.foxtech.cloud.manager.repository.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 实例化配置Ruoyi的JAR的Bean对象
 */
@Configuration
@ComponentScan(basePackages = {"com.ruoyi.common.security.*"})
// 必须填写此包路径:指明JAR包里有@Component注解的子包
public class RuoyiConfig {
}
