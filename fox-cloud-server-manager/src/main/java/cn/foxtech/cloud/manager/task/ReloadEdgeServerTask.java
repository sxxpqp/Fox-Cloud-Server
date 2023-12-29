package cn.foxtech.cloud.manager.task;


import cn.foxtech.cloud.common.mongo.service.EdgeServerService;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTask;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 周期性清理缓存任务
 */
@Component
public class ReloadEdgeServerTask extends PeriodTask {
    @Autowired
    private EdgeServerService edgeServerService;

    @Override
    public int getTaskType() {
        return PeriodTaskType.task_type_share;
    }

    /**
     * 获得调度周期
     *
     * @return 调度周期，单位秒
     */
    public int getSchedulePeriod() {
        return 60;
    }

    /**
     * 待周期性执行的操作
     */
    @Override
    public void execute() {
        this.edgeServerService.syncMongo2Redis();
    }
}
