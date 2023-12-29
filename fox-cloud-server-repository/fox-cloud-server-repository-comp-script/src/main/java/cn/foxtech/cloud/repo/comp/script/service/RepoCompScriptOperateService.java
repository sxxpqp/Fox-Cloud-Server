package cn.foxtech.cloud.repo.comp.script.service;

import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptOperateEntity;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RepoCompScriptOperateService {
    private final Set<String> operateModes = new HashSet<>();
    private final Set<String> dataTypes = new HashSet<>();
    private final Set<String> serviceTypes = new HashSet<>();


    private Set<String> getServiceTypes() {
        if (this.serviceTypes.isEmpty()) {
            this.serviceTypes.add("device");
            this.serviceTypes.add("channel");
        }

        return this.serviceTypes;
    }

    private Set<String> getOperateModes() {
        if (this.operateModes.isEmpty()) {
            this.operateModes.add("exchange");
            this.operateModes.add("publish");
            this.operateModes.add("report");
            this.operateModes.add("keyHandler");
            this.operateModes.add("splitHandler");
        }

        return this.operateModes;
    }

    private Set<String> getDataTypes() {
        if (this.dataTypes.isEmpty()) {
            this.dataTypes.add("status");
            this.dataTypes.add("result");
            this.dataTypes.add("record");
        }

        return this.dataTypes;
    }

    public void verifyEntity(RepoCompScriptOperateEntity operateEntity) {
        if (MethodUtils.hasEmpty(operateEntity.getOperateName())) {
            throw new ServiceException("不能为空：operateName");
        }

        if (MethodUtils.hasEmpty(operateEntity.getEngineParam())) {
            throw new ServiceException("不能为空：engineParam");
        }

        if (!this.getOperateModes().contains(operateEntity.getOperateMode())) {
            throw new ServiceException("不支持的定义：operateMode");
        }

        if (!this.getDataTypes().contains(operateEntity.getDataType())) {
            throw new ServiceException("不支持的定义：dataType");
        }

        if (!this.getServiceTypes().contains(operateEntity.getServiceType())) {
            throw new ServiceException("不支持的定义：serviceType");
        }

        if (operateEntity.getServiceType().equals("channel") && (!operateEntity.getOperateMode().equals("keyHandler") && !operateEntity.getOperateMode().equals("splitHandler"))) {
            throw new ServiceException("不支持的定义：channel 只允许 keyHandler / splitHandler");
        }

        if (operateEntity.getServiceType().equals("device") && (!operateEntity.getOperateMode().equals("exchange") && !operateEntity.getOperateMode().equals("publish") && !operateEntity.getOperateMode().equals("report"))) {
            throw new ServiceException("不支持的定义：device 只允许 publish / report / exchange");
        }
    }

    public void verifyEntityList(List<RepoCompScriptOperateEntity> operateEntityList) {
        Set<String> operateNames = new HashSet<>();
        for (RepoCompScriptOperateEntity entity : operateEntityList) {
            // 验证单个实体
            this.verifyEntity(entity);

            // 验证重复性
            operateNames.add(entity.getOperateName());
        }

        if (operateNames.size() != operateEntityList.size()) {
            throw new ServiceException("参数重复: operateName");
        }
    }
}
