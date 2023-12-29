package cn.foxtech.cloud.manager.task;


import cn.foxtech.common.utils.Maps;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTask;
import cn.foxtech.common.utils.scheduler.multitask.PeriodTaskType;
import cn.foxtech.common.utils.shell.ShellUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 周期性清理缓存任务
 */
@Component
public class CleanCacheTask extends PeriodTask {
    private static final Logger logger = Logger.getLogger(CleanCacheTask.class);

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

            // 取出上次保存的进程内存大小
            Double buffCacheLast = (Double) Maps.getOrDefault(this.statusMap, "OS buff/cache", 0.0d);

            // 检查：内存的膨胀状况，是否缓超过了512M
            if (buffCacheLast > 5.12E8) {
                return;
            }

            // 释放缓存
            ShellUtils.executeShell("echo 1 > /proc/sys/vm/drop_caches");

            // 获得释放后的缓存占用状况
            Double buffCacheNew = (Double) this.getMemInfo().get("ramBuffCache");

            // 将本次占用状况保存下来
            Maps.setValue(this.statusMap, "OS buff/cache", buffCacheNew);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private Map<String, Object> getMemInfo() {
        try {
            // 命令行获得操作系统信息
            List<String> shellLineList = ShellUtils.executeShell("free -h");

            String[] menItems = shellLineList.get(1).split("\\s+");
            Map<String, Object> map = new HashMap<>();
            map.put("ramTotalTxt", menItems[1]);
            map.put("ramTotal", makeNumber(menItems[1]));
            map.put("ramUsed", makeNumber(menItems[2]));
            map.put("ramFree", makeNumber(menItems[3]));
            map.put("ramShared", makeNumber(menItems[4]));
            map.put("ramBuffCache", makeNumber(menItems[5]));
            map.put("ramAvailable", makeNumber(menItems[6]));

            String[] swapItems = shellLineList.get(2).split("\\s+");
            map.put("swapTotalTxt", swapItems[1]);
            map.put("swapTotal", makeNumber(swapItems[1]));
            map.put("swapUsed", makeNumber(swapItems[2]));
            map.put("swapFree", makeNumber(swapItems[3]));

            return map;

        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private double makeNumber(String size) {
        if (size == null || size.isEmpty()) {
            return 0.0;
        }

        String data = size.toUpperCase();
        if (data.endsWith("%")) {
            data = size.substring(0, data.length() - 1);
            return Double.parseDouble(data);
        }
        if (data.endsWith("B")) {
            data = size.substring(0, data.length() - 1);
            return Double.parseDouble(data);
        }
        if (data.endsWith("BI")) {
            data = size.substring(0, data.length() - 2);
            return Double.parseDouble(data);
        }
        if (data.endsWith("K")) {
            data = size.substring(0, data.length() - 1);
            return Double.parseDouble(data) * 1024;
        }
        if (data.endsWith("KI")) {
            data = size.substring(0, data.length() - 2);
            return Double.parseDouble(data) * 1024;
        }
        if (data.endsWith("M")) {
            data = size.substring(0, data.length() - 1);
            return Double.parseDouble(data) * 1024 * 1024;
        }
        if (data.endsWith("MI")) {
            data = size.substring(0, data.length() - 2);
            return Double.parseDouble(data) * 1024 * 1024;
        }
        if (data.endsWith("G")) {
            data = size.substring(0, data.length() - 1);
            return Double.parseDouble(data) * 1024 * 1024 * 1024;
        }
        if (data.endsWith("GI")) {
            data = size.substring(0, data.length() - 2);
            return Double.parseDouble(data) * 1024 * 1024 * 1024;
        }

        return Double.parseDouble(data);
    }
}
