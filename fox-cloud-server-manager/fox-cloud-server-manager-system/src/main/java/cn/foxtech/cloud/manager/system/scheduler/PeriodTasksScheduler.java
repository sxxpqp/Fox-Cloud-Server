package cn.foxtech.cloud.manager.system.scheduler;


import cn.foxtech.cloud.manager.system.task.CleanCacheTask;
import cn.foxtech.cloud.manager.system.task.CleanLogsTask;
import cn.foxtech.cloud.manager.system.task.GcJavaProcessTask;
import cn.foxtech.cloud.manager.system.task.ReloadEdgeServerTask;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTaskScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 后台定时任务调度器：一个线程调度多个任务，所以后台任务不要阻塞，也不能去响应很及时的任务
 */
@Component
public class PeriodTasksScheduler extends PeriodTaskScheduler {

    /**
     * 定时对进程GC定时任务
     */
    @Autowired
    private ReloadEdgeServerTask edgeServerTask;

    @Autowired
    private GcJavaProcessTask gcJavaProcessTask;

    @Autowired
    private CleanCacheTask cleanCacheTask;

    @Autowired
    private CleanLogsTask cleanLogsTask;


    public void initialize() {
        this.insertPeriodTask(this.edgeServerTask);
        this.insertPeriodTask(this.gcJavaProcessTask);
        this.insertPeriodTask(this.cleanCacheTask);
        this.insertPeriodTask(this.cleanLogsTask);
    }
}
