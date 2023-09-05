package cn.foxtech.cloud.manager.repository.initialize;


import cn.foxtech.cloud.manager.repository.service.RepoCompService;
import cn.foxtech.cloud.manager.repository.service.RepoGroupService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 初始化
 */
@Component
public class InitializeRepository {
    private static final Logger logger = Logger.getLogger(InitializeRepository.class);

    @Autowired
    private RepoCompService compService;

    @Autowired
    private RepoGroupService groupService;


    public void initialize() {
        logger.info("------------------------初始化开始！------------------------");
        this.compService.initialize();
        this.groupService.initialize();

        logger.info("------------------------初始化结束！------------------------");
    }
}
