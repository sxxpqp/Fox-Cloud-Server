package cn.foxtech.cloud.manager.system.initialize;


import cn.foxtech.cloud.manager.system.scheduler.PeriodTasksScheduler;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 初始化
 */
@Component
public class InitializeSystem {
    private static final Logger logger = Logger.getLogger(InitializeSystem.class);


    @Autowired
    private PeriodTasksScheduler periodTasksScheduler;

    public void initialize() {
        logger.info("------------------------初始化开始！------------------------");

        this.periodTasksScheduler.initialize();
        this.periodTasksScheduler.schedule();

        logger.info("------------------------初始化结束！------------------------");
    }
}
