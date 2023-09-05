package cn.foxtech.cloud.manager.repository.service;

import cn.craccd.mongoHelper.bean.Page;
import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.bean.UpdateBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.craccd.mongoHelper.utils.CriteriaOrWrapper;
import cn.craccd.mongoHelper.utils.CriteriaWrapper;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.repository.constants.Constant;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoComp;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoCompVer;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoRelation;
import cn.foxtech.cloud.manager.repository.entity.RepoCompEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoCompVerEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoGroupRelation;
import cn.foxtech.common.utils.md5.MD5Utils;
import cn.foxtech.common.utils.method.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

/**
 * 将redis的数据持久化到mongo中
 */
@Component
@RefreshScope
public class RepoCompService {
    @Autowired
    private MongoExHelper mongoHelper;

    @Autowired
    private RepoGroupService groupService;

    @Value("${manager.repository.repoCompService.verifyRepoCompVerEntity}")
    private boolean verifyRepoCompVerEntity;


    public void initialize() {
        List<String> indexFields = new ArrayList<>();
        indexFields.add(ConstantRepoComp.field_owner_id);
        indexFields.add(ConstantRepoComp.field_model_type);
        indexFields.add(ConstantRepoComp.field_model_name);
        indexFields.add(ConstantRepoComp.field_group_name);

        // 创建数据库表：如果不存在则创建，存在则跳过
        this.mongoHelper.createCollection(ConstantRepoComp.field_collection_name, indexFields);
    }

    /**
     * 构造过滤条件
     *
     * @param userName 当前用户
     * @param param    查询参数
     * @return 过滤条件
     */
    private CriteriaWrapper buildWrapper(String userName, Set<Object> groupNames, Map<String, Object> param) {
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        if (param.containsKey(ConstantRepoComp.field_model_type)) {
            andWrapper.eq(ConstantRepoComp.field_model_type, param.get(ConstantRepoComp.field_model_type));
        }
        if (param.containsKey(ConstantRepoComp.field_component)) {
            andWrapper.eq(ConstantRepoComp.field_component, param.get(ConstantRepoComp.field_component));
        }
        if (param.containsKey(ConstantRepoComp.field_model_name)) {
            andWrapper.like(ConstantRepoComp.field_model_name, (String) param.get(ConstantRepoComp.field_model_name));
        }

        // 非admin用户：只能查询自己和public的数据
        if (!userName.equals("admin")) {
            // 允许查询自己所有的
            CriteriaOrWrapper orWrapper = new CriteriaOrWrapper();
            orWrapper.eq(ConstantRepoComp.field_owner_id, userName);

            // 也允许查询public所有的和自己groupName所有的
            Set<Object> groups = new HashSet<>();
            groups.add("public");
            groups.addAll(groupNames);
            orWrapper.in(ConstantRepoComp.field_group_name, groups);

            // 合并过滤条件
            andWrapper.and(orWrapper);
        }

        return andWrapper;
    }


    public Map<String, Object> queryPageList(String userName, Map<String, Object> body) {
        Integer pageNum = (Integer) body.get(Constant.field_page_num);
        Integer pageSize = (Integer) body.get(Constant.field_page_size);

        // 检查：是否至少包含以下几个参数
        if (MethodUtils.hasEmpty(pageNum, pageSize)) {
            throw new ServiceException("body参数缺失:entityType, pageNum, pageSize");
        }

        // 当前用户所属的组信息
        Set<Object> groupNames = new HashSet<>();
        RepoGroupRelation relation = this.groupService.queryGroupRelation(ConstantRepoRelation.value_direct_user2group, userName);
        if (relation != null) {
            groupNames.addAll(relation.getObjects());
        }

        // 构造查询过滤器
        CriteriaWrapper criteriaWrapper = this.buildWrapper(userName, groupNames, body);

        // 分页查询
        Page<EdgeEntity> page = new Page<>();
        page.setQueryCount(true);
        page.setCurr(pageNum);
        page.setLimit(pageSize);
        SortBuilder sortBuilder = new SortBuilder(EdgeEntity::getId, Sort.Direction.ASC);
        Page<RepoCompEntity> result = this.mongoHelper.findPage(criteriaWrapper, sortBuilder, page, ConstantRepoComp.field_collection_name, RepoCompEntity.class);

        // 将结果返回
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getCount());
        data.put("list", result.getList());

        // 验证版本:该操作通过 nacos 上的配置开关切换，来动态启动
        if (this.verifyRepoCompVerEntity) {
            this.verifyRepoCompVerEntity(result.getList());
        }

        // 更新lastVersion信息
        this.extendLastVersion(result.getList());

