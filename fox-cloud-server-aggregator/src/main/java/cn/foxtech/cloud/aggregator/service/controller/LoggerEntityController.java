package cn.foxtech.cloud.aggregator.service.controller;

import cn.foxtech.cloud.aggregator.service.service.RecordPersistService;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityBuilder;
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
@RequestMapping("/logger")
public class LoggerEntityController {
    @Autowired
    private RecordPersistService persistService;

    @Autowired
    private EdgeEntityBuilder entityBuilder;


    @PostMapping("entity")
    public AjaxResult operate(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            String entityType = (String) body.get("entityType");
            Map<String, Object> dataMap = (Map<String, Object>) body.get("data");


            if (MethodUtils.hasEmpty(edgeId, entityType, dataMap)) {
                throw new ServiceException("body参数:缺失edgeId/entityType/data");
            }
            if (edgeId.equals(Constants.SYSTEM)) {
                throw new ServiceException("edgeId非法!");
            }

            List<Map<String, Object>> insertList = (List<Map<String, Object>>) dataMap.get("insert");

            if (insertList != null && !insertList.isEmpty()) {
                Map<String, Object> entityMap = this.entityBuilder.buildEntity(entityType, insertList, EdgeEntityBuilder.tableId);
                this.persistService.insertEntity(edgeId, entityType, entityMap);
            }
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
                throw new ServiceException("body参数:缺失edgeId/entityTypeList");
            }

            Map<String, Object> result = this.persistService.queryMaxTableId(edgeId, entityTypeList);
            return AjaxResult.success("操作成功", result);
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
    @PostMapping("reset")
    public AjaxResult addReset(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get("edgeId");
            String operate = (String) body.get("operate");
            List<String> entityTypeList = (List<String>) body.get("entityTypeList");


            if (MethodUtils.hasEmpty(edgeId, operate, entityTypeList)) {
                throw new ServiceException("body参数缺失:edgeId/operate/entityTypeList");
            }

            return AjaxResult.success("操作成功");
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
