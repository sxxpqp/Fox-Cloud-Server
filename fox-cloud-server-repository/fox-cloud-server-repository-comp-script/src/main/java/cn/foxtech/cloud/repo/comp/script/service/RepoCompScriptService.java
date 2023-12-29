package cn.foxtech.cloud.repo.comp.script.service;

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
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScript;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScriptVersion;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptEntity;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptVersionEntity;
import cn.foxtech.common.utils.ContainerUtils;
import cn.foxtech.common.utils.bean.BeanMapUtils;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将redis的数据持久化到mongo中
 */
@Component
@RefreshScope
public class RepoCompScriptService {
    @Autowired
    private MongoExHelper mongoHelper;

    @Autowired
    private RepoCompScriptVersionService versionService;


    public void initialize() {
        List<String> indexFields = new ArrayList<>();

        // 创建数据库表：如果不存在则创建，存在则跳过
        this.mongoHelper.createCollection(ConstantRepoCompScript.field_collection_name, indexFields);
    }

    public CriteriaWrapper buildWrapper(String userName, Set<String> groupIds, Map<String, Object> param) {
        CriteriaAndWrapper andWrapper = this.buildWrapper(param);

        // 非admin用户：只能查询自己和public的数据
        if (!userName.equals("admin")) {
            // 允许查询自己所有的
            CriteriaOrWrapper orWrapper = new CriteriaOrWrapper();
            orWrapper.eq(ConstantRepoCompScript.field_owner_id, userName);

            // 也允许查询public所有的和自己groupName所有的
            orWrapper.in(ConstantRepoCompScript.field_group_id, groupIds);

            // 合并过滤条件
            andWrapper.and(orWrapper);
        }

        return andWrapper;
    }

    public CriteriaAndWrapper buildWrapper(Map<String, Object> param) {
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        if (param.containsKey(ConstantRepoCompScript.field_id)) {
            andWrapper.eq(ConstantRepoCompScript.field_id, param.get(ConstantRepoCompScript.field_id));
        }
        if (param.containsKey(ConstantRepoCompScript.field_manufacturer)) {
            andWrapper.eq(ConstantRepoCompScript.field_manufacturer, param.get(ConstantRepoCompScript.field_manufacturer));
        }
        if (param.containsKey(ConstantRepoCompScript.field_device_type)) {
            andWrapper.eq(ConstantRepoCompScript.field_device_type, param.get(ConstantRepoCompScript.field_device_type));
        }
        if (param.containsKey(ConstantRepoCompScript.field_description)) {
            andWrapper.like(ConstantRepoCompScript.field_description, (String) param.get(ConstantRepoCompScript.field_description));
        }

        // 关键词查询：从这些文本字段中查询
        if (param.containsKey(ConstantRepoCompScript.field_keyword)) {
            CriteriaOrWrapper orWrapper = new CriteriaOrWrapper();
            orWrapper.like(ConstantRepoCompScript.field_manufacturer, (String) param.get(ConstantRepoCompScript.field_keyword));
            orWrapper.like(ConstantRepoCompScript.field_device_type, (String) param.get(ConstantRepoCompScript.field_keyword));
            orWrapper.like(ConstantRepoCompScript.field_description, (String) param.get(ConstantRepoCompScript.field_keyword));
            orWrapper.like(ConstantRepoCompScript.field_owner_id, (String) param.get(ConstantRepoCompScript.field_keyword));

            andWrapper.and(orWrapper);
        }


        return andWrapper;
    }

    public List<RepoCompScriptEntity> queryEntityList(CriteriaWrapper criteriaWrapper) {
        SortBuilder sortBuilder = new SortBuilder(RepoCompScriptEntity::getWeight, Sort.Direction.DESC);
        List<RepoCompScriptEntity> result = this.mongoHelper.findListByQuery(criteriaWrapper, sortBuilder, ConstantRepoCompScript.field_collection_name, RepoCompScriptEntity.class);
        return result;
    }

    /**
     * 查询页面列表
     *
     * @param criteriaWrapper
     * @param body
     * @return
     */
    public Map<String, Object> queryPageList(CriteriaWrapper criteriaWrapper, Map<String, Object> body) {
        Integer pageNum = (Integer) body.get(Constant.field_page_num);
        Integer pageSize = (Integer) body.get(Constant.field_page_size);

        // 检查：是否至少包含以下几个参数
        if (MethodUtils.hasEmpty(pageNum, pageSize)) {
            throw new ServiceException("body参数缺失: pageNum, pageSize");
        }

        // 分页查询
        Page<EdgeEntity> page = new Page<>();
        page.setQueryCount(true);
        page.setCurr(pageNum);
        page.setLimit(pageSize);
        SortBuilder sortBuilder = new SortBuilder(RepoCompScriptEntity::getWeight, Sort.Direction.DESC);
        Page<RepoCompScriptEntity> result = this.mongoHelper.findPage(criteriaWrapper, sortBuilder, page, ConstantRepoCompScript.field_collection_name, RepoCompScriptEntity.class);


        List<Map<String, Object>> list = this.extendLastVersion(result.getList());

        // 将结果返回
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getCount());
        data.put("list", list);

