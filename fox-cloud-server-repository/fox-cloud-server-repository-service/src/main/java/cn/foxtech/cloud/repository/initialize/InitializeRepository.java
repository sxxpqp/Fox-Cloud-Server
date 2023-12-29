package cn.foxtech.cloud.repository.initialize;


import cn.foxtech.cloud.repo.comp.files.service.RepoCompService;
import cn.foxtech.cloud.repo.comp.script.service.RepoCompScriptService;
import cn.foxtech.cloud.repo.comp.script.service.RepoCompScriptVersionService;
import cn.foxtech.cloud.repo.group.service.RepoGroupService;
import cn.foxtech.cloud.repo.product.service.RepoProductEntityService;
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

    @Autowired
    private RepoProductEntityService productService;

    @Autowired
    private RepoCompScriptService scriptService;


    @Autowired
    private RepoCompScriptVersionService scriptVersionService;


    public void initialize() {
        logger.info("------------------------初始化开始！------------------------");

        this.compService.initialize();
        this.groupService.initialize();
        this.productService.initialize();
        this.scriptService.initialize();
        this.scriptVersionService.initialize();

        logger.info("------------------------初始化结束！------------------------");
    }
}
