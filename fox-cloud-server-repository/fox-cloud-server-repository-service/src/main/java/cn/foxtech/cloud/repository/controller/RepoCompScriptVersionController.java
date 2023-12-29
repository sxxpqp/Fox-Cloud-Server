package cn.foxtech.cloud.repository.controller;

import cn.craccd.mongoHelper.utils.CriteriaWrapper;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScript;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScriptOperate;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScriptVersion;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptEntity;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptOperateEntity;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptVersionEntity;
import cn.foxtech.cloud.repo.comp.script.service.RepoCompScriptService;
import cn.foxtech.cloud.repo.comp.script.service.RepoCompScriptVersionService;
import cn.foxtech.cloud.repo.group.service.RepoGroupService;
import cn.foxtech.common.utils.bean.BeanMapUtils;
import cn.foxtech.common.utils.json.JsonUtils;
import cn.foxtech.common.utils.method.MethodUtils;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/repository/component/script/version")
public class RepoCompScriptVersionController {
    @Autowired
    private RepoCompScriptService scriptService;

    @Autowired
    private RepoCompScriptVersionService scriptVersionService;

    @Autowired
    private RepoGroupService groupService;

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("page")
    public AjaxResult getVersionPage(@RequestBody Map<String, Object> body) {
        return this.getList(body, true);
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("entities")
    public AjaxResult getVersionList(@RequestBody Map<String, Object> body) {
        return this.getList(body, false);
    }

    public AjaxResult getList(Map<String, Object> body, boolean isPage) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String scriptId = (String) body.get(ConstantRepoCompScriptVersion.field_script_id);

            if (MethodUtils.hasEmpty(scriptId)) {
                throw new ServiceException("body参数缺失: scriptId");
            }

            // 查询实体
            RepoCompScriptEntity scriptEntity = this.scriptService.queryEntity(scriptId);
            if (scriptEntity == null) {
                throw new ServiceException("找不到对应的实体");
            }

            // 当前用户所属的组信息
            Set<String> groupIds = this.groupService.queryGroupIds(username);

            // 检查权限
            if (!username.equals(scriptEntity.getOwnerId()) // 作者有权限
                    && !groupIds.contains(scriptEntity.getGroupId()) // 群组成员有权限
                    && !username.equals("admin")// 管理员有权限
            ) {
                throw new ServiceException("只有 admin / owner / group member 有权限查看");
            }

            // 构造过滤器
            CriteriaWrapper criteriaWrapper = this.scriptVersionService.buildWrapper(body);

            if (isPage) {
                // 查询主数据
                Map<String, Object> data = this.scriptVersionService.queryPageList(criteriaWrapper, body);

                // 扩展信息
                Map<String, Object> result = this.extend(data, scriptEntity);

                return AjaxResult.success(result);
            } else {
                // 查询主数据
                List<RepoCompScriptVersionEntity> entityList = this.scriptVersionService.queryEntityList(criteriaWrapper);

                // 扩展信息
                List<Map<String, Object>> result = this.extend(entityList, scriptEntity);

                return AjaxResult.success(result);
            }


        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    private Map<String, Object> extend(Map<String, Object> data, RepoCompScriptEntity scriptEntity) throws IOException {
        // 扩展信息
        String json = JsonUtils.buildJson(data);
        Map<String, Object> result = JsonUtils.buildObject(json, Map.class);
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        for (Map<String, Object> map : list) {
            map.put(ConstantRepoCompScript.field_manufacturer, scriptEntity.getManufacturer());
            map.put(ConstantRepoCompScript.field_device_type, scriptEntity.getDeviceType());
        }

        return result;
    }

    private List<Map<String, Object>> extend(List<RepoCompScriptVersionEntity> list, RepoCompScriptEntity scriptEntity) throws IOException {
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (RepoCompScriptVersionEntity entity : list) {
            Map<String, Object> map = BeanMapUtils.objectToMap(entity);
            map.put(ConstantRepoCompScript.field_manufacturer, scriptEntity.getManufacturer());
            map.put(ConstantRepoCompScript.field_device_type, scriptEntity.getDeviceType());
            map.put(ConstantRepoCompScript.field_group_id, scriptEntity.getGroupId());
            map.put(ConstantRepoCompScript.field_group_name, scriptEntity.getGroupName());
            resultList.add(map);
        }

        return resultList;
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("operate/entities")
    public AjaxResult getOperateList(@RequestBody Map<String, Object> body) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String versionId = (String) body.get(ConstantRepoCompScriptOperate.field_version_id);

            if (MethodUtils.hasEmpty(versionId)) {
                throw new ServiceException("body参数缺失: versionId");
            }

            RepoCompScriptVersionEntity versionEntity = this.scriptVersionService.queryVersionEntity(versionId);
            if (versionEntity == null) {
                throw new ServiceException("找不到对应的版本实体");
            }

            // 查询实体
            RepoCompScriptEntity scriptEntity = this.scriptService.queryEntity(versionEntity.getScriptId());
            if (scriptEntity == null) {
                throw new ServiceException("找不到对应的组件实体");
            }

            // 当前用户所属的组信息
            Set<String> groupIds = this.groupService.queryGroupIds(username);

            // 检查权限
            if (!username.equals(scriptEntity.getOwnerId()) // 作者有权限
                    && !groupIds.contains(scriptEntity.getGroupId()) // 群组成员有权限
                    && !username.equals("admin")// 管理员有权限
            ) {
                throw new ServiceException("只有 admin / owner / group member 有权限查看");
            }

            List<Map<String, Object>> mapList = new ArrayList<>();
            for (RepoCompScriptOperateEntity operateEntity : versionEntity.getOperates()) {
                String json = JsonUtils.buildJson(operateEntity);
                Map<String, Object> map = JsonUtils.buildObject(json, Map.class);
                map.put(ConstantRepoCompScriptOperate.field_version_id, versionEntity.getId());
                map.put(ConstantRepoCompScriptVersion.field_script_id, scriptEntity.getId());
                map.put(ConstantRepoCompScript.field_manufacturer, scriptEntity.getManufacturer());
                map.put(ConstantRepoCompScript.field_device_type, scriptEntity.getDeviceType());
                mapList.add(map);
            }

            return AjaxResult.success(mapList);


        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("operate/entity")
    public AjaxResult getOperateEntity(@RequestBody Map<String, Object> body) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String versionId = (String) body.get(ConstantRepoCompScriptOperate.field_version_id);
            String operateId = (String) body.get(ConstantRepoCompScriptOperate.field_operate_id);

            if (MethodUtils.hasEmpty(versionId, operateId)) {
                throw new ServiceException("body参数缺失: versionId, operateId");
            }

            RepoCompScriptVersionEntity versionEntity = this.scriptVersionService.queryVersionEntity(versionId);
            if (versionEntity == null) {
                throw new ServiceException("找不到对应的版本实体");
            }

            // 查询实体
            RepoCompScriptEntity scriptEntity = this.scriptService.queryEntity(versionEntity.getScriptId());
            if (scriptEntity == null) {
                throw new ServiceException("找不到对应的组件实体");
            }

            // 当前用户所属的组信息
            Set<String> groupIds = this.groupService.queryGroupIds(username);

            // 检查权限
            if (!username.equals(scriptEntity.getOwnerId()) // 作者有权限
                    && !groupIds.contains(scriptEntity.getGroupId()) // 群组成员有权限
                    && !username.equals("admin")// 管理员有权限
            ) {
                throw new ServiceException("只有 admin / owner / group member 有权限查看");
            }

            for (RepoCompScriptOperateEntity operateEntity : versionEntity.getOperates()) {
                if (operateId.equals(operateEntity.getOperateId())) {
                    String json = JsonUtils.buildJson(operateEntity);
                    Map<String, Object> map = JsonUtils.buildObject(json, Map.class);
                    map.put(ConstantRepoCompScriptOperate.field_version_id, versionEntity.getId());
                    map.put(ConstantRepoCompScriptVersion.field_script_id, scriptEntity.getId());
                    map.put(ConstantRepoCompScript.field_manufacturer, scriptEntity.getManufacturer());
                    map.put(ConstantRepoCompScript.field_device_type, scriptEntity.getDeviceType());
                    return AjaxResult.success(map);
                }
            }

            return AjaxResult.error("找不到该操作实体");


        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("entity")
    public AjaxResult createVersion(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 提交代码
            this.scriptVersionService.uploadVersionEntity(userName, body);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
