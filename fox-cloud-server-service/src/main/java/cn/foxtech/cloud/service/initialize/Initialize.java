package cn.foxtech.cloud.service.initialize;


import cn.foxtech.cloud.manager.initialize.InitializeSystem;
import cn.foxtech.cloud.manager.social.initialize.InitializeSocial;
import cn.foxtech.cloud.repository.initialize.InitializeRepository;
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
    private InitializeSystem initializeSystem;

    @Autowired
    private InitializeRepository initializeRepository;

    @Autowired
    private InitializeSocial initializeSocial;


    @Override
    public void run(String... args) {
        logger.info("------------------------初始化开始！------------------------");

        this.initializeSystem.initialize();
        this.initializeRepository.initialize();
        this.initializeSocial.initialize();

        logger.info("------------------------初始化结束！------------------------");
    }
}
