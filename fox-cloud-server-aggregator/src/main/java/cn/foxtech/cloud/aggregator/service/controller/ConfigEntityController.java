package cn.foxtech.cloud.aggregator.service.controller;

import cn.foxtech.cloud.aggregator.service.service.ConfigPersistService;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityFlag;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntitySchema;
import cn.foxtech.cloud.common.mongo.service.EdgeEntityFlagService;
import cn.foxtech.cloud.common.mongo.service.EdgeEntityTimeStampService;
import cn.foxtech.cloud.core.constant.Constants;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备操作
 *
 * @author hupengwu
 */
@RestController
@RequestMapping("/config")
public class ConfigEntityController {
    @Autowired
    private ConfigPersistService persistService;

    @Autowired
    private EdgeEntitySchema entitySchema;

    @Autowired
    private EdgeEntityFlagService flagService;

    @Autowired
    private EdgeEntityTimeStampService timeStampService;


    @PostMapping("entity")
    public AjaxResult operate(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            String entityType = (String) body.get("entityType");
            String timeStamp = (String) body.get("timeStamp");
            Map<String, Object> dataMap = (Map<String, Object>) body.get("data");

            if (MethodUtils.hasNull(edgeId, entityType, timeStamp, dataMap)) {
                throw new ServiceException("body参数，缺失edgeId, entityType, timeStamp, dataMap");
            }
            if (MethodUtils.hasNull(edgeId, entityType, timeStamp)) {
                throw new ServiceException("body参数，空参数edgeId, entityType, timeStamp");
            }

            // 检查：该数据类型是否支持
            if (!this.entitySchema.getTables().containsKey(entityType)) {
                throw new ServiceException("不支持的实体类型:" + entityType);
            }


            List<Map<String, Object>> resetList = (List<Map<String, Object>>) dataMap.get("reset");
            List<Map<String, Object>> insertList = (List<Map<String, Object>>) dataMap.get("insert");
            List<Map<String, Object>> updateList = (List<Map<String, Object>>) dataMap.get("update");
            List<String> deleteList = (List<String>) dataMap.get("delete");


            if (resetList != null && !resetList.isEmpty()) {
                this.persistService.resetEntity(edgeId, entityType, resetList, timeStamp);
            }
            if (insertList != null && !insertList.isEmpty()) {
                this.persistService.insertEntity(edgeId, entityType, insertList, timeStamp);
            }
            if (updateList != null && !updateList.isEmpty()) {
                this.persistService.updateEntity(edgeId, entityType, updateList, timeStamp);
            }
            if (deleteList != null && !deleteList.isEmpty()) {
                this.persistService.deleteEntity(edgeId, entityType, deleteList, timeStamp);
            }
            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 查询时间戳
     */
    @PostMapping("timestamp")
    public AjaxResult timestamp(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            List<String> entityTypeList = (List<String>) body.get("entityTypeList");

            if (edgeId == null || entityTypeList == null) {
                throw new ServiceException("body参数，缺失edgeId/entityTypeList");
            }

            Map<String, Object> result = this.timeStampService.getTimeSpan(edgeId, entityTypeList);
            return AjaxResult.success("操作成功", result);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 查询时间戳
     */
    @PostMapping("reset")
    public AjaxResult addReset(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            String operate = (String) body.get("operate");
            List<String> entityTypeList = (List<String>) body.get("entityTypeList");

            if (edgeId == null || operate == null || entityTypeList == null) {
                throw new ServiceException("body参数，缺失edgeId/entityType/operate");
            }
            if (edgeId.equals(Constants.SYSTEM)) {
                throw new ServiceException("edgeId非法!");
            }

            Map<String, EdgeEntityFlag> resetMap = this.flagService.getFlag(edgeId, entityTypeList, "reset");

            Map<String, Boolean> result = new HashMap<>();
            for (String entityType : entityTypeList) {
                if ("get".equals(operate)) {
                    EdgeEntityFlag flag = resetMap.get(entityType);
                    if (flag == null || !Boolean.TRUE.toString().equals(flag.getFlagValue())) {
                        result.put(entityType, false);
                    } else {
                        result.put(entityType, true);
                    }
                }
                if ("set".equals(operate)) {
                    this.flagService.updateFlag(edgeId, entityType, "reset", Boolean.TRUE.toString());
                }
            }
            return AjaxResult.success("操作成功", result);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}

