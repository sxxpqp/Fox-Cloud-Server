package cn.foxtech.cloud.repo.comp.script.service;


import cn.craccd.mongoHelper.bean.Page;
import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.craccd.mongoHelper.utils.CriteriaOrWrapper;
import cn.craccd.mongoHelper.utils.CriteriaWrapper;
import cn.foxtech.cloud.common.mongo.entity.EdgeEntity;
import cn.foxtech.cloud.common.utils.mongo.MongoExHelper;
import cn.foxtech.cloud.core.constant.Constant;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScript;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScriptOperate;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScriptVersion;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptEntity;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptOperateEntity;
import cn.foxtech.cloud.repo.comp.script.entity.RepoCompScriptVersionEntity;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.security.SecurityUtils;
import cn.foxtech.common.utils.uuid.UuidUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RepoCompScriptVersionService {
    @Autowired
    private MongoExHelper mongoHelper;

    @Autowired
    private RepoCompScriptOperateService operateService;

    @Autowired
    private RepoCompScriptService scriptService;


    public void initialize() {
        List<String> indexFields = new ArrayList<>();

        // 创建数据库表：如果不存在则创建，存在则跳过
        this.mongoHelper.createCollection(ConstantRepoCompScriptVersion.field_collection_name, indexFields);
    }

    public void insertVersionEntity(RepoCompScriptVersionEntity entity) {
        if (MethodUtils.hasEmpty(entity.getAuthor(), entity.getScriptId(), entity.getOperates())) {
            throw new ServiceException("参数不能为空: scriptId, author, operates");
        }

        // 验证：操作内容是否合法
        this.operateService.verifyEntityList(entity.getOperates());

        // 写入数据库
        this.mongoHelper.insert(ConstantRepoCompScriptVersion.field_collection_name, entity);
    }

    /**
     * 上传版本
     *
     * @param userName
     * @param body
     */
    public void uploadVersionEntity(String userName, Map<String, Object> body) {
        String compId = (String) body.get(ConstantRepoCompScriptVersion.field_comp_id);
        String description = (String) body.get(ConstantRepoCompScriptVersion.field_description);
        String commitKey = (String) body.get(ConstantRepoCompScript.field_commit_key);
        List<Map<String, Object>> operates = (List<Map<String, Object>>) body.get(ConstantRepoCompScriptVersion.field_operates);
        if (MethodUtils.hasEmpty(compId, operates)) {
            throw new ServiceException("body参数缺失: compId, operates");
        }

        // 独立提示
        if (MethodUtils.hasEmpty(commitKey)) {
            throw new ServiceException("请输入commitKey");
        }


        Map<String, Object> param = new HashMap<>();
        param.put(ConstantRepoCompScriptVersion.field_id, compId);
        CriteriaWrapper criteriaWrapper = this.scriptService.buildWrapper(param);
        List<RepoCompScriptEntity> entityList = this.scriptService.queryEntityList(criteriaWrapper);
        if (entityList.isEmpty()) {
            throw new ServiceException("找不到对应的组件!");
        }


        RepoCompScriptEntity scriptEntity = entityList.get(0);
        if (!userName.equals("admin")) {
            if (commitKey.isEmpty()) {
                throw new ServiceException("只有下列人员才可以提交：admin和持有commitKey的人员：" + scriptEntity.getGroupName());
            }

            // 验证提交密码
            if (!SecurityUtils.matchesPassword(commitKey, scriptEntity.getCommitKey())) {
                throw new ServiceException("commitKey验证不通过！");
            }
        }


        // 构造一个新版本对象
        RepoCompScriptVersionEntity newEntity = this.buildVersionEntity(userName, scriptEntity.getId(), operates, description);

        // 检查：是否缺少最小参数
        if (MethodUtils.hasEmpty(newEntity.getAuthor(), newEntity.getScriptId(), newEntity.getOperates())) {
            throw new ServiceException("参数不能为空: scriptId, author, operates");
        }

        // 查找最近的提交版本
        param = new HashMap<>();
        param.put(ConstantRepoCompScriptVersion.field_script_id, scriptEntity.getId());
        criteriaWrapper = this.buildWrapper(param);
        RepoCompScriptVersionEntity lastEntity = this.queryEntity(criteriaWrapper, new SortBuilder(RepoCompScriptEntity::getCreateTime, Sort.Direction.DESC));
        if (lastEntity == null) {
            // 插入脚本代码
            this.insertVersionEntity(newEntity);
            return;
        }

        // 比较一下，内容是否发生了变化
        String lastValue = this.getServiceValue(lastEntity.getOperates());
        String newValue = this.getServiceValue(newEntity.getOperates());
        if (!lastValue.equals(newValue)) {
            this.insertVersionEntity(newEntity);
            return;
        }


        throw new ServiceException("没有发生修改，请勿重复提交!");
    }

    public RepoCompScriptVersionEntity buildVersionEntity(String userName, String scriptId, List<Map<String, Object>> operates, String description) {
        RepoCompScriptVersionEntity entity = new RepoCompScriptVersionEntity();
        entity.setScriptId(scriptId);
        entity.setAuthor(userName);
        entity.setDescription(description);

        for (Map<String, Object> operate : operates) {
            String operateName = (String) operate.get(ConstantRepoCompScriptOperate.field_operate_name);
            String operateMode = (String) operate.get(ConstantRepoCompScriptOperate.field_operate_mode);
            String dataType = (String) operate.get(ConstantRepoCompScriptOperate.field_data_type);
            String serviceType = (String) operate.get(ConstantRepoCompScriptOperate.field_service_type);
            String engineType = (String) operate.get(ConstantRepoCompScriptOperate.field_engine_type);
            Boolean polling = (Boolean) operate.get(ConstantRepoCompScriptOperate.field_polling);
            Integer timeout = (Integer) operate.get(ConstantRepoCompScriptOperate.field_timeout);
            Map<String, Object> engineParam = (Map<String, Object>) operate.get(ConstantRepoCompScriptOperate.field_engine_param);
            Object updateTime = operate.get(ConstantRepoCompScriptOperate.field_update_time);
            Object createTime = operate.get(ConstantRepoCompScriptOperate.field_create_time);

            // 简单校验参数
            if (MethodUtils.hasEmpty(operateName, operateMode, dataType, serviceType, engineType, polling, timeout, engineParam, updateTime, createTime)) {
                throw new cn.foxtech.core.exception.ServiceException("参数不能为空: operateName, operateMode, dataType, serviceType, engineType, polling, timeout, engineParam, updateTime, createTime");
            }

            RepoCompScriptOperateEntity operateEntity = new RepoCompScriptOperateEntity();
            operateEntity.setOperateId(UuidUtils.randomUUID());
            operateEntity.setOperateMode(operateMode);
            operateEntity.setOperateName(operateName);
            operateEntity.setDataType(dataType);
            operateEntity.setServiceType(serviceType);
            operateEntity.setTimeout(timeout);
            operateEntity.setEngineType(engineType);
            operateEntity.setEngineParam(engineParam);
            operateEntity.setPolling(polling);
            operateEntity.setCreateTime(Long.parseLong(createTime.toString()));
            operateEntity.setUpdateTime(Long.parseLong(updateTime.toString()));

            entity.getOperates().add(operateEntity);
        }

        return entity;
    }

    private String getServiceValue(List<RepoCompScriptOperateEntity> list) {
        list.sort(new Comparator<RepoCompScriptOperateEntity>() {
            @Override
            public int compare(RepoCompScriptOperateEntity v1, RepoCompScriptOperateEntity v2) {
                String name1 = v1.getOperateName();
                String name2 = v2.getOperateName();
                return name1.compareTo(name2);
            }
        });

        StringBuilder sb = new StringBuilder();
        for (RepoCompScriptOperateEntity entity : list) {
            sb.append(entity.makeServiceValue());
            sb.append(";");
        }

        return sb.toString();
    }

    public RepoCompScriptVersionEntity queryVersionEntity(String id) {
        return this.mongoHelper.findById(id, ConstantRepoCompScriptVersion.field_collection_name, RepoCompScriptVersionEntity.class);
    }

    public void deleteVersionEntity(List<String> ids) {
        this.mongoHelper.deleteByIds(ids, ConstantRepoCompScriptVersion.field_collection_name, RepoCompScriptVersionEntity.class);
    }

    public List<RepoCompScriptVersionEntity> queryEntityList(CriteriaWrapper criteriaWrapper) {
        SortBuilder sortBuilder = new SortBuilder(RepoCompScriptEntity::getCreateTime, Sort.Direction.DESC);
        List<RepoCompScriptVersionEntity> result = this.mongoHelper.findListByQuery(criteriaWrapper, sortBuilder, ConstantRepoCompScriptVersion.field_collection_name, RepoCompScriptVersionEntity.class);
        return result;
    }

    public RepoCompScriptVersionEntity queryEntity(CriteriaWrapper criteriaWrapper, SortBuilder sortBuilder) {
        RepoCompScriptVersionEntity result = this.mongoHelper.findOneByQuery(criteriaWrapper, sortBuilder, ConstantRepoCompScriptVersion.field_collection_name, RepoCompScriptVersionEntity.class);
        return result;
    }


    public CriteriaAndWrapper buildWrapper(Map<String, Object> param) {
        CriteriaAndWrapper andWrapper = new CriteriaAndWrapper();
        if (param.containsKey(ConstantRepoCompScriptVersion.field_id)) {
            andWrapper.eq(ConstantRepoCompScriptVersion.field_id, param.get(ConstantRepoCompScriptVersion.field_id));
        }
        if (param.containsKey(ConstantRepoCompScriptVersion.field_author)) {
            andWrapper.eq(ConstantRepoCompScriptVersion.field_author, param.get(ConstantRepoCompScriptVersion.field_author));
        }
        if (param.containsKey(ConstantRepoCompScriptVersion.field_script_id)) {
            andWrapper.eq(ConstantRepoCompScriptVersion.field_script_id, param.get(ConstantRepoCompScriptVersion.field_script_id));
        }
        if (param.containsKey(ConstantRepoCompScriptVersion.field_script_ids)) {
            andWrapper.in(ConstantRepoCompScriptVersion.field_script_id, (Collection)param.get(ConstantRepoCompScriptVersion.field_script_ids));
        }
        if (param.containsKey(ConstantRepoCompScriptVersion.field_description)) {
            andWrapper.like(ConstantRepoCompScriptVersion.field_description, (String) param.get(ConstantRepoCompScriptVersion.field_description));
        }

        // 关键词查询：从这些文本字段中查询
        if (param.containsKey(ConstantRepoCompScriptVersion.field_keyword)) {
            CriteriaOrWrapper orWrapper = new CriteriaOrWrapper();
            orWrapper.like(ConstantRepoCompScriptVersion.field_author, (String) param.get(ConstantRepoCompScriptVersion.field_keyword));
            orWrapper.like(ConstantRepoCompScriptVersion.field_description, (String) param.get(ConstantRepoCompScriptVersion.field_keyword));

            andWrapper.and(orWrapper);
        }

        return andWrapper;
    }

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
        SortBuilder sortBuilder = new SortBuilder(RepoCompScriptVersionEntity::getCreateTime, Sort.Direction.DESC);
        Page<RepoCompScriptVersionEntity> result = this.mongoHelper.findPage(criteriaWrapper, sortBuilder, page, ConstantRepoCompScriptVersion.field_collection_name, RepoCompScriptVersionEntity.class);

        // 将结果返回
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getCount());
        data.put("list", result.getList());


        return data;
    }
}
