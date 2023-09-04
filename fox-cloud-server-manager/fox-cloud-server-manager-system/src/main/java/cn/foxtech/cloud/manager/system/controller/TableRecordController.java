package cn.foxtech.cloud.manager.system.controller;

import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
import cn.foxtech.cloud.common.mongo.service.EdgeEntitySchemaService;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.system.constants.Constant;
import cn.foxtech.cloud.manager.system.service.TableRecordService;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.rowset.serial.SerialException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/table")
public class TableRecordController {
    @Autowired
    private EdgeEntitySchemaService schemaService;

    @Autowired
    private TableRecordService recordService;


    @PostMapping("page")
    public AjaxResult getPage(@RequestBody Map<String, Object> body) {
        try {
            String entityType = (String) body.get(Constant.field_entity_type);

            if (MethodUtils.hasEmpty(entityType)) {
                throw new ServiceException("body参数:缺失entityType");
            }

            // 查询SchemaTable信息
            EdgeEntityTable table = this.schemaService.querySchemaTable(entityType);
            if (table == null) {
                throw new SerialException("schema未包含该定义：" + entityType);
            }

            // 查询数据
            Map<String, Object> data = this.recordService.queryPageList(table, body);

            // 脱敏处理
            this.sensitive(data);

            return AjaxResult.success(data);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    private void sensitive(Map<String, Object> data) {
        Map<String, Object> words = new HashMap<>();
        words.put("password", "-");

        List<EdgeEntity> list = (List<EdgeEntity>) data.get("list");
        for (EdgeEntity entity : list) {
            Map<String, Object> values = entity.getValues();
            if (values == null) {
                continue;
            }

            for (String word : words.keySet()) {
                if (values.containsKey(word)) {
                    values.put(word, words.get(word));
                }
            }

        }
    }

    @PostMapping("count")
    public AjaxResult getCount(@RequestBody Map<String, Object> body) {
        try {
            List<String> entityTypeList = (List<String>) body.get("entityTypeList");

            if (MethodUtils.hasEmpty(entityTypeList)) {
                throw new ServiceException("body参数:缺失entityType");
            }

            // 查询SchemaTable信息
            Map<String, EdgeEntityTable> tables = this.schemaService.querySchemaTable();

            // 检查：是否包括该定义
            Map<String, Object> data = new HashMap<>();
            for (String entityType : entityTypeList) {
                EdgeEntityTable table = tables.get(entityType);
                if (table == null) {
                    continue;
                }

                Long count = this.recordService.queryCount(table);
                data.put(entityType, count);
            }

            return AjaxResult.success(data);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

}
