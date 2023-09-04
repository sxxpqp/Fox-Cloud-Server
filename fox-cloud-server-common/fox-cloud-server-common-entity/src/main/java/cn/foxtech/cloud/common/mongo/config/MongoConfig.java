package cn.foxtech.cloud.common.mongo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
@ComponentScan(basePackages = {"cn.craccd", "cn.foxtech.cloud.common.utils.mongo"})
// 必须填写此包路径:指明JAR包里有@Component注解的子包
public class MongoConfig {
    @Autowired
    private MongoDatabaseFactory mongoFactory;

    @Autowired
    private MongoMappingContext mongoMappingContext;


    /**
     * 优先使用自定义的MappingMongoConverter，而不是组件中的MappingMongoConverter，解决key中包含.字符的问题
     * @return
     * @throws Exception
     */
    @Bean
    @Primary
    public MappingMongoConverter mongoConverter() throws Exception {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoFactory);
        MappingMongoConverter mongoConverter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        mongoConverter.setTypeMapper(new DefaultMongoTypeMapper((String)null));
        mongoConverter.setMapKeyDotReplacement("_");
        mongoConverter.afterPropertiesSet();
        return mongoConverter;
    }

    // 开启事务(如使用单机mongodb,可不配置此@Bean)
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}