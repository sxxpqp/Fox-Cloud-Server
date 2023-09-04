package cn.foxtech.cloud.manager.system.task;


import cn.foxtech.common.utils.Maps;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTask;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTaskType;
import cn.foxtech.common.utils.shell.ShellUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 周期性GC任务
 */
@Component
public class GcJavaProcessTask extends PeriodTask {
    private static final Logger logger = Logger.getLogger(GcJavaProcessTask.class);

    private final Map<String, Object> statusMap = new HashMap<>();


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
        try {
            // 检查：是否是LINUX操作系统，只有在该环境下，才会运行loader方式
            String OS = System.getProperty("os.name").toLowerCase();
            if (OS.indexOf("linux") < 0) {
                return;
            }

            // 通过PS命令获得进程列表
            Map<Long, Long> pid2rss = this.getPID2RSS();

            // 逐个对进程进行GC操作
            for (Long pid : pid2rss.keySet()) {
                try {
                    Long rss = pid2rss.get(pid);

                    // 取出上次保存的进程内存大小
                    Long rssLast = (Long) Maps.getOrDefault(this.statusMap, "gc", "rss", pid, 0L);

                    // 检查：内存的膨胀状况，是否超过100M内存
                    if (rss < rssLast + 100 * 1000) {
                        continue;
                    }

                    // 膨胀过大的进程，进行GC回收过期内存
                    this.gcProcess(pid);

                    Map<Long, Long> newPid2rss = this.getPID2RSS();

                    // 获得GC后进程的内存占用大小
                    Long rssNew = newPid2rss.get(pid);
                    if (rssNew == null) {
                        continue;
                    }

                    // 将本次内存大小保存下来
                    Maps.setValue(this.statusMap, "gc", "rss", pid, rssNew);
                } catch (Exception e) {
                    logger.error("GC进程失败：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private Map<Long, Long> getPID2RSS() throws IOException, InterruptedException {
        Map<Long, Long> result = new HashMap<>();

        List<String> shellLineList = ShellUtils.executeShell("ps -aux|grep java");
        for (String shellLine : shellLineList) {
            String[] items = shellLine.split("\\s+");
            if (items.length < 14) {
                continue;
            }

            // ps -aux返回的格式
            // 0~10是linux的固定信息项目
            Long pid = Long.parseLong(items[1]);
            Long rss = Long.parseLong(items[5]);

            // 检查：该命令是否为java命令
            if (!"java".equals(items[10]) && !items[10].endsWith("/java")) {
                continue;
            }


            result.put(pid, rss);
        }

        return result;
    }

    public List<String> gcProcess(Long pid) throws IOException, InterruptedException {
        return ShellUtils.executeShell("jmap -histo:live " + pid + " | head -10");
    }
}
