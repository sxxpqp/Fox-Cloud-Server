package cn.foxtech.cloud.repo.group.service;

import cn.foxtech.cloud.repo.group.constants.ConstantRepoGroupPermit;
import cn.foxtech.cloud.repo.group.constants.ConstantRepoRole;
import cn.foxtech.core.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RepoGroupRoleService {

    public void isPermit(String permit, Set<String> roles) {
        if (roles.contains(ConstantRepoRole.role_admin)) {
            return;
        }

        if (ConstantRepoGroupPermit.permit_group_delete.equals(permit)) {
            if (roles.contains(ConstantRepoRole.role_enterprise_user)) {
                return;
            }
        }
        if (ConstantRepoGroupPermit.permit_group_create.equals(permit)) {
            if (roles.contains(ConstantRepoRole.role_enterprise_user)) {
                return;
            }
        }

        throw new ServiceException("没有权限进行该操作!");
    }

}
