package cn.foxtech.cloud.repository.controller;

import cn.craccd.mongoHelper.utils.CriteriaWrapper;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScript;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptEntity;
import cn.foxtech.cloud.repo.comp.script.service.RepoCompScriptService;
import cn.foxtech.cloud.repo.group.constants.ConstantRepoGroup;
import cn.foxtech.cloud.repo.group.entity.RepoGroupEntity;
import cn.foxtech.cloud.repo.group.service.RepoGroupService;
import cn.foxtech.common.utils.bean.BeanMapUtils;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.uuid.UuidUtils;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/repository/component/script")
public class RepoCompScriptController {
    @Autowired
    private RepoCompScriptService scriptService;


    @Autowired
    private RepoGroupService groupService;

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("entities")
    public AjaxResult getEntities(@RequestBody Map<String, Object> body) {
        return this.getList(body, false);
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("page")
    public AjaxResult getPage(@RequestBody Map<String, Object> body) {
        return this.getList(body, true);
    }

    private AjaxResult getList(Map<String, Object> body, boolean isPage) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 当前用户所属的组信息
            Set<String> groupIds = this.groupService.queryGroupIds(username);

            // 构造过滤条件
            CriteriaWrapper criteriaWrapper = this.scriptService.buildWrapper(username, groupIds, body);

            List<String> filterKeys = new ArrayList<>();
            filterKeys.add(ConstantRepoCompScript.field_commit_key);

            if (isPage) {
                Map<String, Object> data = this.scriptService.queryPageList(criteriaWrapper, body);

                BeanMapUtils.filterKeys((List<Map<String, Object>>) data.get("list"), filterKeys);
                return AjaxResult.success(data);
            } else {
                List<RepoCompScriptEntity> entityList = this.scriptService.queryEntityList(criteriaWrapper);

                return AjaxResult.success(BeanMapUtils.objectToMap(entityList, filterKeys));
            }


        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("title/list")
    public AjaxResult getTitleList(@RequestBody Map<String, Object> body) {
        return this.getTitles(body, false);
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("title/page")
    public AjaxResult getTitlePage(@RequestBody Map<String, Object> body) {
        return this.getTitles(body, true);
    }

    private AjaxResult getTitles(Map<String, Object> body, boolean isPage) {
        try {
            // 构造过滤条件
            CriteriaWrapper criteriaWrapper = this.scriptService.buildWrapper(body);

            List<String> filterKeys = new ArrayList<>();
            filterKeys.add(ConstantRepoCompScript.field_commit_key);

            if (isPage) {
                Map<String, Object> data = this.scriptService.queryPageList(criteriaWrapper, body);

                data.put("list", BeanMapUtils.objectToMap((List) data.get("list"), filterKeys));
                return AjaxResult.success(data);
            } else {
                List<RepoCompScriptEntity> entityList = this.scriptService.queryEntityList(criteriaWrapper);

                return AjaxResult.success(BeanMapUtils.objectToMap(entityList, filterKeys));
            }

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }


    @RequiresPermissions("monitor:repo:query")
    @PostMapping("entity")
    public AjaxResult insert(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String manufacturer = (String) body.get(ConstantRepoCompScript.field_manufacturer);
            String deviceType = (String) body.get(ConstantRepoCompScript.field_device_type);
            String groupId = (String) body.get(ConstantRepoCompScript.field_group_id);
            String description = (String) body.get(ConstantRepoCompScript.field_description);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(groupId, manufacturer, deviceType)) {
                throw new ServiceException("body参数缺失: groupId, manufacturer, deviceType");
            }


            // 检查：群组
            String groupName = ConstantRepoGroup.value_public_group_name;
            if (!groupId.equals(ConstantRepoGroup.value_public_group_name)) {
                RepoGroupEntity groupEntity = this.groupService.queryRepoGroupEntity(groupId);
                if (groupEntity == null) {
                    throw new ServiceException("指定的群组不存在!");
                }
                groupName = groupEntity.getGroupName();
            }

            RepoCompScriptEntity entity = new RepoCompScriptEntity();
            entity.setManufacturer(manufacturer);
            entity.setDeviceType(deviceType);
            entity.setOwnerId(userName);
            entity.setGroupId(groupId);
            entity.setGroupName(groupName);
            entity.setDescription(description);
            entity.setCommitKey(UuidUtils.randomUUID());
            entity.setWeight(0);

            // 查询数据
            this.scriptService.insertEntity(entity);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequiresPermissions("monitor:repo:query")
    @PutMapping("commitKey")
    public AjaxResult updateCommitKey(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 查询数据
            this.scriptService.updateEntity(userName, body);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @DeleteMapping("entity")
    public AjaxResult delete(String id) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(id)) {
                throw new ServiceException("id不能为空!");
            }

            // 当前用户所属的组信息
            Set<String> groupIds = this.groupService.queryGroupIds(username);

            Map<String, Object> body = new HashMap<>();
            body.put(ConstantRepoCompScript.field_id, id);

            // 构造过滤条件
            CriteriaWrapper criteriaWrapper = this.scriptService.buildWrapper(username, groupIds, body);

            // 查询列表信息
            List<RepoCompScriptEntity> entityList = this.scriptService.queryEntityList(criteriaWrapper);
            if (MethodUtils.hasEmpty(entityList)) {
                throw new ServiceException("找不到对应的数据!");
            }

            // 查询数据
            List<String> ids = new ArrayList<>();
            ids.add(id);
            this.scriptService.deleteEntity(ids);


            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PutMapping("entity")
    public AjaxResult update(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 查询数据
            this.scriptService.updateEntity(userName, body);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
