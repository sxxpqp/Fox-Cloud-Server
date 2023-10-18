package cn.foxtech.cloud.aggregator.service.initialize;


import cn.foxtech.cloud.aggregator.service.mqtt.MqttClientService;
import cn.foxtech.cloud.aggregator.service.mqtt.MqttMessageRespond;
import cn.foxtech.cloud.aggregator.service.scheduler.PeriodTasksScheduler;
import cn.foxtech.cloud.aggregator.service.service.EntityManageService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化
 */
@Component
public class Initialize implements CommandLineRunner {
    private static final Logger logger = Logger.getLogger(Initialize.class);

    @Autowired
    private EntityManageService manageService;

    @Autowired
    private MqttClientService mqttClientService;

    @Autowired
    private PeriodTasksScheduler periodTasksScheduler;

    @Autowired
    private MqttMessageRespond mqttMessageRespond;

    @Override
    public void run(String... args) {
        logger.info("------------------------初始化开始！------------------------");

        this.manageService.initialize();

        this.mqttClientService.initialize();
        this.mqttMessageRespond.initialize();
        this.mqttMessageRespond.schedule();

        this.periodTasksScheduler.initialize();
        this.periodTasksScheduler.schedule();

        logger.info("------------------------初始化结束！------------------------");
    }
}
