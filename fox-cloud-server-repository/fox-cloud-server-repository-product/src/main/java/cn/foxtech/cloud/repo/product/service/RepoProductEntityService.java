package cn.foxtech.cloud.repo.product.service;

import cn.craccd.mongoHelper.bean.Page;
import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.bean.UpdateBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.craccd.mongoHelper.utils.CriteriaOrWrapper;
import cn.craccd.mongoHelper.utils.CriteriaWrapper;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.constant.Constant;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.product.constants.ConstantRepoProduct;
import cn.foxtech.cloud.repo.product.entity.RepoProductEntity;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Component
public class RepoProductEntityService {
    @Autowired
    private MongoExHelper mongoHelper;

    private String absolutePath = null;


    private String getAbsolutePath() {
        if (this.absolutePath == null) {
            File file = new File("");
            this.absolutePath = file.getAbsolutePath();
        }

        return this.absolutePath;
    }

    public void initialize() {
        List<String> indexFields = new ArrayList<>();
        indexFields.add(ConstantRepoProduct.field_uuid);
        indexFields.add(ConstantRepoProduct.field_manufacturer);
        indexFields.add(ConstantRepoProduct.field_device_type);
        this.mongoHelper.createCollection(ConstantRepoProduct.field_collection_name, indexFields);
    }

    public Set<String> splitKeyWords(String keyWords) {
        // 按空格分拆多个关键字
        Set<String> keys = new HashSet<>();
        if (!MethodUtils.hasEmpty(keyWords)) {
            String[] items = keyWords.split("\\s+");
            Collections.addAll(keys, items);
        }

        return keys;
    }

    public Map<String, Object> queryProductEntityPage(Map<String, Object> body, SortBuilder sortBuilder) {
        String uuid = (String) body.get(ConstantRepoProduct.field_uuid);
        String manufacturer = (String) body.get(ConstantRepoProduct.field_manufacturer);
        String deviceType = (String) body.get(ConstantRepoProduct.field_device_type);
        String description = (String) body.get(ConstantRepoProduct.field_description);
        String words = (String) body.get(ConstantRepoProduct.field_key_word);
        Set<String> keyWords = this.splitKeyWords(words);

        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();

        // 选填参数：uuid/name/description/keyWords
        if (!MethodUtils.hasEmpty(uuid)) {
            andWrapper.eq(ConstantRepoProduct.field_uuid, uuid);
        }
        if (!MethodUtils.hasEmpty(manufacturer)) {
            andWrapper.eq(ConstantRepoProduct.field_manufacturer, manufacturer);
        }
        if (!MethodUtils.hasEmpty(deviceType)) {
            andWrapper.eq(ConstantRepoProduct.field_device_type, deviceType);
        }
        if (!MethodUtils.hasEmpty(description)) {
            andWrapper.eq(ConstantRepoProduct.field_description, description);
        }
        if (!MethodUtils.hasEmpty(keyWords)) {
            CriteriaOrWrapper orWrapper = new CriteriaOrWrapper();
            for (String keyWord : keyWords) {
                orWrapper.like(RepoProductEntity::getDeviceType, keyWord);
                orWrapper.like(RepoProductEntity::getManufacturer, keyWord);
                orWrapper.like(RepoProductEntity::getTags, keyWord);
                orWrapper.like(RepoProductEntity::getDescription, keyWord);
            }

            andWrapper.and(orWrapper);
        }

        Page<RepoProductEntity> result = this.queryProductPage(andWrapper, body, sortBuilder);

        // 将结果返回
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getCount());
        data.put("list", result.getList());
        return data;
    }

    public List<RepoProductEntity> queryProductList(SortBuilder sortBuilder) {
        return this.mongoHelper.findAll(sortBuilder, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
    }

    private Page<RepoProductEntity> queryProductPage(CriteriaWrapper criteriaWrapper, Map<String, Object> body, SortBuilder sortBuilder) {
        Integer pageNum = (Integer) body.get(Constant.field_page_num);
        Integer pageSize = (Integer) body.get(Constant.field_page_size);

        // 检查：是否至少包含以下几个参数
        if (MethodUtils.hasEmpty(pageNum, pageSize)) {
            throw new ServiceException("body参数缺失:pageNum, pageSize");
        }

        // 分页查询
        Page<RepoProductEntity> page = new Page<>();
        page.setQueryCount(true);
        page.setCurr(pageNum);
        page.setLimit(pageSize);

        return this.mongoHelper.findPage(criteriaWrapper, sortBuilder, page, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
    }

    public void insertProductEntity(RepoProductEntity entity) {
        this.mongoHelper.insert(ConstantRepoProduct.field_collection_name, entity);
    }

    public void deleteProductEntity(String uuid) {
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        andWrapper.eq(ConstantRepoProduct.field_uuid, uuid);

        this.mongoHelper.deleteByQuery(andWrapper, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
    }

    public RepoProductEntity updateProductEntity(Map<String, Object> param) {
        String uuid = (String) param.get(ConstantRepoProduct.field_uuid);
        if (MethodUtils.hasEmpty(uuid)) {
            throw new ServiceException("body参数缺失:uuid");
        }


        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoProductEntity::getUuid, uuid);

        // 查询当前记录内容
        RepoProductEntity productEntity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
        if (productEntity == null) {
            throw new RuntimeException("不存在该实体！");
        }

        // 构造更新操作
        UpdateBuilder updateBuilder = new UpdateBuilder();
        updateBuilder.set(RepoProductEntity::getUpdateTime, System.currentTimeMillis());

        // 只有管理员允许修改这些内容
        if (param.containsKey(ConstantRepoProduct.field_manufacturer)) {
            updateBuilder.set(RepoProductEntity::getManufacturer, param.get(ConstantRepoProduct.field_manufacturer));
        }
        if (param.containsKey(ConstantRepoProduct.field_device_type)) {
            updateBuilder.set(RepoProductEntity::getDeviceType, param.get(ConstantRepoProduct.field_device_type));
        }
        if (param.containsKey(ConstantRepoProduct.field_description)) {
            updateBuilder.set(RepoProductEntity::getDescription, param.get(ConstantRepoProduct.field_description));
        }
        if (param.containsKey(ConstantRepoProduct.field_image)) {
            updateBuilder.set(RepoProductEntity::getImage, param.get(ConstantRepoProduct.field_image));
        }
        if (param.containsKey(ConstantRepoProduct.field_url)) {
            updateBuilder.set(RepoProductEntity::getUrl, param.get(ConstantRepoProduct.field_url));
        }
        if (param.containsKey(ConstantRepoProduct.field_tags)) {
            updateBuilder.set(RepoProductEntity::getTags, param.get(ConstantRepoProduct.field_tags));
        }
        if (param.containsKey(ConstantRepoProduct.field_weight)) {
            updateBuilder.set(RepoProductEntity::getWeight, param.get(ConstantRepoProduct.field_weight));
        }

        // 检查是否存在更新操作：如果没有更新操作，直接去更新，会出现清空记录的问题
        if (updateBuilder.toUpdate().getUpdateObject().isEmpty()) {
            return productEntity;
        }

        // 执行更新
        this.mongoHelper.updateFirst(criteriaAndWrapper, updateBuilder, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);

        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
    }

    public RepoProductEntity queryProductEntity(String uuid) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoProductEntity::getUuid, uuid);

        // 检查：该模块是否已经存在
        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoProduct.field_collection_name, RepoProductEntity.class);
    }
}
