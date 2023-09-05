package cn.foxtech.cloud.manager.repository.service;

import cn.craccd.mongoHelper.bean.Page;
import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.bean.UpdateBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.craccd.mongoHelper.utils.CriteriaOrWrapper;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.repository.constants.Constant;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoGroup;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoRelation;
import cn.foxtech.cloud.manager.repository.entity.RepoGroupEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoGroupRelation;
import cn.foxtech.common.utils.ContainerUtils;
import cn.foxtech.common.utils.method.MethodUtils;
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

    private void extentMembers(List<RepoGroupEntity> entityList) {
        Set<String> groupNames = new HashSet<>();
        for (RepoGroupEntity entity : entityList) {
            groupNames.add(entity.getGroupName());
        }

        List<RepoGroupRelation> relations = this.queryGroupRelations(ConstantRepoRelation.value_direct_group2user, groupNames);
        Map<String, RepoGroupRelation> map = ContainerUtils.buildMapByKey(relations, RepoGroupRelation::getName);

        for (RepoGroupEntity entity : entityList) {
            RepoGroupRelation relation = map.get(entity.getGroupName());
            if (relation == null) {
                continue;
            }

            for (String userName : relation.getObjects()) {
                Map<String, Object> user = new HashMap<>();
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

    public RepoGroupEntity queryRepoGroupEntity(String groupName) {
        // 构造查询过滤器
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupEntity::getGroupName, groupName);

        // 检查：该模块是否已经存在
        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
    }

    public void deleteRepoGroupEntity(String groupName) {
        // 查询原来的关系
        RepoGroupRelation relation = this.queryGroupRelation(ConstantRepoRelation.value_direct_group2user, groupName);

        // 逐个删除相关用户下的分组
        if (relation != null) {
            for (String member : relation.getObjects()) {
                this.deleteRepoGroupRelation(groupName, member);
            }
        }

        // 删除关系表的分组
        this.delRelation(ConstantRepoRelation.value_direct_group2user, groupName);

        // 删除主表的该分组
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupEntity::getGroupName, groupName);
        this.mongoHelper.deleteByQuery(criteriaAndWrapper, ConstantRepoGroup.field_collection_name, RepoGroupEntity.class);
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

    public void insertRepoGroupRelation(String groupName, String member) {
        // 检查：该成员是否已经存在
        RepoGroupRelation relation = this.queryGroupRelation(ConstantRepoRelation.value_direct_group2user, groupName);
        if (relation != null && relation.getObjects().contains(member)) {
            throw new RuntimeException("该成员已经存在！");
        }

        // 加入双向关系
        this.addRelation(ConstantRepoRelation.value_direct_group2user, groupName, member);
        this.addRelation(ConstantRepoRelation.value_direct_user2group, member, groupName);
    }

    public void deleteRepoGroupRelation(String groupName, String member) {
        // 删除双向关系
        this.delRelation(ConstantRepoRelation.value_direct_group2user, groupName, member);
        this.delRelation(ConstantRepoRelation.value_direct_user2group, member, groupName);
    }

    /**
     * 查询某个双向关系
     */
    public RepoGroupRelation queryGroupRelation(String direct, String name) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupRelation::getDirect, direct);
        criteriaAndWrapper.eq(RepoGroupRelation::getName, name);

        return this.mongoHelper.findOneByQuery(criteriaAndWrapper, ConstantRepoRelation.field_collection_name, RepoGroupRelation.class);
    }

    /**
     * 查询一批双向关系
     *
     * @param direct 方向
     * @param names  一批名称
     * @return 关系列表
     */
    public List<RepoGroupRelation> queryGroupRelations(String direct, Set<String> names) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(RepoGroupRelation::getDirect, direct);
        criteriaAndWrapper.in(RepoGroupRelation::getName, names);

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
            entity.setName(name);
            entity.getObjects().add(object);

            this.mongoHelper.insert(ConstantRepoRelation.field_collection_name, entity);
        } else {
            if (!exist.getObjects().contains(object)) {
                exist.getObjects().add(object);

                CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
                criteriaAndWrapper.eq(RepoGroupRelation::getDirect, direct);
                criteriaAndWrapper.eq(RepoGroupRelation::getName, name);

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
        criteriaAndWrapper.eq(RepoGroupRelation::getName, name);

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
        criteriaAndWrapper.eq(RepoGroupRelation::getName, name);

        this.mongoHelper.deleteByQuery(criteriaAndWrapper, ConstantRepoRelation.field_collection_name, RepoGroupRelation.class);
    }
}