        return data;
    }

    private List<Map<String, Object>> extendLastVersion(List<RepoCompScriptEntity> scriptEntityList) {
        // 组织成map获得keys
        Map<String, RepoCompScriptEntity> scriptEntityMap = ContainerUtils.buildMapByKey(scriptEntityList, RepoCompScriptEntity::getId);

        // 查询版组件的相关版本
        Map<String, Object> param = new HashMap<>();
        param.put(ConstantRepoCompScriptVersion.field_script_ids, scriptEntityMap.keySet());
        CriteriaAndWrapper andWrapper = this.versionService.buildWrapper(param);
        List<RepoCompScriptVersionEntity> versionEntityList = this.versionService.queryEntityList(andWrapper);

        // 重新按组件进行组织
        Map<String, List<RepoCompScriptVersionEntity>> versionEntityMap = ContainerUtils.buildMapByTypeAndFinalMethod(versionEntityList, RepoCompScriptVersionEntity::getScriptId, String.class);


        List<Map<String, Object>> resultList = new ArrayList<>();
        for (RepoCompScriptEntity scriptEntity : scriptEntityList) {
            Map<String, Object> map = BeanMapUtils.objectToMap(scriptEntity);
            resultList.add(map);

            // 获得组件相关的版本列表
            List<RepoCompScriptVersionEntity> versionList = versionEntityMap.getOrDefault(scriptEntity.getId(), new ArrayList<>());
            if (versionList.isEmpty()) {
                continue;
            }
            // 排序
            Collections.sort(versionList, new Comparator<RepoCompScriptVersionEntity>() {
                public int compare(RepoCompScriptVersionEntity o1, RepoCompScriptVersionEntity o2) {
                    //降序
                    return o2.getCreateTime().compareTo(o1.getCreateTime());
                }
            });

            // 获得最新的版本
            RepoCompScriptVersionEntity versionEntity = versionList.get(0);


            // 版本信息
            Map<String, Object> lastVersion = new HashMap<>();
            lastVersion.put("id", versionEntity.getId());
            lastVersion.put("createTime", versionEntity.getCreateTime());
            map.put("lastVersion", lastVersion);
        }

        return resultList;
    }

    public RepoCompScriptEntity queryEntity(String id) {
        return this.mongoHelper.findById(id, ConstantRepoCompScript.field_collection_name, RepoCompScriptEntity.class);
    }

    public void insertEntity(RepoCompScriptEntity entity) {
        if (MethodUtils.hasEmpty(entity.getOwnerId())) {
            throw new RuntimeException("ownerId不能为空！");
        }

        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoCompScriptEntity::getManufacturer, entity.getManufacturer());
        criteriaAndWrapper.eq(RepoCompScriptEntity::getDeviceType, entity.getDeviceType());

        // 检查：该模块是否已经存在
        Long count = this.mongoHelper.findCountByQuery(criteriaAndWrapper, ConstantRepoCompScript.field_collection_name, RepoCompScriptEntity.class);
        if (count > 0) {
            throw new RuntimeException("已经存在该名称的模块！");
        }

        this.mongoHelper.insert(ConstantRepoCompScript.field_collection_name, entity);
    }

    public void deleteEntity(List<String> ids) {
        if (MethodUtils.hasEmpty(ids)) {
            throw new RuntimeException("ids不能为空！");
        }

        this.mongoHelper.deleteByIds(ids, ConstantRepoCompScript.field_collection_name, RepoCompScriptEntity.class);
    }

    public void updateEntity(String userName, Map<String, Object> param) {
        String id = (String) param.get(ConstantRepoCompScript.field_id);
        if (MethodUtils.hasEmpty(id)) {
            throw new ServiceException("body参数缺失: id");
        }

        // 查询当前记录内容
        RepoCompScriptEntity entity = this.mongoHelper.findById(id, ConstantRepoCompScript.field_collection_name, RepoCompScriptEntity.class);
        if (entity == null) {
            throw new ServiceException("找不到该实体");
        }

        // 只有owner和admin允许修改
        if (!userName.equals(entity.getOwnerId()) && !userName.equals("admin")) {
            throw new ServiceException("只有admin和owner允许修改");
        }

        // 构造更新操作
        UpdateBuilder updateBuilder = new UpdateBuilder();

        if (param.containsKey(ConstantRepoCompScript.field_owner_id)) {
            updateBuilder.set(RepoCompScriptEntity::getOwnerId, param.get(ConstantRepoCompScript.field_owner_id));
        }
        if (param.containsKey(ConstantRepoCompScript.field_group_name)) {
            updateBuilder.set(RepoCompScriptEntity::getGroupName, param.get(ConstantRepoCompScript.field_group_name));
        }
        if (param.containsKey(ConstantRepoCompScript.field_manufacturer)) {
            updateBuilder.set(RepoCompScriptEntity::getManufacturer, param.get(ConstantRepoCompScript.field_manufacturer));
        }
        if (param.containsKey(ConstantRepoCompScript.field_device_type)) {
            updateBuilder.set(RepoCompScriptEntity::getDeviceType, param.get(ConstantRepoCompScript.field_device_type));
        }
        if (param.containsKey(ConstantRepoCompScript.field_description)) {
            updateBuilder.set(RepoCompScriptEntity::getDescription, param.get(ConstantRepoCompScript.field_description));
        }
        if (param.containsKey(ConstantRepoCompScript.field_commit_key)) {
            String commitKey = (String) param.get(ConstantRepoCompScript.field_commit_key);
            commitKey = SecurityUtils.encryptPassword(commitKey);
            updateBuilder.set(RepoCompScriptEntity::getCommitKey, commitKey);
        }

        // 检查是否存在更新操作：如果没有更新操作，直接去更新，会出现清空记录的问题
        if (updateBuilder.toUpdate().getUpdateObject().isEmpty()) {
            return;
        }

        // 执行更新
        this.mongoHelper.updateById(entity.getId(), updateBuilder, ConstantRepoCompScript.field_collection_name, RepoCompScriptEntity.class);
    }
}
