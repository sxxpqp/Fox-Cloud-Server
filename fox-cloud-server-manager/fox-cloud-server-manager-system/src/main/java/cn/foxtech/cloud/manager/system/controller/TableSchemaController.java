package cn.foxtech.cloud.manager.system.controller;

import cn.foxtech.cloud.common.mongo.entity.EdgeEntityTable;
import cn.foxtech.cloud.common.mongo.service.EdgeEntitySchemaService;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/schema")
public class TableSchemaController {
    @Autowired
    private EdgeEntitySchemaService service;

    @GetMapping("names")
    public AjaxResult queryNames() {
        try {
            Map<String, EdgeEntityTable> tables = this.service.querySchemaTable();
            return AjaxResult.success(tables.keySet());

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("entities")
    public AjaxResult queryEntities() {
        try {
            Map<String, EdgeEntityTable> tables = this.service.querySchemaTable();
            Collection<EdgeEntityTable> data = tables.values();
            return AjaxResult.success(data);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("entity")
    public AjaxResult queryEntity(String entityType) {
        try {
            if (MethodUtils.hasEmpty(entityType)) {
                throw new ServiceException("缺失entityType");
            }

            EdgeEntityTable table = this.service.querySchemaTable(entityType);
            return AjaxResult.success(table);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