        return data;
    }

    private void extendLastVersion(List<RepoCompEntity> entityList) {
        for (RepoCompEntity entity : entityList) {
            long lastVersion = 0L;

            // 找最大版本号的版本
            for (RepoCompVerEntity verEntity : entity.getVersions()) {
                long version = this.convertLong(verEntity.getVersion());
                if (version > lastVersion) {
                    lastVersion = version;
                    entity.setLastVersion(verEntity);
                }
            }

            if (lastVersion == 0L) {
                continue;
            }

            // 在多个最大版本号的版本中，有效找master版本
            for (RepoCompVerEntity verEntity : entity.getVersions()) {
                long version = this.convertLong(verEntity.getVersion());
                if (version != lastVersion) {
                    continue;
                }

                if (ConstantRepoCompVer.value_stage_master.equals(verEntity.getStage())) {
                    entity.setLastVersion(verEntity);
                }
            }
        }
    }

    public List<RepoCompEntity> queryEntityList(String userName, Map<String, Object> body) {
        // 当前用户所属的组信息
        Set<Object> groupNames = new HashSet<>();
        RepoGroupRelation relation = this.groupService.queryGroupRelation(ConstantRepoRelation.value_direct_user2group, userName);
        if (relation != null) {
            groupNames.addAll(relation.getObjects());
        }

        // 构造查询过滤器
        CriteriaWrapper criteriaWrapper = this.buildWrapper(userName, groupNames, body);

        SortBuilder sortBuilder = new SortBuilder(EdgeEntity::getId, Sort.Direction.ASC);
        List<RepoCompEntity> entityList = this.mongoHelper.findListByQuery(criteriaWrapper, sortBuilder, ConstantRepoComp.field_collection_name, RepoCompEntity.class);

        // 填充lastVersion信息
        this.extendLastVersion(entityList);

        // 剔除版本信息为空的数据
        List<RepoCompEntity> result = new ArrayList<>();
        for (RepoCompEntity entity : entityList) {
            if (MethodUtils.hasEmpty(entity.getVersions())) {
                continue;
            }

            result.add(entity);
        }

        return result;
    }

    public long newLastMasterVersion(List<RepoCompVerEntity> versions) {
        // 找到最大的版本号
        long lastVersion = 0L;
        for (RepoCompVerEntity verEntity : versions) {
            long version = this.convertLong(verEntity.getVersion());
            if (version > lastVersion) {
                lastVersion = version;
            }
        }

        // 当前最新版本号
        if (lastVersion > 0L) {
            // 检查：是否存在master版本
            boolean hasMaster = false;
            for (RepoCompVerEntity verEntity : versions) {
                long version = this.convertLong(verEntity.getVersion());
                if (version != lastVersion) {
                    continue;
                }

                if (ConstantRepoCompVer.value_stage_master.equals(verEntity.getStage())) {
                    hasMaster = true;
                    break;
                }
            }

            // 如果已经存在master版本，那么就分配下一个为master的版本号，否则就为这个版本的master版本号
            if (hasMaster) {
                lastVersion += 1;
            }

            return lastVersion;
        } else {
            lastVersion = this.convertLong("1.0.0");
            return lastVersion;
        }
    }

    public RepoCompEntity queryRepoCompEntity(String modelType, String modelName) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, modelType);
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, modelName);

        // 检查：该模块是否已经存在
        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
    }

    public void insertRepoCompEntity(RepoCompEntity entity) {
        if (MethodUtils.hasEmpty(entity.getOwnerId())) {
            throw new RuntimeException("ownerId不能为空！");
        }

        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, entity.getModelType());
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, entity.getModelName());

        // 检查：该模块是否已经存在
        Long count = this.mongoHelper.findCountByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
        if (count > 0) {
            throw new RuntimeException("已经存在该名称的模块！");
        }

        this.mongoHelper.insert(ConstantRepoComp.field_collection_name, entity);
    }

    public void updateRepoCompEntity(String userName, Map<String, Object> param) {
        String modelName = (String) param.get(ConstantRepoComp.field_model_name);
        String modelType = (String) param.get(ConstantRepoComp.field_model_type);

        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, modelType);
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, modelName);

        // 查询当前记录内容
        RepoCompEntity compEntity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
        if (compEntity == null) {
            throw new RuntimeException("不存在该模块！");
        }

        // 构造更新操作
        UpdateBuilder updateBuilder = new UpdateBuilder();
        if (userName.equals("admin")) {
            // 只有管理员允许修改这些内容
            if (param.containsKey(ConstantRepoComp.field_owner_id)) {
                updateBuilder.set(RepoCompEntity::getOwnerId, param.get(ConstantRepoComp.field_owner_id));
            }
            if (param.containsKey(ConstantRepoComp.field_group_name)) {
                updateBuilder.set(RepoCompEntity::getGroupName, param.get(ConstantRepoComp.field_group_name));
            }
            if (param.containsKey(ConstantRepoComp.field_component)) {
                updateBuilder.set(RepoCompEntity::getComponent, param.get(ConstantRepoComp.field_component));
            }
            if (param.containsKey(ConstantRepoComp.field_description)) {
                updateBuilder.set(RepoCompEntity::getDescription, param.get(ConstantRepoComp.field_description));
            }
        }

        // 非admin用户，只允许修改自己归属的内容
        if (!compEntity.getOwnerId().equals(userName)) {
            throw new RuntimeException("没有权限删除该模块：只允许owner和admin删除该模块!");
        }

        if (param.containsKey(ConstantRepoComp.field_description)) {
            updateBuilder.set(RepoCompEntity::getDescription, param.get(ConstantRepoComp.field_description));
        }

        // 检查是否存在更新操作：如果没有更新操作，直接去更新，会出现清空记录的问题
        if (updateBuilder.toUpdate().getUpdateObject().isEmpty()) {
            return;
        }

        // 执行更新
        this.mongoHelper.updateFirst(criteriaAndWrapper, updateBuilder, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
    }

    public Long convertLong(String version) {
        String[] items = version.split("\\.");
        if (items.length != 3) {
            throw new RuntimeException("版本号必须为:xx.xx.xx格式，例如，1.0.2");
        }
        long result = 0L;
        for (String item : items) {
            result = result * 100 + Integer.parseInt(item);
        }

        return result;
    }

    public String convertVersion(Long version) {
        return version / 10000L + "." + version % 10000L / 100L + "." + version % 10000L % 100L;
    }

    public void deleteRepoCompEntity(String userName, String modelName, String modelType) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, modelType);
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, modelName);

        if (!userName.equals("admin")) {
            // 检查：该模块是否已经存在
            RepoCompEntity compEntity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
            if (compEntity == null) {
                throw new RuntimeException("不存在该模块！");
            }

            if (!compEntity.getOwnerId().equals(userName)) {
                throw new RuntimeException("没有权限删除该模块：只允许owner和admin删除该模块!");
            }
        }

        // 检查：该模块是否已经存在
        this.mongoHelper.deleteByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
    }

    public void updateRepoCompVerEntity(RepoCompEntity compEntity) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, compEntity.getModelType());
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, compEntity.getModelName());


        // 更新数据库信息
        UpdateBuilder updateBuilder = new UpdateBuilder();
        updateBuilder.set(RepoCompEntity::getVersions, compEntity.getVersions());
        this.mongoHelper.updateFirst(criteriaAndWrapper, updateBuilder, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
    }

    public void verifyRepoCompVerEntity(List<RepoCompEntity> entityList) {
        for (RepoCompEntity compEntity : entityList) {
            // 验证是否需要更新状态
            boolean isDirty = false;
            for (RepoCompVerEntity verEntity : compEntity.getVersions()) {
                isDirty |= !this.verifyRepoCompVerEntity(compEntity.getModelType(), compEntity.getModelName(), verEntity);
            }
            if (!isDirty) {
                continue;
            }


            // 构造过滤器
            CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
            criteriaAndWrapper.eq(RepoCompEntity::getModelType, compEntity.getModelType());
            criteriaAndWrapper.eq(RepoCompEntity::getModelName, compEntity.getModelName());

            // 更新数据库信息
            UpdateBuilder updateBuilder = new UpdateBuilder();
            updateBuilder.set(RepoCompEntity::getVersions, compEntity.getVersions());
            this.mongoHelper.updateFirst(criteriaAndWrapper, updateBuilder, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
        }

    }

    private boolean verifyRepoCompVerEntity(String modelType, String modelName, RepoCompVerEntity verEntity) {
        try {
            // 验证版本阶段:如果为空，那么补充默认值master
            if (MethodUtils.hasEmpty(verEntity.getStage())) {
                verEntity.setStage(ConstantRepoCompVer.value_stage_master);
                return false;
            }

            // 检查：是否已经下载
            File file = new File("");
            String absolutePath = file.getAbsolutePath();
            String tarFileName = absolutePath + "/repository/" + modelType + "/" + modelName + "/" + verEntity.getVersion() + "/" + verEntity.getPathName();

            // 检查：文件是否存在
            File tarFile = new File(tarFileName);
            if (!tarFile.exists()) {
                verEntity.setMd5("");
                verEntity.setFileSize(0);
                return false;
            }

            // 获得文件的MD5和大小
            String md5 = MD5Utils.getMD5Txt(tarFile);
            long fileSize = tarFile.length();

            // 验证MD5和文件大小
            if (!md5.equals(verEntity.getMd5()) || fileSize != verEntity.getFileSize()) {
                verEntity.setMd5(md5);
                verEntity.setFileSize(fileSize);
                return false;
            }


            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public RepoCompVerEntity queryRepoCompVerEntity(String userName, String modelName, String modelType, String version, String stage) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, modelType);
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, modelName);

        // 检查：该模块是否已经存在
        RepoCompEntity compEntity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
        if (compEntity == null) {
            throw new RuntimeException("不存在该模块！");
        }

        if (!compEntity.getGroupName().equals("public") && !userName.equals("admin") && !compEntity.getOwnerId().equals(userName)) {
            throw new RuntimeException("这是私有模块：只允许owner和admin查询该模块!");
        }

        for (RepoCompVerEntity verEntity : compEntity.getVersions()) {
            if (verEntity.getVersion().equals(version) && verEntity.getStage().equals(stage)) {
                return verEntity;
            }
        }

        return null;
    }
}
