package cn.foxtech.cloud.aggregator.service.task;


import cn.foxtech.cloud.common.mongo.service.EdgeEntitySchemaService;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTask;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 周期性清理缓存任务
 */
@Component
public class ReloadSchemaTask extends PeriodTask {
    @Autowired
    private EdgeEntitySchemaService schemaService;

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
        this.schemaService.reloadSchemaTable();
    }
}
