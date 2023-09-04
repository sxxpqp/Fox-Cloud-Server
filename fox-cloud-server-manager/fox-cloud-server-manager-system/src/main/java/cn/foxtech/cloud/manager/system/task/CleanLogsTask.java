package cn.foxtech.cloud.manager.system.task;

import cn.foxtech.common.utils.scheduler.multitask.PeriodTask;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTaskType;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CleanLogsTask extends PeriodTask {
    private static final Logger logger = Logger.getLogger(CleanLogsTask.class);


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
        return 10 * 60;
    }

    /**
     * 待周期性执行的操作
     */
    @Override
    public void execute() {
        try {
            // 检查：是否是LINUX操作系统，只有在该环境下，才会运行loader方式
            String OS = System.getProperty("os.name").toLowerCase();
            if (OS.indexOf("linux") < 0) {
                return;
            }

            File nginxLogs = new File("/var/log/nginx");
            if (!nginxLogs.exists()) {
                return;
            }

            if (!nginxLogs.isDirectory()) {
                return;
            }

            // 删除日志文件
            for (String fileName : nginxLogs.list()) {
                File file = new File("/var/log/nginx/" + fileName);
                file.delete();
            }


        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
