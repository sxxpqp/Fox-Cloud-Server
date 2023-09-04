package cn.foxtech.cloud.manager.system.controller;

import cn.craccd.mongoHelper.bean.Page;
import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.common.mongo.constants.EdgeServerConstant;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.mongo.entity.EdgeServer;
import cn.foxtech.cloud.common.mongo.service.EdgeServerService;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.system.constants.Constant;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/edge/server")
public class EdgeServerController {
    @Autowired
    private MongoExHelper mongoHelper;

    @Autowired
    private EdgeServerService service;

    @PostMapping("page")
    public AjaxResult getPageList(@RequestBody Map<String, Object> body) {
        try {
            Integer pageNum = (Integer) body.get(Constant.field_page_num);
            Integer pageSize = (Integer) body.get(Constant.field_page_size);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(pageNum, pageSize)) {
                throw new ServiceException("body参数缺失:entityType, pageNum, pageSize");
            }

            // 构造过滤条件
            CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
            if (body.containsKey(EdgeServerConstant.field_index_edge_id)) {
                criteriaAndWrapper.eq(EdgeServerConstant.field_index_edge_id, body.get(EdgeServerConstant.field_index_edge_id));
            }
            if (body.containsKey(EdgeServerConstant.field_index_name)) {
                criteriaAndWrapper.eq(EdgeServerConstant.field_index_name, body.get(EdgeServerConstant.field_index_name));
            }

            // 分页查询
            Page<EdgeEntity> page = new Page<>();
            page.setQueryCount(true);
            page.setCurr(pageNum);
            page.setLimit(pageSize);
            SortBuilder sortBuilder = new SortBuilder(EdgeEntity::getId, Sort.Direction.ASC);
            Page<EdgeServer> result = this.mongoHelper.findPage(criteriaAndWrapper, sortBuilder, page, EdgeServerConstant.field_table_name, EdgeServer.class);

            // 将结果返回
            Map<String, Object> data = new HashMap<>();
            data.put("total", result.getCount());
            data.put("list", result.getList());

            return AjaxResult.success(data);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("insert")
    public AjaxResult register(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get(EdgeServerConstant.field_index_edge_id);
            String name = (String) body.get(EdgeServerConstant.field_index_name);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(edgeId)) {
                throw new ServiceException("body参数缺失:edgeId");
            }

            CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
            criteriaAndWrapper.eq(EdgeServerConstant.field_index_edge_id, edgeId);
            Long count = this.mongoHelper.findCountByQuery(criteriaAndWrapper, EdgeServerConstant.field_table_name, EdgeServer.class);
            if (count > 0) {
                throw new ServiceException("该edgeId已经存在:" + edgeId);
            }

            // 检查：名称是否已经被占用
            if (!MethodUtils.hasEmpty(name)) {
                criteriaAndWrapper = new CriteriaAndWrapper();
                criteriaAndWrapper.eq(EdgeServerConstant.field_index_name, name);
                count = this.mongoHelper.findCountByQuery(criteriaAndWrapper, EdgeServerConstant.field_table_name, EdgeServer.class);
                if (count > 0) {
                    throw new ServiceException("该name已经存在:" + name);
                }
            }

            EdgeServer entity = new EdgeServer();
            entity.setEdgeId(edgeId);
            entity.setName(name);
            this.service.insert(entity);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("delete")
    public AjaxResult unregister(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get(EdgeServerConstant.field_index_edge_id);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(edgeId)) {
                throw new ServiceException("body参数缺失:edgeId");
            }

            CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
            criteriaAndWrapper.eq(EdgeServerConstant.field_index_edge_id, edgeId);
            Long count = this.mongoHelper.findCountByQuery(criteriaAndWrapper, EdgeServerConstant.field_table_name, EdgeServer.class);
            if (count > 0) {
                throw new ServiceException("该edgeId已经存在:" + edgeId);
            }

            this.service.delete(edgeId);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PutMapping("update")
    public AjaxResult update(@RequestBody Map<String, Object> body) {
        try {
            String edgeId = (String) body.get(EdgeServerConstant.field_index_edge_id);
            String name = (String) body.get(EdgeServerConstant.field_index_name);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(edgeId, name)) {
                throw new ServiceException("body参数缺失:edgeId, name");
            }

            // 检查：名称是否已经被占用
            if (!MethodUtils.hasEmpty(name)) {
                CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
                criteriaAndWrapper.eq(EdgeServerConstant.field_index_name, name);
                List<EdgeServer> list = this.mongoHelper.findListByQuery(criteriaAndWrapper, EdgeServerConstant.field_table_name, EdgeServer.class);
                if (list.size() > 1) {
                    throw new ServiceException("该name已经存在:" + name);
                }
                if (list.size() == 1 && !list.get(0).getEdgeId().equals(edgeId)) {
                    throw new ServiceException("该name已经存在:" + name);
                }
            }

            EdgeServer entity = new EdgeServer();
            entity.setEdgeId(edgeId);
            entity.setName(name);
            this.service.update(entity);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
