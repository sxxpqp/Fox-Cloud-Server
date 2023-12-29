package cn.foxtech.cloud.repo.group.service;

import cn.craccd.mongoHelper.bean.Page;
import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.bean.UpdateBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.craccd.mongoHelper.utils.CriteriaOrWrapper;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.constant.Constant;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.group.constants.ConstantRepoGroup;
import cn.foxtech.cloud.repo.group.constants.ConstantRepoRelation;
import cn.foxtech.cloud.repo.group.entity.RepoGroupEntity;
import cn.foxtech.cloud.repo.group.entity.RepoGroupRelation;
import cn.foxtech.common.utils.ContainerUtils;
import cn.foxtech.common.utils.method.MethodUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RepoGroupService {
    @Autowired
    private MongoExHelper mongoHelper;


    public void initialize() {
        List<String> indexFields1 = new ArrayList<>();
        indexFields1.add(ConstantRepoGroup.field_group_name);
        this.mongoHelper.createCollection(ConstantRepoGroup.field_collection_name, indexFields1);

        List<String> indexFields2 = new ArrayList<>();
        indexFields2.add(ConstantRepoRelation.field_name);
        this.mongoHelper.createCollection(ConstantRepoRelation.field_collection_name, indexFields2);
    }

    public CriteriaAndWrapper buildWrapper(Map<String, Object> param) {
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();

        if (param.containsKey(ConstantRepoGroup.field_ids)) {
            List<ObjectId> list = new ArrayList<>();
            Collection<String> ids = (Collection) param.get(ConstantRepoGroup.field_ids);
            for (String id : ids) {

                ObjectId objectId = new ObjectId(id);
                list.add(objectId);
            }

            andWrapper.in(RepoGroupEntity::getId, list);
        }
        if (param.containsKey(ConstantRepoGroup.field_group_name)) {
            andWrapper.eq(RepoGroupEntity::getGroupName, param.get(ConstantRepoGroup.field_group_name));
        }

        return andWrapper;
    }

    private CriteriaAndWrapper buildWrapper(String userName, Map<String, Object> param) {
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        if (userName.equals("admin")) {
            // 管理员：默认查询全部分组
            return andWrapper;
        } else {
            // 分组Owner：该用户相关的分组
            Set<String> userNames = new HashSet<>();
            userNames.add(userName);
            List<RepoGroupRelation> relations = this.queryGroupRelations(ConstantRepoRelation.value_direct_user2group, userNames);

            Set<String> groupNames = new HashSet<>();
            for (RepoGroupRelation relation : relations) {
                groupNames.addAll(relation.getObjects());
            }

            // 查询他为owner的分组和他归属member的分组
            CriteriaOrWrapper orWrapper = new CriteriaOrWrapper();
            orWrapper.eq(RepoGroupEntity::getOwnerId, userName);
            if (!groupNames.isEmpty()) {
                // 如果这个用户属于一些分组，那么他查询跟他相关的分组
                orWrapper.in(RepoGroupEntity::getGroupName, groupNames);
            }

            andWrapper.and(orWrapper);
        }


        if (param.containsKey(ConstantRepoGroup.field_group_name)) {
            andWrapper.eq(RepoGroupEntity::getGroupName, param.get(ConstantRepoGroup.field_group_name));
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

        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = this.buildWrapper(userName, body);

        // 分页查询
        Page<EdgeEntity> page = new Page<>();
        page.setQueryCount(true);
        page.setCurr(pageNum);
        page.setLimit(pageSize);
        SortBuilder sortBuilder = new SortBuilder(EdgeEntity::getId, Sort.Direction.ASC);
        Page<RepoGroupEntity> result = this.mongoHelper.findPage(criteriaAndWrapper, sortBuilder, page, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);

        // 查询成员信息
        this.extentMembers(result.getList());

        // 将结果返回
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getCount());
        data.put("list", result.getList());

        return data;
    }

    public Map<String, String> queryGroupNames(Collection<String> groupIds) {
        Set<String> ids = new HashSet<>();
        ids.addAll(groupIds);

        ids.remove(ConstantRepoGroup.value_public_group_id);

        // 查询真正的群准对象
        Map<String, String> result = new HashMap<>();
        if (!ids.isEmpty()) {
            Map<String, Object> param = new HashMap<>();
            param.put(ConstantRepoGroup.field_ids, groupIds);
            CriteriaAndWrapper criteriaAndWrapper = this.buildWrapper(param);
            // 当前用户所属的组信息
            List<RepoGroupEntity> groupEntityList = this.queryGroupEntityList(criteriaAndWrapper);
            for (RepoGroupEntity groupEntity : groupEntityList) {
                result.put(groupEntity.getId(), groupEntity.getGroupName());
            }
        }

        if (groupIds.contains(ConstantRepoGroup.value_public_group_id)) {
            result.put(ConstantRepoGroup.value_public_group_id, ConstantRepoGroup.value_public_group_name);
        }

        return result;
    }

    private void extentMembers(List<RepoGroupEntity> entityList) {
        Set<String> groupKeys = new HashSet<>();
        for (RepoGroupEntity entity : entityList) {
            groupKeys.add(entity.getId());
        }

        List<RepoGroupRelation> relations = this.queryGroupRelations(ConstantRepoRelation.value_direct_group2user, groupKeys);
        Map<String, RepoGroupRelation> map = ContainerUtils.buildMapByKey(relations, RepoGroupRelation::getKey);

        for (RepoGroupEntity entity : entityList) {
            RepoGroupRelation relation = map.get(entity.getId());
            if (relation == null) {
                continue;
            }

            for (String userName : relation.getObjects()) {
                Map<String, Object> user = new HashMap<>();
                user.put(ConstantRepoGroup.field_id, entity.getId());
                user.put(ConstantRepoGroup.field_member, userName);
                user.put(ConstantRepoGroup.field_group_name, entity.getGroupName());
                entity.getMembers().add(user);
            }
        }
    }

    private boolean existRepoGroupEntity(String groupName) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupEntity::getGroupName, groupName);

        // 检查：该模块是否已经存在
        Long count = this.mongoHelper.findCountByQuery(criteriaAndWrapper, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
        return count > 0;
    }

    public RepoGroupEntity queryRepoGroupEntity(String id) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupEntity::getId, id);

        // 检查：该模块是否已经存在
        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
    }

    public void deleteRepoGroupEntity(String groupId) {
        // 查询原来的关系
        RepoGroupRelation relation = this.queryGroupRelation(ConstantRepoRelation.value_direct_group2user, groupId);

        // 逐个删除相关用户下的分组
        if (relation != null) {
            for (String member : relation.getObjects()) {
                this.deleteRepoGroupRelation(groupId, member);
            }
        }

        // 删除关系表的分组
        this.delRelation(ConstantRepoRelation.value_direct_group2user, groupId);

        // 删除主表的该分组
        this.mongoHelper.deleteById(groupId, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
    }

    public void insertRepoGroupEntity(RepoGroupEntity entity) {
        if (MethodUtils.hasEmpty(entity.getGroupName())) {
            throw new RuntimeException("groupName不能为空！");
        }

        // 检查：该模块是否已经存在
        if (this.existRepoGroupEntity(entity.getGroupName())) {
            throw new RuntimeException("已经存在该名称的分组！");
        }


        this.mongoHelper.insert(ConstantRepoGroup.field_collection_name, entity);
    }

    public void insertRepoGroupRelation(String groupKey, String member) {
        // 检查：该成员是否已经存在
        RepoGroupRelation relation = this.queryGroupRelation(ConstantRepoRelation.value_direct_group2user, groupKey);
        if (relation != null && relation.getObjects().contains(member)) {
            throw new RuntimeException("该成员已经存在！");
        }

        // 加入双向关系
        this.addRelation(ConstantRepoRelation.value_direct_group2user, groupKey, member);
        this.addRelation(ConstantRepoRelation.value_direct_user2group, member, groupKey);
    }

    public void deleteRepoGroupRelation(String groupId, String member) {
        // 删除双向关系
        this.delRelation(ConstantRepoRelation.value_direct_group2user, groupId, member);
        this.delRelation(ConstantRepoRelation.value_direct_user2group, member, groupId);
    }

    public List<RepoGroupEntity> queryGroupEntityList(String userName) {
        if (userName.equals("admin")) {
            CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
            List<RepoGroupEntity> entityList = this.mongoHelper.findListByQuery(andWrapper, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
            return entityList;
        } else {
            RepoGroupRelation repoGroupRelation = this.queryGroupRelation(ConstantRepoRelation.value_direct_user2group, userName);
            if (repoGroupRelation == null) {
                return new ArrayList<>();
            }

            CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
            andWrapper.in(ConstantRepoGroup.field_id, repoGroupRelation.getObjects());
            List<RepoGroupEntity> entityList = this.mongoHelper.findListByIds(repoGroupRelation.getObjects(), ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
            return entityList;
        }

    }

    /**
     * 查询某个双向关系
     */
    private RepoGroupRelation queryGroupRelation(String direct, String name) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupRelation::getDirect, direct);
        criteriaAndWrapper.eq(RepoGroupRelation::getKey, name);

        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoRelation.field_collection_name, RepoGroupRelation.class);
    }


    public Set<String> queryGroupIds(String userName) {
        Set<String> groupIds = new HashSet<>();
        groupIds.add(ConstantRepoGroup.value_public_group_id);


        RepoGroupRelation relation = this.queryGroupRelation(ConstantRepoRelation.value_direct_user2group, userName);
        if (relation != null) {
            groupIds.addAll(relation.getObjects());
        }


        return groupIds;
    }

    public RepoGroupEntity queryGroupEntityOrDefault(String userName) {
        // 查找当前用户为责任人的GroupEntity
        Map<String, Object> param = new HashMap<>();
        param.put(ConstantRepoGroup.field_owner_id, userName);

        RepoGroupEntity entity = this.queryGroupEntity(param);
        if (entity != null) {
            return entity;
        }

        // 如果不存在，那么创建一个默认的群组，然后返回
        RepoGroupEntity defaultEntity = new RepoGroupEntity();
        defaultEntity.setGroupName(userName);
        defaultEntity.setOwnerId(userName);
        this.insertRepoGroupEntity(defaultEntity);

        return defaultEntity;
    }


    public RepoGroupEntity queryGroupEntity(Map<String, Object> param) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();

        if (param.containsKey(ConstantRepoGroup.field_id)) {
            criteriaAndWrapper.eq(RepoGroupEntity::getId, param.get(ConstantRepoGroup.field_id));
        }
        if (param.containsKey(ConstantRepoGroup.field_group_name)) {
            criteriaAndWrapper.eq(RepoGroupEntity::getGroupName, param.get(ConstantRepoGroup.field_group_name));
        }
        if (param.containsKey(ConstantRepoGroup.field_owner_id)) {
            criteriaAndWrapper.eq(RepoGroupEntity::getOwnerId, param.get(ConstantRepoGroup.field_owner_id));
        }


        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
    }

    public List<RepoGroupEntity> queryGroupEntityList(CriteriaAndWrapper criteriaAndWrapper) {
        return this.mongoHelper.findListByQuery(criteriaAndWrapper, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
    }


    public Map<String, RepoGroupEntity> queryGroupEntityMap(String userName) {
        Map<String, RepoGroupEntity> result = new HashMap<>();

        // 数据库的配置部分
        List<RepoGroupEntity> groupEntityList = this.queryGroupEntityList(userName);
        for (RepoGroupEntity entity : groupEntityList) {
            result.put(entity.getId(), entity);
        }

        // 默认的公共部分
        RepoGroupEntity publicEntity = new RepoGroupEntity();
        publicEntity.setId(ConstantRepoGroup.value_public_group_id);
        publicEntity.setGroupName(ConstantRepoGroup.value_public_group_name);
        result.put(publicEntity.getId(), publicEntity);

        return result;
    }


    /**
     * 查询一批双向关系
     *
     * @param direct 方向
     * @param keys   一批业务特征
     * @return 关系列表
     */
    public List<RepoGroupRelation> queryGroupRelations(String direct, Set<String> keys) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupRelation::getDirect, direct);
        criteriaAndWrapper.in(RepoGroupRelation::getKey, keys);

        return this.mongoHelper.findListByQuery(criteriaAndWrapper, ConstantRepoRelation.field_collection_name, RepoGroupRelation.class);
    }

    /**
     * 插入单向
     */
    private void addRelation(String direct, String name, String object) {
        RepoGroupRelation exist = this.queryGroupRelation(direct, name);
        if (exist == null) {
            RepoGroupRelation entity = new RepoGroupRelation();
            entity.setDirect(direct);
            entity.setKey(name);
            entity.getObjects().add(object);

            this.mongoHelper.insert(ConstantRepoRelation.field_collection_name, entity);
        } else {
            if (!exist.getObjects().contains(object)) {
                exist.getObjects().add(object);

                CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
                criteriaAndWrapper.eq(RepoGroupRelation::getDirect, direct);
                criteriaAndWrapper.eq(RepoGroupRelation::getKey, name);

                UpdateBuilder updateBuilder = new UpdateBuilder();
                updateBuilder.set(RepoGroupRelation::getObjects, exist.getObjects());

                this.mongoHelper.updateFirst(criteriaAndWrapper, updateBuilder, ConstantRepoRelation.field_collection_name, RepoGroupRelation.class);
            }
        }
    }

    private void delRelation(String direct, String name, String object) {
        // 检查：关系对象是否存在
        RepoGroupRelation exist = this.queryGroupRelation(direct, name);
        if (exist == null) {
            return;
        }

        // 检查：成员是否存在
        if (!exist.getObjects().contains(object)) {
            return;
        }

        exist.getObjects().remove(object);

        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupRelation::getDirect, direct);
        criteriaAndWrapper.eq(RepoGroupRelation::getKey, name);

        if (exist.getObjects().isEmpty()) {
            this.mongoHelper.deleteByQuery(criteriaAndWrapper, ConstantRepoRelation.field_collection_name, RepoGroupRelation.class);
        } else {
            UpdateBuilder updateBuilder = new UpdateBuilder();
            updateBuilder.set(RepoGroupRelation::getObjects, exist.getObjects());


            this.mongoHelper.updateFirst(criteriaAndWrapper, updateBuilder, ConstantRepoRelation.field_collection_name, RepoGroupRelation.class);

        }
    }

    public void delRelation(String direct, String name) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupRelation::getDirect, direct);
        criteriaAndWrapper.eq(RepoGroupRelation::getKey, name);

        this.mongoHelper.deleteByQuery(criteriaAndWrapper, ConstantRepoRelation.field_collection_name, RepoGroupRelation.class);
    }
}
