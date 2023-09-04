package cn.foxtech.cloud.manager.system.controller;

import cn.foxtech.cloud.common.mongo.service.EdgeServerService;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/task")
public class TaskSchedulerController {
    @Autowired
    private EdgeServerService edgeServerService;

    @PostMapping("entity")
    public AjaxResult operate(@RequestBody Map<String, Object> body) {
        try {
            String taskName = (String) body.get("taskName");

            if (MethodUtils.hasEmpty(taskName)) {
                throw new ServiceException("body参数缺失:taskName");
            }

            if (taskName.equals("syncEdgeServerTask")) {
                this.edgeServerService.syncMongo2Redis();
            } else {
                throw new ServiceException("不认识的任务名:" + taskName);
            }

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
