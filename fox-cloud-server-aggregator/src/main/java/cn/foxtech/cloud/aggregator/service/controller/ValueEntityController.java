package cn.foxtech.cloud.aggregator.service.controller;

import cn.foxtech.cloud.aggregator.service.service.ValuePersistService;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityBuilder;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntitySchema;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
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
@RequestMapping("/value")
public class ValueEntityController {
    @Autowired
    private ValuePersistService persistService;

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

            // 找到该实体类型的信息
            EdgeEntityTable entityTable = this.entitySchema.getTables().get(entityType);
            if (entityTable == null) {
                throw new ServiceException("不支持该实体类型：" + entityType);
            }


            // 上传的数据
            List<Map<String, Object>> insertList = (List<Map<String, Object>>) dataMap.get("insert");
            if (MethodUtils.hasEmpty(insertList)) {
                throw new ServiceException("body参数缺失:insert");
            }

            // 将用户提交的数据进行转换
            Map<String, Object> entityMap = this.entityBuilder.buildEntity(entityType, insertList, EdgeEntityBuilder.serviceKey);


            // 将mongodb中的数据查询出来
            Map<String, Object> existMap = this.persistService.queryMongoEntityList(edgeId, entityType);

            // 比对数据的差异
            Map<String, Map<String, Object>> compareMap = this.persistService.compareUpdate2Mongo(entityMap, existMap, entityType);
            Map<String, Object> insertMap = compareMap.get("insert");
            Map<String, Object> deleteMap = compareMap.get("delete");
            Map<String, Object> updateMap = compareMap.get("update");

            // 根据差异，进行增删改
            this.persistService.insert(edgeId, insertMap, entityType);
            this.persistService.delete(edgeId, deleteMap, entityType);
            this.persistService.update(edgeId, updateMap, entityType);

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

            // 检查：该数据类型是否支持
            if (!this.entitySchema.getTables().containsKey(entityType)) {
                throw new ServiceException("不支持的实体类型:" + entityType);
            }

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

            // 删除标识为失效的旧数据
            //    this.valuePersistService.deleteEntity(edgeId, entityType);

            // 更新时间戳状态
            this.timeStampService.updateTimeSpan(edgeId, entityType, timeStamp, "complete");

            return AjaxResult.success("操作成功");
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

}
