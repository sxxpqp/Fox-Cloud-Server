package cn.foxtech.cloud.repo.product.service;

import cn.craccd.mongoHelper.bean.UpdateBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.product.constants.ConstantRepoProduct;
import cn.foxtech.cloud.repo.product.constants.ConstantRepoProductComp;
import cn.foxtech.cloud.repo.product.entity.RepoProductComp;
import cn.foxtech.cloud.repo.product.entity.RepoProductEntity;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.uuid.UuidUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RepoProductCompService {
    @Autowired
    private MongoExHelper mongoHelper;

    private void updateComps(String productId, List<RepoProductComp> comps) {
        // 构造查询条件
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        andWrapper.eq(ConstantRepoProduct.field_uuid, productId);


        // 构造更新操作
        UpdateBuilder updateBuilder = new UpdateBuilder();
        updateBuilder.set(RepoProductEntity::getComps, comps);

        // 执行更新
        this.mongoHelper.updateFirst(andWrapper, updateBuilder, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
    }


    public void deleteCompEntity(String productId, String uuid) {
        if (MethodUtils.hasEmpty(productId, uuid)) {
            throw new ServiceException("productId, uuid 不能为空!");
        }

        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        andWrapper.eq(ConstantRepoProduct.field_uuid, productId);

        // 检查：该box是否存在
        RepoProductEntity productEntity = this.mongoHelper.findOneByQuery(andWrapper, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
        if (productEntity == null) {
            throw new ServiceException("找不到该Box实体!");
        }

        List<RepoProductComp> newList = new ArrayList<>();
        for (RepoProductComp compEntity : productEntity.getComps()) {
            if (compEntity.getUuid().equals(uuid)) {
                continue;
            }

            newList.add(compEntity);
        }
        productEntity.setComps(newList);

        // 更新comps
        this.updateComps(productId, newList);
    }

    public RepoProductComp insertCompEntity(Map<String, Object> param) {
        String productId = (String) param.get(ConstantRepoProductComp.field_product_id);
        String modelType = (String) param.get(ConstantRepoProductComp.field_model_type);
        String modelName = (String) param.get(ConstantRepoProductComp.field_model_name);
        String modelVersion = (String) param.get(ConstantRepoProductComp.field_model_version);

        if (MethodUtils.hasNull(productId, modelType, modelName, modelVersion)) {
            throw new ServiceException("body参数缺失: productId, modelType, modelName, modelVersion");
        }

        // 构造一个缺省的实体
        RepoProductComp compEntity = new RepoProductComp();
        compEntity.setUuid(UuidUtils.randomUUID());
        compEntity.setModelName(modelName);
        compEntity.setModelType(modelType);
        compEntity.setModelVersion(modelVersion);

        // 构造查询条件
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        andWrapper.eq(ConstantRepoProduct.field_uuid, productId);

        // 检查：该实体是否已经存在
        RepoProductEntity productEntity = this.mongoHelper.findOneByQuery(andWrapper, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
        if (productEntity == null) {
            throw new ServiceException("指定的 ProductEntity 不存在!");
        }

        // 添加item
        productEntity.getComps().add(compEntity);

        // 更新contents
        this.updateComps(productId, productEntity.getComps());

        return compEntity;
    }

    public RepoProductComp updateCompEntity(Map<String, Object> param) {
        String uuid = (String) param.get(ConstantRepoProductComp.field_uuid);
        String productId = (String) param.get(ConstantRepoProductComp.field_product_id);
        String modelType = (String) param.get(ConstantRepoProductComp.field_model_type);
        String modelName = (String) param.get(ConstantRepoProductComp.field_model_name);
        String modelVersion = (String) param.get(ConstantRepoProductComp.field_model_version);

        if (MethodUtils.hasNull(productId, uuid, modelType, modelName, modelVersion)) {
            throw new ServiceException("body参数缺失: productId, uuid, modelType, modelName, modelVersion");
        }

        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoProductComp::getUuid, productId);

        // 查询当前记录内容
        RepoProductEntity productEntity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoProductComp.field_collection_name, RepoProductEntity.class);
        if (productEntity == null) {
            throw new RuntimeException("不存在该实体！");
        }

        // 找到指定的项目
        RepoProductComp find = null;
        for (RepoProductComp itemEntity : productEntity.getComps()) {
            if (itemEntity.getUuid().equals(uuid)) {
                find = itemEntity;
                break;
            }
        }
        if (find == null) {
            throw new ServiceException("找不到对应的item项目");
        }

        // 构造一个缺省的实体
        find.setModelName(modelName);
        find.setModelType(modelType);
        find.setModelVersion(modelVersion);


        // 更新数据库
        this.updateComps(productId, productEntity.getComps());

        return find;
    }
}
