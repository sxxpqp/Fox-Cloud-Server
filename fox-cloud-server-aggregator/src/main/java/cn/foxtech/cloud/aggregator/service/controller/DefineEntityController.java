package cn.foxtech.cloud.aggregator.service.controller;

import cn.foxtech.cloud.aggregator.service.service.DefinePersistService;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityBuilder;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntitySchema;
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

import java.util.List;
import java.util.Map;

/**
 * 设备操作
 *
 * @author hupengwu
 */
@RestController
@RequestMapping("/define")
public class DefineEntityController {
    @Autowired
    private DefinePersistService persistService;


    @Autowired
    private EdgeEntityBuilder entityBuilder;


    @Autowired
    private EdgeEntityTimeStampService timeStampService;

    @Autowired
    private EdgeEntitySchema entitySchema;


    @PostMapping("entity")
    public AjaxResult operate(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            String entityType = (String) body.get("entityType");
            String timeStamp = (String) body.get("timeStamp");
            Map<String, Object> dataMap = (Map<String, Object>) body.get("data");

            if (MethodUtils.hasEmpty(edgeId, entityType, timeStamp)) {
                throw new ServiceException("body参数缺失:edgeId, entityType, timestamp");
            }

            if (edgeId.equals(Constants.SYSTEM)) {
                throw new ServiceException("edgeId非法!");
            }

            // 检查：该数据类型是否支持
            if (!this.entitySchema.getTables().containsKey(entityType)) {
                throw new ServiceException("不支持的实体类型:" + entityType);
            }

            // 场景2：上传数据阶段
            List<Map<String, Object>> insertList = (List<Map<String, Object>>) dataMap.get("insert");
            if (MethodUtils.hasEmpty(insertList)) {
                throw new ServiceException("body参数缺失:insert");
            }

            // 插入数据
            Map<String, Object> entityMap = this.entityBuilder.buildEntity(entityType, insertList, EdgeEntityBuilder.tableId);
            this.persistService.insertEntity(edgeId, entityType, entityMap, timeStamp, "insert");

            // 更新时间戳状态
            this.timeStampService.updateTimeSpan(edgeId, entityType, timeStamp, "insert");

            return AjaxResult.success();


        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 查询时间戳
     *
     * @param body
     * @return
     */
    @PostMapping("timestamp")
    public AjaxResult timestamp(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            List<String> entityTypeList = (List<String>) body.get("entityTypeList");

            if (MethodUtils.hasEmpty(edgeId, entityTypeList)) {
                throw new ServiceException("body参数缺失:edgeId/entityTypeList");
            }

            Map<String, Object> result = this.timeStampService.getTimeSpan(edgeId, entityTypeList);

            return AjaxResult.success("操作成功", result);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 重置
     *
     * @param body
     * @return
     */
    @PostMapping("reset")
    public AjaxResult reset(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            String entityType = (String) body.get("entityType");
            String timeStamp = (String) body.get("timeStamp");

            if (MethodUtils.hasEmpty(edgeId, entityType, timeStamp)) {
                throw new ServiceException("body参数缺失:edgeId, entityType, timestamp");
            }

            // 删除数据
            this.persistService.resetEntity(edgeId, entityType, timeStamp);

            // 更新时间戳状态
            this.timeStampService.updateTimeSpan(edgeId, entityType, timeStamp, "reset");

            return AjaxResult.success("操作成功");
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 完成
     *
     * @param body
     * @return
     */
    @PostMapping("complete")
    public AjaxResult complete(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            String entityType = (String) body.get("entityType");
            String timeStamp = (String) body.get("timeStamp");

            if (MethodUtils.hasEmpty(edgeId, entityType, timeStamp)) {
                throw new ServiceException("body参数缺失:edgeId, entityType, timestamp");
            }

            // 更新时间戳状态
            this.timeStampService.updateTimeSpan(edgeId, entityType, timeStamp, "complete");

            return AjaxResult.success("操作成功");
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

}
