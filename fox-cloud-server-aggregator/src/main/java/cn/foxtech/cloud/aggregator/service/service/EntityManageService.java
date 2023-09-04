package cn.foxtech.cloud.aggregator.service.service;

import cn.foxtech.cloud.common.mongo.service.EdgeEntityFlagService;
import cn.foxtech.cloud.common.mongo.service.EdgeEntitySchemaService;
import cn.foxtech.cloud.common.mongo.service.EdgeEntityTimeStampService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntityManageService {
    @Autowired
    private EdgeEntitySchemaService schemaService;

    @Autowired
    private EdgeEntityTimeStampService timeStampService;

    @Autowired
    private EdgeEntityFlagService flagService;

    public void initialize() {
        // 初始化时间戳：预创建数据库表
        this.timeStampService.initialize();

        // 初始化标志表：预创建数据库表
        this.flagService.initialize();
        this.schemaService.initialize();
    }
}
