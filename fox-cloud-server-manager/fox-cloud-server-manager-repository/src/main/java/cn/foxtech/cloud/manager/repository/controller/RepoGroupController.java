package cn.foxtech.cloud.manager.repository.controller;

import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.repository.constants.Constant;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoGroup;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoRelation;
import cn.foxtech.cloud.manager.repository.entity.RepoGroupEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoGroupRelation;
import cn.foxtech.cloud.manager.repository.service.RepoGroupService;
import cn.foxtech.common.utils.method.MethodUtils;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.model.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/repository/group")
public class RepoGroupController {
    @Autowired
    private RepoGroupService groupService;

    @Autowired
    private RemoteUserService remoteUserService;

    @PostMapping("page")
    public AjaxResult getPage(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            Integer pageNum = (Integer) body.get(Constant.field_page_num);
            Integer pageSize = (Integer) body.get(Constant.field_page_size);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(pageNum, pageSize)) {
                throw new ServiceException("body参数缺失:pageNum, pageSize");
            }

            // 查询数据
            Map<String, Object> data = this.groupService.queryPageList(userName, body);

            return AjaxResult.success(data);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("groupName")
    public AjaxResult getGroupNames() {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 查询该用户的所有属组
            Set<String> groupNames = new HashSet<>();
            groupNames.add("public");
            RepoGroupRelation relation = this.groupService.queryGroupRelation(ConstantRepoRelation.value_direct_user2group, userName);
            if (relation != null) {
                groupNames.addAll(relation.getObjects());
            }

            return AjaxResult.success(groupNames);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("entity")
    public AjaxResult insertGroup(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String groupName = (String) body.get(ConstantRepoGroup.field_group_name);
            String description = (String) body.get(ConstantRepoGroup.field_group_description);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(groupName)) {
                throw new ServiceException("body参数缺失:groupName");
            }

            // 忽略大小写
            groupName = groupName.toLowerCase();

            RepoGroupEntity entity = new RepoGroupEntity();
            entity.setOwnerId(userName);
            entity.setGroupName(groupName);
            entity.setDescription(description);
            entity.setValid(true);

            //新建分组
            this.groupService.insertRepoGroupEntity(entity);

            // 添加用户成员
            this.groupService.insertRepoGroupRelation(groupName, userName);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @DeleteMapping("entity")
    public AjaxResult deleteGroup(String groupName) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            if (MethodUtils.hasEmpty(groupName)) {
                throw new RuntimeException("groupName不能为空！");
            }

            RepoGroupEntity entity = this.groupService.queryRepoGroupEntity(groupName);
            if (entity == null) {
                throw new RuntimeException("该分组不存在！");
            }

            // 检查：是否有权限
            if (!username.equals("admin") && !username.equals(entity.getOwnerId())) {
                throw new RuntimeException("只有admin和owner才可以删除该分组");
            }

            this.groupService.deleteRepoGroupEntity(groupName);
            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @DeleteMapping("member")
    public AjaxResult deleteRelation(String groupName, String member) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            if (MethodUtils.hasEmpty(groupName, member)) {
                throw new RuntimeException("groupName, member 不能为空！");
            }

            // 检查：该模块是否已经存在
            RepoGroupEntity entity = this.groupService.queryRepoGroupEntity(groupName);
            if (entity == null) {
                throw new RuntimeException("不存在该名称的分组！");
            }

            // 检查：是否有权限
            if (!username.equals("admin") && !username.equals(entity.getOwnerId())) {
                throw new RuntimeException("只有admin和owner才可以删除该分组");
            }

            // 检查：该member是否为owner
            if (member.equals(entity.getOwnerId())) {
                throw new RuntimeException("该member是owner");
            }

            // 删除双向关系
            this.groupService.deleteRepoGroupRelation(groupName, member);
            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("member")
    public AjaxResult insertRelation(@RequestBody Map<String, Object> body) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String groupName = (String) body.get(ConstantRepoGroup.field_group_name);
            String member = (String) body.get(ConstantRepoGroup.field_member);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(groupName, member)) {
                throw new ServiceException("body参数缺失:groupName, member");
            }

            // 检查：该用户是否存在
            R<LoginUser> userResult = this.remoteUserService.getUserInfo(member, SecurityConstants.INNER);
            if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData())) {
                throw new ServiceException("系统中不存在该账号：" + member);
            }
            if (R.SUCCESS != userResult.getCode()) {
                throw new ServiceException(userResult.getMsg());
            }

            // 检查：该模块是否已经存在
            RepoGroupEntity entity = this.groupService.queryRepoGroupEntity(groupName);
            if (entity == null) {
                throw new RuntimeException("不存在该名称的分组！");
            }

            // 检查：是否有权限
            if (!username.equals("admin") && !username.equals(entity.getOwnerId())) {
                throw new RuntimeException("只有admin和owner才可以添加member");
            }

            // 添加用户成员
            this.groupService.insertRepoGroupRelation(groupName, member);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

}
