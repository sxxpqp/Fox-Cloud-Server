package cn.foxtech.cloud.repo.comp.files.service;

import cn.craccd.mongoHelper.bean.Page;
import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.bean.UpdateBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.craccd.mongoHelper.utils.CriteriaOrWrapper;
import cn.craccd.mongoHelper.utils.CriteriaWrapper;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.constant.Constant;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.comp.files.constants.ConstantRepoComp;
import cn.foxtech.cloud.repo.comp.files.constants.ConstantRepoCompVer;
import cn.foxtech.cloud.repo.comp.files.entity.RepoCompEntity;
import cn.foxtech.cloud.repo.comp.files.entity.RepoCompVerEntity;
import cn.foxtech.cloud.repo.group.constants.ConstantRepoGroup;
import cn.foxtech.cloud.repo.group.entity.RepoGroupEntity;
import cn.foxtech.cloud.repo.group.service.RepoGroupService;
import cn.foxtech.common.utils.md5.MD5Utils;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.security.SecurityUtils;
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
    /**
     * 正则表达式：英文字母+ ‘-’和‘_’字符
     */
    public final String REGEX_PATTERN = "^([a-zA-Z0-9]+-?+_?)+[a-zA-Z0-9]{1,255}$";
    @Autowired
    private MongoExHelper mongoHelper;
    @Autowired
    private RepoGroupService groupService;
    @Autowired
    private RepoCompVerService compVerService;
    @Value("${manager.repository.repoCompService.verifyRepoCompVerEntity}")
    private boolean verifyRepoCompVerEntity;

    public void initialize() {
        List<String> indexFields = new ArrayList<>();
        indexFields.add(ConstantRepoComp.field_owner_id);
        indexFields.add(ConstantRepoComp.field_model_type);
        indexFields.add(ConstantRepoComp.field_model_name);
        indexFields.add(ConstantRepoComp.field_group_id);
        indexFields.add(ConstantRepoComp.field_group_name);
        indexFields.add(ConstantRepoComp.field_weight);

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
    public CriteriaAndWrapper buildWrapper(String userName, Set<String> groupIds, Map<String, Object> param) {
        CriteriaAndWrapper andWrapper = this.buildWrapper(param);

        // 非admin用户：只能查询自己和public的数据
        if (!userName.equals("admin")) {
            // 允许查询自己所有的
            CriteriaOrWrapper orWrapper = new CriteriaOrWrapper();
            orWrapper.eq(ConstantRepoComp.field_owner_id, userName);

            // 也允许查询public所有的和自己groupName所有的
            orWrapper.in(ConstantRepoComp.field_group_id, groupIds);

            // 合并过滤条件
            andWrapper.and(orWrapper);
        }

        return andWrapper;
    }

    public CriteriaAndWrapper buildWrapper(Map<String, Object> param) {
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        if (param.containsKey(ConstantRepoComp.field_id)) {
            andWrapper.eq(ConstantRepoComp.field_id, param.get(ConstantRepoComp.field_id));
        }
        if (param.containsKey(ConstantRepoComp.field_model_type)) {
            andWrapper.eq(ConstantRepoComp.field_model_type, param.get(ConstantRepoComp.field_model_type));
        }
        if (param.containsKey(ConstantRepoComp.field_model_version)) {
            andWrapper.eq(ConstantRepoComp.field_model_version, param.get(ConstantRepoComp.field_model_version));
        }
        if (param.containsKey(ConstantRepoComp.field_component)) {
            andWrapper.eq(ConstantRepoComp.field_component, param.get(ConstantRepoComp.field_component));
        }
        if (param.containsKey(ConstantRepoComp.field_model_name)) {
            andWrapper.eq(ConstantRepoComp.field_model_name, param.get(ConstantRepoComp.field_model_name));
        }
        if (param.containsKey(ConstantRepoComp.field_description)) {
            andWrapper.like(ConstantRepoComp.field_description, (String) param.get(ConstantRepoComp.field_description));
        }

        return andWrapper;
    }

    public Map<String, Object> queryPageList(CriteriaWrapper criteriaWrapper, Map<String, Object> body) {
        Integer pageNum = (Integer) body.get(Constant.field_page_num);
        Integer pageSize = (Integer) body.get(Constant.field_page_size);

        // 检查：是否至少包含以下几个参数
        if (MethodUtils.hasEmpty(pageNum, pageSize)) {
            throw new ServiceException("body参数缺失:entityType, pageNum, pageSize");
        }


        // 分页查询
        Page<EdgeEntity> page = new Page<>();
        page.setQueryCount(true);
        page.setCurr(pageNum);
        page.setLimit(pageSize);
        SortBuilder sortBuilder = new SortBuilder(RepoCompEntity::getWeight, Sort.Direction.DESC);
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
        for (RepoCompEntity entity : result.getList()) {
            this.extendLastVersion(entity);
        }

        return data;
    }


    public void extendLastVersion(RepoCompEntity entity) {
        {
            long lastVersion = 0L;

            // 找最大版本号的版本
            for (RepoCompVerEntity verEntity : entity.getVersions()) {
                long version = this.compVerService.convertLong(verEntity.getVersion());
                if (version > lastVersion) {
                    lastVersion = version;
                    entity.setLastVersion(verEntity);
                }
            }

            if (lastVersion == 0L) {
                return;
            }

            // 在多个最大版本号的版本中，有效找master版本
            for (RepoCompVerEntity verEntity : entity.getVersions()) {
                long version = this.compVerService.convertLong(verEntity.getVersion());
                if (version != lastVersion) {
                    continue;
                }

                if (ConstantRepoCompVer.value_stage_master.equals(verEntity.getStage())) {
                    entity.setLastVersion(verEntity);
                }
            }
        }
    }

    public RepoCompVerEntity makeVersion(RepoCompEntity compEntity, String component, String fileName, String md5Txt, long fileSize) {
        long time = System.currentTimeMillis();


        if (compEntity.getModelType().equals("decoder")) {
            // 场景1：decoder的版本信息已经存在老的配置项目，则更新该项目的内容
            for (RepoCompVerEntity verEntity : compEntity.getVersions()) {
                if (verEntity.getVersion().equals(compEntity.getJarEntity().getProperty().getVersion())) {
                    verEntity.setStage(ConstantRepoCompVer.value_stage_master);
                    verEntity.setComponent(component);
                    verEntity.setDescription("");
                    verEntity.setCreateTime(time);
                    verEntity.setUpdateTime(time);
                    verEntity.setPathName(fileName);
                    verEntity.setMd5(md5Txt);
                    verEntity.setFileSize(fileSize);
                    return verEntity;
                }
            }

            // 场景2：decoder的版本信息不存在该项目，则新增一个版本号项目，并且该版本好使用的是jar文件的版本好
            RepoCompVerEntity verEntity = new RepoCompVerEntity();
            verEntity.setVersion(compEntity.getJarEntity().getProperty().getVersion());
            verEntity.setStage(ConstantRepoCompVer.value_stage_master);
            verEntity.setComponent(component);
            verEntity.setDescription("");
            verEntity.setCreateTime(time);
            verEntity.setUpdateTime(time);
            verEntity.setPathName(fileName);
            verEntity.setMd5(md5Txt);
            verEntity.setFileSize(fileSize);

            // 追加版本
            compEntity.getVersions().add(0, verEntity);

            return verEntity;
        } else {
            // 场景3：非decoder的其他类型，一概新增版本项目
            long lastMasterVersion = this.newLastMasterVersion(compEntity.getVersions());

            RepoCompVerEntity verEntity = new RepoCompVerEntity();
            verEntity.setVersion(this.convertVersion(lastMasterVersion));
            verEntity.setStage(ConstantRepoCompVer.value_stage_master);
            verEntity.setComponent(component);
            verEntity.setDescription("");
            verEntity.setCreateTime(time);
            verEntity.setUpdateTime(time);
            verEntity.setPathName(fileName);
            verEntity.setMd5(md5Txt);
            verEntity.setFileSize(fileSize);

            // 追加版本
            compEntity.getVersions().add(0, verEntity);

            return verEntity;
        }

    }

    public long newLastMasterVersion(List<RepoCompVerEntity> versions) {
        // 找到最大的版本号
        long lastVersion = 0L;
        for (RepoCompVerEntity verEntity : versions) {
            long version = this.compVerService.convertLong(verEntity.getVersion());
            if (version > lastVersion) {
                lastVersion = version;
            }
        }

        // 当前最新版本号
        if (lastVersion > 0L) {
            // 检查：是否存在master版本
            boolean hasMaster = false;
            for (RepoCompVerEntity verEntity : versions) {
                long version = this.compVerService.convertLong(verEntity.getVersion());
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
            lastVersion = this.compVerService.convertLong("1.0.0");
            return lastVersion;
        }
    }

    public RepoCompEntity queryRepoCompEntity(String modelType, String modelName, String modelVersion) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, modelType);
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, modelName);
        criteriaAndWrapper.eq(RepoCompEntity::getModelVersion, modelVersion);

        // 检查：该模块是否已经存在
        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
    }

    public List<RepoCompEntity> queryEntityList(Map<String, Object> body) {
        CriteriaAndWrapper andWrapper = this.buildWrapper(body);
        return this.mongoHelper.findListByQuery(andWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
    }

    public List<RepoCompEntity> queryEntityList(CriteriaAndWrapper andWrapper) {
        SortBuilder sortBuilder = new SortBuilder(RepoCompEntity::getWeight, Sort.Direction.DESC);
        List<RepoCompEntity> entityList = this.mongoHelper.findListByQuery(andWrapper, sortBuilder, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
        return entityList;
    }

    public List<RepoCompEntity> extendAndFilter(List<RepoCompEntity> entityList) {
        // 更新lastVersion信息
        for (RepoCompEntity entity : entityList) {
            this.extendLastVersion(entity);
        }

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

    public void insertRepoCompEntity(String userName, Map<String, Object> body) {
        String groupId = (String) body.get(ConstantRepoComp.field_group_id);
        String modelName = (String) body.get(ConstantRepoComp.field_model_name);
        String modelType = (String) body.get(ConstantRepoComp.field_model_type);
        String modelVersion = (String) body.get(ConstantRepoComp.field_model_version);
        String component = (String) body.get(ConstantRepoComp.field_component);
        String description = (String) body.get(ConstantRepoComp.field_description);
        String manufacturer = (String) body.get(ConstantRepoComp.field_manufacturer);
        String deviceType = (String) body.get(ConstantRepoComp.field_device_type);

        // 检查：是否至少包含以下几个参数
        if (MethodUtils.hasEmpty(groupId, modelName, modelVersion, modelType, component)) {
            throw new ServiceException("body参数缺失: groupId, modelName, modelVersion, modelType, component");
        }

        // 检查：模块名格式
        if (!this.validateStringUsingRegex(modelName)) {
            throw new ServiceException("modelName只能包含英文字符和横杠和下划线字符");
        }
        // 检查：模块类型
        if (!this.validateModelType(modelType)) {
            throw new ServiceException("modelType 不在定义的范围内!");
        }
        // 检查：模块版本
        if (!this.validateModelVersion(modelVersion)) {
            throw new ServiceException("modelVersion 不是v1，v2这种格式v+整数的格式");
        }
        // 规范化命名：只有decoder才允许多办法，其他都是只能单一版本，也就是v1
        modelVersion = this.makeModelVersion(modelType, modelVersion);

        // 检查：群组
        String groupName = ConstantRepoGroup.value_public_group_id;
        if (!groupId.equals(ConstantRepoGroup.value_public_group_id)) {
            RepoGroupEntity groupEntity = this.groupService.queryRepoGroupEntity(groupId);
            if (groupEntity == null) {
                throw new ServiceException("指定的群组不存在!");
            }
            groupName = groupEntity.getGroupName();
        }

        RepoCompEntity entity = new RepoCompEntity();
        entity.setModelName(modelName.toLowerCase());
        entity.setModelType(modelType);
        entity.setModelVersion(modelVersion);
        entity.setOwnerId(userName);
        entity.setGroupId(groupId);
        entity.setGroupName(groupName);
        entity.setComponent(component);
        entity.setDescription(description);
        entity.setManufacturer(manufacturer);
        entity.setDeviceType(deviceType);


        if (MethodUtils.hasEmpty(entity.getOwnerId())) {
            throw new RuntimeException("ownerId不能为空！");
        }

        // CSV模板文件的验证
        if (entity.getModelType().equals("template")) {
            if (MethodUtils.hasEmpty(entity.getManufacturer(), entity.getDeviceType())) {
                throw new ServiceException("body参数缺失: manufacturer, deviceType");
            }
        }

        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, entity.getModelType());
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, entity.getModelName());
        criteriaAndWrapper.eq(RepoCompEntity::getModelVersion, entity.getModelVersion());

        // 检查：该模块是否已经存在
        Long count = this.mongoHelper.findCountByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
        if (count > 0) {
            throw new RuntimeException("已经存在该名称的模块！");
        }

        this.mongoHelper.insert(ConstantRepoComp.field_collection_name, entity);
    }

    private boolean validateModelType(String modelType) {
        if ("service".equals(modelType)) {
            return true;
        }
        if ("decoder".equals(modelType)) {
            return true;
        }
        if ("template".equals(modelType)) {
            return true;
        }
        if ("webpack".equals(modelType)) {
            return true;
        }
        return "system".equals(modelType);
    }

    private boolean validateModelVersion(String modelVersion) {
        if (!modelVersion.startsWith("v")) {
            return false;
        }
        try {
            Integer.valueOf(modelVersion.substring(1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String makeModelVersion(String modelType, String modelVersion) {
        if (!modelType.equals(ConstantRepoComp.field_value_model_type_decoder)) {
            return "v1";
        }

        return modelVersion;
    }

    /**
     * 验证文件名
     *
     * @param filename
     * @return
     */
    public boolean validateStringUsingRegex(String filename) {
        if (MethodUtils.hasEmpty(filename)) {
            return false;
        }

        return filename.matches(REGEX_PATTERN);
    }

    public void updateRepoCompEntity(String userName, Map<String, Object> param) {
        String modelName = (String) param.get(ConstantRepoComp.field_model_name);
        String modelType = (String) param.get(ConstantRepoComp.field_model_type);
        String modelVersion = (String) param.get(ConstantRepoComp.field_model_version);

        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, modelType);
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, modelName);
        criteriaAndWrapper.eq(RepoCompEntity::getModelVersion, modelVersion);


        this.updateRepoCompEntity(userName, criteriaAndWrapper, param);
    }

    public void updateRepoCompEntity(String userName, CriteriaAndWrapper criteriaAndWrapper, Map<String, Object> param) {
        // 查询当前记录内容
        RepoCompEntity compEntity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
        if (compEntity == null) {
            throw new RuntimeException("不存在该模块！");
        }

        // 只有owner和admin允许修改
        if (!userName.equals(compEntity.getOwnerId()) && !userName.equals("admin")) {
            throw new ServiceException("只有admin和owner允许修改");
        }

        // 构造更新操作
        UpdateBuilder updateBuilder = new UpdateBuilder();
        if (userName.equals("admin")) {
            // 只有管理员允许修改这些内容
            if (param.containsKey(ConstantRepoComp.field_owner_id)) {
                updateBuilder.set(RepoCompEntity::getOwnerId, param.get(ConstantRepoComp.field_owner_id));
            }
            if (param.containsKey(ConstantRepoComp.field_component)) {
                updateBuilder.set(RepoCompEntity::getComponent, param.get(ConstantRepoComp.field_component));
            }
        }

        if (compEntity.getModelType().equals("template") || compEntity.getModelType().equals("decoder")) {
            String manufacturer = (String) param.get(ConstantRepoComp.field_manufacturer);
            if (!MethodUtils.hasEmpty(manufacturer)) {
                updateBuilder.set(RepoCompEntity::getManufacturer, manufacturer);
            }

            String deviceType = (String) param.get(ConstantRepoComp.field_device_type);
            if (!MethodUtils.hasEmpty(deviceType)) {
                updateBuilder.set(RepoCompEntity::getDeviceType, deviceType);
            }
        }


        if (param.containsKey(ConstantRepoComp.field_description)) {
            updateBuilder.set(RepoCompEntity::getDescription, param.get(ConstantRepoComp.field_description));
        }
        if (param.containsKey(ConstantRepoComp.field_commit_key)) {
            String commitKey = (String) param.get(ConstantRepoComp.field_commit_key);
            commitKey = SecurityUtils.encryptPassword(commitKey);
            updateBuilder.set(RepoCompEntity::getCommitKey, commitKey);
        }

        // 检查是否存在更新操作：如果没有更新操作，直接去更新，会出现清空记录的问题
        if (updateBuilder.toUpdate().getUpdateObject().isEmpty()) {
            return;
        }

        // 执行更新
        this.mongoHelper.updateById(compEntity.getId(), updateBuilder, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
    }


    public String convertVersion(Long version) {
        return version / 10000L + "." + version % 10000L / 100L + "." + version % 10000L % 100L;
    }

    public void deleteRepoCompEntity(String userName, String modelName, String modelType, String modelVersion) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, modelType);
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, modelName);
        criteriaAndWrapper.eq(RepoCompEntity::getModelVersion, modelVersion);

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
        criteriaAndWrapper.eq(RepoCompEntity::getModelVersion, compEntity.getModelVersion());


        // 更新数据库信息
        UpdateBuilder updateBuilder = new UpdateBuilder();

        if (!MethodUtils.hasEmpty(compEntity.getVersions())) {
            updateBuilder.set(RepoCompEntity::getVersions, compEntity.getVersions());
        }
        if (!MethodUtils.hasEmpty(compEntity.getManufacturer())) {
            updateBuilder.set(RepoCompEntity::getManufacturer, compEntity.getManufacturer());
        }
        if (!MethodUtils.hasEmpty(compEntity.getDeviceType())) {
            updateBuilder.set(RepoCompEntity::getDeviceType, compEntity.getDeviceType());
        }
        if (!MethodUtils.hasEmpty(compEntity.getNamespace())) {
            updateBuilder.set(RepoCompEntity::getNamespace, compEntity.getNamespace());
        }
        if (!MethodUtils.hasEmpty(compEntity.getJarEntity())) {
            updateBuilder.set(RepoCompEntity::getJarEntity, compEntity.getJarEntity());
        }

        // 检查是否存在更新操作：如果没有更新操作，直接去更新，会出现清空记录的问题
        if (updateBuilder.toUpdate().getUpdateObject().isEmpty()) {
            return;
        }

        this.mongoHelper.updateFirst(criteriaAndWrapper, updateBuilder, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
    }

    public void extendGroupName(List<RepoCompEntity> entityList, Map<String, RepoGroupEntity> groupMap) {
        for (RepoCompEntity compEntity : entityList) {
            RepoGroupEntity groupEntity = groupMap.get(compEntity.getGroupId());
            if (groupEntity == null) {
                continue;
            }

            compEntity.setGroupName(groupEntity.getGroupName());

        }
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

    public RepoCompVerEntity queryRepoCompVerEntity(String userName, String modelName, String modelType, String modelVersion, String version, String stage) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompEntity::getModelType, modelType);
        criteriaAndWrapper.eq(RepoCompEntity::getModelName, modelName);
        criteriaAndWrapper.eq(RepoCompEntity::getModelVersion, modelVersion);

        // 检查：该模块是否已经存在
        RepoCompEntity compEntity = this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoComp.field_collection_name, RepoCompEntity.class);
        if (compEntity == null) {
            throw new RuntimeException("不存在该模块！");
        }

        if (!compEntity.getGroupName().equals(ConstantRepoGroup.value_public_group_name) && !userName.equals("admin") && !compEntity.getOwnerId().equals(userName)) {
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
