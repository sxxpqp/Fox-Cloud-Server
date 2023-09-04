package cn.foxtech.cloud.common.utils.mongo;

import cn.craccd.mongoHelper.bean.*;
import cn.craccd.mongoHelper.config.Constant;
import cn.craccd.mongoHelper.reflection.ReflectionUtil;
import cn.craccd.mongoHelper.reflection.SerializableFunction;
import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.craccd.mongoHelper.utils.CriteriaWrapper;
import cn.craccd.mongoHelper.utils.FormatUtils;
import cn.craccd.mongoHelper.utils.SystemTool;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.convert.UpdateMapper;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

/**
 * mongodb操作器
 */
@Component
public class MongoExHelper {
    @Autowired
    protected MongoConverter mongoConverter;
    protected QueryMapper queryMapper;
    protected UpdateMapper updateMapper;
    @Autowired
    protected MongoTemplate mongoTemplate;
    @Value("${spring.data.mongodb.print:false}")
    protected Boolean print;
    @Value("${spring.data.mongodb.slowQuery:false}")
    protected Boolean slowQuery;
    @Value("${spring.data.mongodb.slowTime:1000}")
    protected Long slowTime;
    @Autowired
    MongoMappingContext mongoMappingContext;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    @Bean("FoxMongoHelper.Init")
    public void init() {
        queryMapper = new QueryMapper(mongoConverter);
        updateMapper = new UpdateMapper(mongoConverter);
    }

    public void createCollection(String collectionName, String indexField) {
        List<String> indexFields = new ArrayList<>();
        indexFields.add(indexField);

        this.createCollection(collectionName, indexFields);
    }

    /**
     * 创建mongodb的表和索引
     *
     * @param collectionName 数据库表名称
     * @param indexFields    需要索引的字段列表
     */
    public void createCollection(String collectionName, Collection<String> indexFields) {
        // 创建表
        if (!this.mongoTemplate.collectionExists(collectionName)) {
            this.mongoTemplate.createCollection(collectionName);
            System.out.println("创建了" + collectionName + "表");
        }

        // 创建索引：注意，mongo的index的name是随机取的，可能不一定跟field一样，所以mongo提供了ensureIndex
        IndexOperations indexOps = this.mongoTemplate.indexOps(collectionName);
        for (String indexField : indexFields) {
            indexOps.ensureIndex(new Index(indexField, Sort.Direction.ASC));
        }
    }

    private void insertSlowQuery(String log, Long queryTime) {
        if (slowQuery) {
            SlowQuery slowQuery = new SlowQuery();
            slowQuery.setQuery(log);
            slowQuery.setTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            slowQuery.setQueryTime(queryTime);
            slowQuery.setSystem(SystemTool.getSystem());
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();

            // 保存堆栈
            String stackStr = "";
            for (int i = 0; i < stack.length; i++) {
                stackStr += stack[i].getClassName() + "." + stack[i].getMethodName() + ":" + stack[i].getLineNumber() + "\n";
            }
            slowQuery.setStack(stackStr);

            mongoTemplate.insert(slowQuery);
        }
    }

    /**
     * 打印查询语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    private void logQuery(String collectionName, Class<?> clazz, Query query, Long startTime) {

        MongoPersistentEntity<?> entity = mongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);
        Document mappedField = queryMapper.getMappedObject(query.getFieldsObject(), entity);
        Document mappedSort = queryMapper.getMappedObject(query.getSortObject(), entity);

        String log = "\ndb." + collectionName + ".find(";

        log += FormatUtils.bson(mappedQuery.toJson()) + ")";

        if (!query.getFieldsObject().isEmpty()) {
            log += ".projection(";
            log += FormatUtils.bson(mappedField.toJson()) + ")";
        }

        if (query.isSorted()) {
            log += ".sort(";
            log += FormatUtils.bson(mappedSort.toJson()) + ")";
        }

        if (query.getLimit() != 0L) {
            log += ".limit(" + query.getLimit() + ")";
        }

        if (query.getSkip() != 0L) {
            log += ".skip(" + query.getSkip() + ")";
        }
        log += ";";

        // 记录慢查询
        Long queryTime = System.currentTimeMillis() - startTime;
        if (queryTime > slowTime) {
            insertSlowQuery(log, queryTime);
        }
        if (print) {
            // 打印语句
            logger.info(log + "\n执行时间:" + queryTime + "ms");
        }

    }

    /**
     * 打印查询语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    private void logCount(String collectionName, Class<?> clazz, Query query, Long startTime) {

        MongoPersistentEntity<?> entity = mongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);

        String log = "\ndb." + collectionName + ".find(";
        log += FormatUtils.bson(mappedQuery.toJson()) + ")";
        log += ".count();";

        // 记录慢查询
        Long queryTime = System.currentTimeMillis() - startTime;
        if (queryTime > slowTime) {
            insertSlowQuery(log, queryTime);
        }
        if (print) {
            // 打印语句
            logger.info(log + "\n执行时间:" + queryTime + "ms");
        }

    }

    /**
     * 打印查询语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    private void logDelete(String collectionName, Class<?> clazz, Query query, Long startTime) {

        MongoPersistentEntity<?> entity = mongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);

        String log = "\ndb." + collectionName + ".remove(";
        log += FormatUtils.bson(mappedQuery.toJson()) + ")";
        log += ";";

        // 记录慢查询
        Long queryTime = System.currentTimeMillis() - startTime;
        if (queryTime > slowTime) {
            insertSlowQuery(log, queryTime);
        }
        if (print) {
            // 打印语句
            logger.info(log + "\n执行时间:" + queryTime + "ms");
        }

    }

    /**
     * 打印查询语句
     *
     * @param clazz         类
     * @param query         查询对象
     * @param updateBuilder 更新对象
     * @param multi         是否为批量更新
     * @param startTime     查询开始时间
     */
    private void logUpdate(String collectionName, Class<?> clazz, Query query, UpdateBuilder updateBuilder, boolean multi, Long startTime) {

        MongoPersistentEntity<?> entity = mongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);
        Document mappedUpdate = updateMapper.getMappedObject(updateBuilder.toUpdate().getUpdateObject(), entity);

        String log = "\ndb." + collectionName + ".update(";
        log += FormatUtils.bson(mappedQuery.toJson()) + ",";
        log += FormatUtils.bson(mappedUpdate.toJson()) + ",";
        log += FormatUtils.bson("{multi:" + multi + "})");
        log += ";";

        // 记录慢查询
        Long queryTime = System.currentTimeMillis() - startTime;
        if (queryTime > slowTime) {
            insertSlowQuery(log, queryTime);
        }
        if (print) {
            // 打印语句
            logger.info(log + "\n执行时间:" + queryTime + "ms");
        }

    }

    /**
     * 打印查询语句
     *
     * @param object    保存对象
     * @param startTime 查询开始时间
     * @param isInsert  是否为插入
     */
    private void logSave(String collectionName, Object object, Long startTime, Boolean isInsert) {
        JSONObject jsonObject = JSONUtil.parseObj(object);

        if (isInsert) {
            jsonObject.remove(Constant.ID);
        }

        String log = "\ndb." + collectionName + ".save(";
        log += JSONUtil.toJsonPrettyStr(jsonObject);
        log += ");";

        // 记录慢查询
        Long queryTime = System.currentTimeMillis() - startTime;
        if (queryTime > slowTime) {
            insertSlowQuery(log, queryTime);
        }
        if (print) {
            // 打印语句
            logger.info(log + "\n执行时间:" + queryTime + "ms");
        }
    }

    /**
     * 打印查询语句
     *
     * @param list      保存集合
     * @param startTime 查询开始时间
     */
    private void logSave(String collectionName, List<?> list, Long startTime) {
        List<JSONObject> cloneList = new ArrayList<>();
        for (Object item : list) {
            JSONObject jsonObject = JSONUtil.parseObj(item);

            jsonObject.remove(Constant.ID);
            cloneList.add(jsonObject);
        }

        Object object = list.get(0);
        String log = "\ndb." + collectionName + ".save(";
        log += JSONUtil.toJsonPrettyStr(cloneList);
        log += ");";

        // 记录慢查询
        Long queryTime = System.currentTimeMillis() - startTime;
        if (queryTime > slowTime) {
            insertSlowQuery(log, queryTime);
        }
        if (print) {
            // 打印语句
            logger.info(log + "\n执行时间:" + queryTime + "ms");
        }
    }

    /**
     * 插入或更新
     *
     * @param object 保存对象
     * @return String 对象id
     */
    public String insertOrUpdate(String collectionName, Object object) {

        Long time = System.currentTimeMillis();
        String id = (String) ReflectUtil.getFieldValue(object, Constant.ID);
        Object objectOrg = StrUtil.isNotEmpty(id) ? findById(id, collectionName, object.getClass()) : null;

        if (objectOrg == null) {
            // 插入
            // 设置插入时间
            setCreateTime(object, time);
            // 设置更新时间
            setUpdateTime(object, time);

            // 设置默认值
            setDefaultVaule(object);
            // 去除id值
            ReflectUtil.setFieldValue(object, Constant.ID, null);

            // 克隆一个@IgnoreColumn的字段设为null的对象;
            Object objectClone = BeanUtil.copyProperties(object, object.getClass());
            ignoreColumn(objectClone);

            mongoTemplate.save(objectClone, collectionName);
            id = (String) ReflectUtil.getFieldValue(objectClone, Constant.ID);

            // 设置id值
            ReflectUtil.setFieldValue(object, Constant.ID, id);

            logSave(collectionName, objectClone, time, true);

        } else {
            // 更新
            Field[] fields = ReflectUtil.getFields(object.getClass());
            // 拷贝属性
            for (Field field : fields) {
                if (!field.getName().equals(Constant.ID) && ReflectUtil.getFieldValue(object, field) != null) {
                    ReflectUtil.setFieldValue(objectOrg, field, ReflectUtil.getFieldValue(object, field));
                }
            }

            // 设置更新时间
            setUpdateTime(objectOrg, time);
            // 克隆一个@IgnoreColumn的字段设为null的对象;
            Object objectClone = BeanUtil.copyProperties(objectOrg, object.getClass());
            ignoreColumn(objectClone);

            mongoTemplate.save(objectClone, collectionName);
            logSave(collectionName, objectClone, time, false);
        }

        return id;
    }

    /**
     * 插入
     *
     * @param object 对象
     * @return String 对象id
     */
    public String insert(String collectionName, Object object) {
        ReflectUtil.setFieldValue(object, Constant.ID, null);
        insertOrUpdate(collectionName, object);
        return (String) ReflectUtil.getFieldValue(object, Constant.ID);
    }

    /**
     * 批量插入
     *
     * @param list 批量插入对象
     * @return Collection<T> 批量对象id集合
     */
    public <T> List<String> insertAll(String collectionName, List<T> list) {
        Long time = System.currentTimeMillis();

        List listClone = new ArrayList<>();
        for (Object object : list) {

            // 去除id以便插入
            ReflectUtil.setFieldValue(object, Constant.ID, null);
            // 设置插入时间
            setCreateTime(object, time);
            // 设置更新时间
            setUpdateTime(object, time);
            // 设置默认值
            setDefaultVaule(object);
            // 克隆一个@IgnoreColumn的字段设为null的对象;
            Object objectClone = BeanUtil.copyProperties(object, object.getClass());
            ignoreColumn(objectClone);
            listClone.add(objectClone);
        }

        mongoTemplate.insertAll(listClone);
        logSave(collectionName, listClone, time);

        List<String> ids = new ArrayList<>();
        for (Object object : listClone) {
            String id = (String) ReflectUtil.getFieldValue(object, Constant.ID);
            ids.add(id);
        }

        return ids;
    }

    /**
     * 设置更新时间
     *
     * @param object 对象
     * @param time   更新时间
     */
    private void setUpdateTime(Object object, Long time) {
        Field[] fields = ReflectUtil.getFields(object.getClass());
        for (Field field : fields) {
            // 获取注解
            if (field.isAnnotationPresent(UpdateTime.class) && field.getType().equals(Long.class)) {
                ReflectUtil.setFieldValue(object, field, time);
            }
        }
    }

    /**
     * 设置创建时间
     *
     * @param object 对象
     * @param time   创建时间
     */
    private void setCreateTime(Object object, Long time) {
        Field[] fields = ReflectUtil.getFields(object.getClass());
        for (Field field : fields) {
            // 获取注解
            if (field.isAnnotationPresent(CreateTime.class) && field.getType().equals(Long.class)) {
                ReflectUtil.setFieldValue(object, field, time);
            }
        }
    }

    /**
     * 将带有@IgnoreColumn的字段设为null;
     *
     * @param object 对象
     */
    private void ignoreColumn(Object object) {
        Field[] fields = ReflectUtil.getFields(object.getClass());
        for (Field field : fields) {
            // 获取注解
            if (field.isAnnotationPresent(IgnoreColumn.class)) {
                ReflectUtil.setFieldValue(object, field, null);
            }
        }
    }

    /**
     * 根据id更新
     *
     * @param object 对象
     * @return String 对象id
     */
    public String updateById(String collectionName, Object object) {
        if (StrUtil.isEmpty((String) ReflectUtil.getFieldValue(object, Constant.ID))) {
            return null;
        }
        if (findById((String) ReflectUtil.getFieldValue(object, Constant.ID), collectionName, object.getClass()) == null) {
            return null;
        }
        return insertOrUpdate(collectionName, object);
    }

    /**
     * 根据id更新全部字段
     *
     * @param object 对象
     * @return String 对象id
     */
    public String updateAllColumnById(String collectionName, Object object) {

        if (StrUtil.isEmpty((String) ReflectUtil.getFieldValue(object, Constant.ID))) {
            return null;
        }
        if (findById((String) ReflectUtil.getFieldValue(object, Constant.ID), collectionName, object.getClass()) == null) {
            return null;
        }
        Long time = System.currentTimeMillis();
        setUpdateTime(object, time);
        mongoTemplate.save(object,collectionName);
        logSave(collectionName, object, time, false);

        return (String) ReflectUtil.getFieldValue(object, Constant.ID);
    }

    /**
     * 更新查到的第一项
     *
     * @param criteriaWrapper 查询
     * @param updateBuilder   更新
     * @param clazz           类
     * @return UpdateResult 更新结果
     */
    public UpdateResult updateFirst(CriteriaWrapper criteriaWrapper, UpdateBuilder updateBuilder, String collectionName, Class<?> clazz) {
        Long time = System.currentTimeMillis();
        Query query = new Query(criteriaWrapper.build());

        UpdateResult updateResult = mongoTemplate.updateFirst(query, updateBuilder.toUpdate(), collectionName);
        logUpdate(collectionName, clazz, query, updateBuilder, false, time);

        return updateResult;
    }

    /**
     * 更新查到的全部项
     *
     * @param criteriaWrapper 查询
     * @param updateBuilder   更新
     * @param clazz           类
     * @return UpdateResult 更新结果
     */
    public UpdateResult updateMulti(CriteriaWrapper criteriaWrapper, UpdateBuilder updateBuilder, String collectionName, Class<?> clazz) {

        Long time = System.currentTimeMillis();
        Query query = new Query(criteriaWrapper.build());
        UpdateResult updateResult = mongoTemplate.updateMulti(new Query(criteriaWrapper.build()), updateBuilder.toUpdate(), collectionName);
        logUpdate(collectionName, clazz, query, updateBuilder, true, time);

        return updateResult;
    }

    /**
     * 根据id删除
     *
     * @param id    对象
     * @param clazz 类
     * @return DeleteResult 删除结果
     */
    public DeleteResult deleteById(String id, String collectionName, Class<?> clazz) {

        if (StrUtil.isEmpty(id)) {
            return null;
        }
        return deleteByQuery(new CriteriaAndWrapper().eq(Constant::getId, id), collectionName, clazz);
    }

    /**
     * 根据id删除
     *
     * @param ids   对象
     * @param clazz 类
     * @return DeleteResult 删除结果
     */
    public DeleteResult deleteByIds(List<String> ids, String collectionName, Class<?> clazz) {

        if (ids == null || ids.size() == 0) {
            return null;
        }

        return deleteByQuery(new CriteriaAndWrapper().in(Constant::getId, ids), collectionName, clazz);
    }

    /**
     * 根据条件删除
     *
     * @param criteriaWrapper 查询
     * @param clazz           类
     * @return DeleteResult 删除结果
     */
    public DeleteResult deleteByQuery(CriteriaWrapper criteriaWrapper, String collectionName, Class<?> clazz) {
        Long time = System.currentTimeMillis();
        Query query = new Query(criteriaWrapper.build());
        DeleteResult deleteResult = mongoTemplate.remove(query, collectionName);
        logDelete(collectionName, clazz, query, time);

        return deleteResult;
    }

    /**
     * 设置默认值
     *
     * @param object 对象
     */
    private void setDefaultVaule(Object object) {
        Field[] fields = ReflectUtil.getFields(object.getClass());
        for (Field field : fields) {
            // 获取注解
            if (field.isAnnotationPresent(InitValue.class)) {
                InitValue defaultValue = field.getAnnotation(InitValue.class);

                String value = defaultValue.value();

                if (ReflectUtil.getFieldValue(object, field) == null) {
                    // 获取字段类型
                    Class<?> type = field.getType();
                    if (type.equals(String.class)) {
                        ReflectUtil.setFieldValue(object, field, value);
                    }
                    if (type.equals(Short.class)) {
                        ReflectUtil.setFieldValue(object, field, Short.parseShort(value));
                    }
                    if (type.equals(Integer.class)) {
                        ReflectUtil.setFieldValue(object, field, Integer.parseInt(value));
                    }
                    if (type.equals(Long.class)) {
                        ReflectUtil.setFieldValue(object, field, Long.parseLong(value));
                    }
                    if (type.equals(Float.class)) {
                        ReflectUtil.setFieldValue(object, field, Float.parseFloat(value));
                    }
                    if (type.equals(Double.class)) {
                        ReflectUtil.setFieldValue(object, field, Double.parseDouble(value));
                    }
                    if (type.equals(Boolean.class)) {
                        ReflectUtil.setFieldValue(object, field, Boolean.parseBoolean(value));
                    }
                }
            }
        }
    }

    /**
     * 累加某一个字段的数量,原子操作
     *
     * @param id
     * @return UpdateResult 更新结果
     */
    public <R, E> UpdateResult addCountById(String id, SerializableFunction<E, R> property, Number count, String collectionName, Class<?> clazz) {
        UpdateBuilder updateBuilder = new UpdateBuilder().inc(property, count);

        return updateFirst(new CriteriaAndWrapper().eq(Constant::getId, id), updateBuilder, collectionName, clazz);
    }

    /**
     * 按查询条件获取Page
     *
     * @param criteriaWrapper 查询
     * @param page            分页
     * @param clazz           类
     * @return Page 分页
     */
    public <T> Page<T> findPage(CriteriaWrapper criteriaWrapper, Page<?> page, String collectionName, Class<T> clazz) {
        SortBuilder sortBuilder = new SortBuilder(Constant::getId, Direction.DESC);
        return findPage(criteriaWrapper, sortBuilder, page, collectionName, clazz);
    }

    /**
     * 按查询条件获取Page
     *
     * @param criteriaWrapper 查询
     * @param sortBuilder     排序
     * @param clazz           类
     * @return Page 分页
     */
    public <T> Page<T> findPage(CriteriaWrapper criteriaWrapper, SortBuilder sortBuilder, Page<?> page, String collectionName, Class<T> clazz) {

        Page<T> pageResp = new Page<T>();
        pageResp.setCurr(page.getCurr());
        pageResp.setLimit(page.getLimit());

        // 查询出总条数
        if (page.getQueryCount()) {
            Long count = findCountByQuery(criteriaWrapper, collectionName, clazz);
            pageResp.setCount(count);
        }

        // 查询List
        Query query = new Query(criteriaWrapper.build());
        query.with(sortBuilder.toSort());
        query.skip((long) (page.getCurr() - 1) * page.getLimit());// 从那条记录开始
        query.limit(page.getLimit());// 取多少条记录

        Long systemTime = System.currentTimeMillis();
        List<T> list = mongoTemplate.find(query, clazz, collectionName);
        logQuery(collectionName, clazz, query, systemTime);

        pageResp.setList(list);

        return pageResp;
    }

    /**
     * 按查询条件获取Page
     *
     * @param sortBuilder 排序
     * @param clazz       类
     * @return Page 分页
     */
    public <T> Page<T> findPage(SortBuilder sortBuilder, Page<?> page, String collectionName, Class<T> clazz) {
        return findPage(new CriteriaAndWrapper(), sortBuilder, page, collectionName, clazz);
    }

    /**
     * 获取Page
     *
     * @param page  分页
     * @param clazz 类
     * @return Page 分页
     */
    public <T> Page<T> findPage(Page<?> page, String collectionName, Class<T> clazz) {
        return findPage(new CriteriaAndWrapper(), page, collectionName, clazz);
    }

    /**
     * 根据id查找
     *
     * @param id    id
     * @param clazz 类
     * @return T 对象
     */
    public <T> T findById(String id, String collectionName, Class<T> clazz) {

        if (StrUtil.isEmpty(id)) {
            return null;
        }
        Long systemTime = System.currentTimeMillis();

        T t = mongoTemplate.findById(id, clazz, collectionName);

        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper().eq(Constant::getId, id);
        logQuery(collectionName, clazz, new Query(criteriaAndWrapper.build()), systemTime);
        return t;
    }

    /**
     * 根据条件查找单个
     *
     * @param <T>             类型
     * @param criteriaWrapper
     * @param clazz           类
     * @return T 对象
     */
    public <T> T findOneByQuery(CriteriaWrapper criteriaWrapper, String collectionName, Class<T> clazz) {
        SortBuilder sortBuilder = new SortBuilder(Constant::getId, Direction.DESC);
        return findOneByQuery(criteriaWrapper, sortBuilder, collectionName, clazz);
    }

    /**
     * 根据条件查找单个
     *
     * @param criteriaWrapper 查询
     * @param clazz           类
     * @return T 对象
     */
    public <T> T findOneByQuery(CriteriaWrapper criteriaWrapper, SortBuilder sortBuilder, String collectionName, Class<T> clazz) {

        Query query = new Query(criteriaWrapper.build());
        query.limit(1);
        query.with(sortBuilder.toSort());

        Long systemTime = System.currentTimeMillis();
        T t = mongoTemplate.findOne(query, clazz, collectionName);
        logQuery(collectionName, clazz, query, systemTime);

        return t;

    }

    /**
     * 根据条件查找单个
     *
     * @param sortBuilder 查询
     * @param clazz       类
     * @return T 对象
     */
    public <T> T findOneByQuery(SortBuilder sortBuilder, String collectionName, Class<T> clazz) {
        return findOneByQuery(new CriteriaAndWrapper(), sortBuilder, collectionName, clazz);
    }

    /**
     * 根据条件查找List
     *
     * @param <T>             类型
     * @param criteriaWrapper 查询
     * @param clazz           类
     * @return List 列表
     */
    public <T> List<T> findListByQuery(CriteriaWrapper criteriaWrapper, String collectionName, Class<T> clazz) {
        SortBuilder sortBuilder = new SortBuilder().add(Constant::getId, Direction.DESC);
        return findListByQuery(criteriaWrapper, sortBuilder, collectionName, clazz);

    }

    /**
     * 根据条件查找List
     *
     * @param <T>             类型
     * @param criteriaWrapper 查询
     * @param sortBuilder     排序
     * @param clazz           类
     * @return List 列表
     */
    public <T> List<T> findListByQuery(CriteriaWrapper criteriaWrapper, SortBuilder sortBuilder, String collectionName, Class<T> clazz) {
        Query query = new Query(criteriaWrapper.build());
        query.with(sortBuilder.toSort());

        Long systemTime = System.currentTimeMillis();
        List<T> list = mongoTemplate.find(query, clazz, collectionName);
        logQuery(collectionName, clazz, query, systemTime);
        return list;

    }

    /**
     * 根据条件查找某个属性
     *
     * @param <T>             类型
     * @param criteriaWrapper 查询
     * @param documentClass   类
     * @param property        属性
     * @param propertyClass   属性类
     * @return List 列表
     */
    public <T, R, E> List<T> findPropertiesByQuery(CriteriaWrapper criteriaWrapper, String collectionName, Class<?> documentClass, SerializableFunction<E, R> property, Class<T> propertyClass) {
        Query query = new Query(criteriaWrapper.build());
        query.fields().include(ReflectionUtil.getFieldName(property));

        Long systemTime = System.currentTimeMillis();
        List<?> list = mongoTemplate.find(query, documentClass,collectionName);
        logQuery(collectionName, documentClass, query, systemTime);

        List<T> propertyList = extractProperty(list, ReflectionUtil.getFieldName(property), propertyClass);
        return propertyList;
    }


    /**
     * 根据条件查找某个属性
     *
     * @param <T>             类型
     * @param criteriaWrapper 查询
     * @param documentClass   类
     * @param property        属性
     * @param propertyClass   属性类
     * @return List 列表
     */
    public <T, R, E> List<T> findPropertiesByQuery(CriteriaWrapper criteriaWrapper, String collectionName, Class<?> documentClass, String property, Class<T> propertyClass) {
        Query query = new Query(criteriaWrapper.build());
        query.fields().include(property);

        Long systemTime = System.currentTimeMillis();
        List<?> list = mongoTemplate.find(query, documentClass,collectionName);
        logQuery(collectionName, documentClass, query, systemTime);

        List<T> propertyList = extractProperty(list, property, propertyClass);
        return propertyList;
    }

    /**
     * 根据条件查找某个属性
     *
     * @param criteriaWrapper 查询
     * @param documentClass   类
     * @param property        属性
     * @return List 列表
     */
    public <R, E> List<String> findPropertiesByQuery(CriteriaWrapper criteriaWrapper, String collectionName, Class<?> documentClass, SerializableFunction<E, R> property) {
        return findPropertiesByQuery(criteriaWrapper, collectionName, documentClass, property, String.class);
    }

    /**
     * 根据id查找某个属性
     *
     * @param property 属性
     * @return List 列表
     */
    public <R, E> List<String> findPropertiesByIds(List<String> ids, String collectionName, Class<?> clazz, SerializableFunction<E, R> property) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper().in(Constant::getId, ids);
        return findPropertiesByQuery(criteriaAndWrapper, collectionName, clazz, property);
    }

    /**
     * 根据条件查找id
     *
     * @param criteriaWrapper 查询
     * @param clazz           类
     * @return List 列表
     */
    public List<String> findIdsByQuery(CriteriaWrapper criteriaWrapper, String collectionName, Class<?> clazz) {
        return findPropertiesByQuery(criteriaWrapper, collectionName, clazz, Constant::getId);
    }

    /**
     * 根据id集合查找
     *
     * @param ids   id集合
     * @param clazz 类
     * @return List 列表
     */
    public <T> List<T> findListByIds(Collection<String> ids, String collectionName, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper().in(Constant::getId, ids);
        return findListByQuery(criteriaAndWrapper, collectionName, clazz);
    }

    /**
     * 根据id集合查找
     *
     * @param ids   id集合
     * @param clazz 类
     * @return List 列表
     */
    public <T> List<T> findListByIds(Collection<String> ids, SortBuilder sortBuilder, String collectionName, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper().in(Constant::getId, ids);
        return findListByQuery(criteriaAndWrapper, sortBuilder, collectionName, clazz);
    }

    /**
     * 根据id集合查找
     *
     * @param ids   id集合
     * @param clazz 类
     * @return List 列表
     */
    public <T> List<T> findListByIds(String[] ids, SortBuilder sortBuilder, String collectionName, Class<T> clazz) {
        return findListByIds(Arrays.asList(ids), sortBuilder, collectionName, clazz);
    }

    /**
     * 根据id集合查找
     *
     * @param ids   id集合
     * @param clazz 类
     * @return List 列表
     */
    public <T> List<T> findListByIds(String[] ids, String collectionName, Class<T> clazz) {
        SortBuilder sortBuilder = new SortBuilder(Constant::getId, Direction.DESC);
        return findListByIds(ids, sortBuilder, collectionName, clazz);
    }

    /**
     * 查询全部
     *
     * @param <T>   类型
     * @param clazz 类
     * @return List 列表
     */
    public <T> List<T> findAll(String collectionName, Class<T> clazz) {
        SortBuilder sortBuilder = new SortBuilder(Constant::getId, Direction.DESC);
        return findListByQuery(new CriteriaAndWrapper(), sortBuilder, collectionName, clazz);
    }

    /**
     * 查询全部
     *
     * @param <T>   类型
     * @param clazz 类
     * @return List 列表
     */
    public <T> List<T> findAll(SortBuilder sortBuilder, String collectionName, Class<T> clazz) {
        return findListByQuery(new CriteriaAndWrapper(), sortBuilder, collectionName, clazz);
    }

    /**
     * 查找全部的id
     *
     * @param clazz 类
     * @return List 列表
     */
    public List<String> findAllIds(String collectionName, Class<?> clazz) {
        return findIdsByQuery(new CriteriaAndWrapper(), collectionName, clazz);
    }

    /**
     * 查找数量
     *
     * @param criteriaWrapper 查询
     * @param clazz           类
     * @return Long 数量
     */
    public Long findCountByQuery(CriteriaWrapper criteriaWrapper, String collectionName, Class<?> clazz) {
        Long systemTime = System.currentTimeMillis();
        Long count = null;

        Query query = new Query(criteriaWrapper.build());
        if (query.getQueryObject().isEmpty()) {
            count = mongoTemplate.getCollection(collectionName).estimatedDocumentCount();
        } else {
            count = mongoTemplate.count(query, collectionName);
        }

        logCount(collectionName, clazz, query, systemTime);
        return count;
    }

    /**
     * 查找全部数量
     *
     * @param clazz 类
     * @return Long 数量
     */
    public Long findAllCount(String collectionName, Class<?> clazz) {
        return findCountByQuery(new CriteriaAndWrapper(), collectionName, clazz);
    }

    /**
     * 获取list中对象某个属性,组成新的list
     *
     * @param list     列表
     * @param clazz    类
     * @param property 属性名
     * @return List<T> 属性列表
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> extractProperty(List<?> list, String property, Class<T> clazz) {
        Set<T> rs = new HashSet<T>();
        for (Object object : list) {
            Object value = ReflectUtil.getFieldValue(object, property);
            if (value != null && value.getClass().equals(clazz)) {
                rs.add((T) value);
            }
        }

        return new ArrayList<T>(rs);
    }

    /**
     * 根据指定字段查询数据
     *
     * @param column 字段名称
     * @param params 字段参数
     * @param <E>
     * @param <R>
     * @return
     */
    public <E, R, T> List<T> findListByQuery(SerializableFunction<E, R> column, Object params, String collectionName, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(column, params);

        return this.findListByQuery(criteriaAndWrapper, collectionName, clazz);
    }

    /**
     * In查询操作
     *
     * @param column
     * @param params
     * @param clazz
     * @param <E>
     * @param <R>
     * @param <T>
     * @return
     */
    public <E, R, T> List<T> findListByInQuery(SerializableFunction<E, R> column, Collection params, String collectionName, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.in(column, params);

        return this.findListByQuery(criteriaAndWrapper, collectionName, clazz);
    }

    /**
     * 查找单条数据
     *
     * @param column
     * @param params
     * @param <E>
     * @param <R>
     * @return
     */
    public <E, R, T> T findOneByQuery(SerializableFunction<E, R> column, Object params, String collectionName, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(column, params);

        return this.findOneByQuery(criteriaAndWrapper, collectionName, clazz);
    }

    /**
     * 是否存在某个记录
     *
     * @param column
     * @param params
     * @param clazz
     * @param <E>
     * @param <R>
     * @param <T>
     * @return
     */
    public <E, R, T> boolean has(SerializableFunction<E, R> column, Object params, String collectionName, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(column, params);

        T obj = this.findOneByQuery(criteriaAndWrapper, collectionName, clazz);
        return obj != null;
    }

    /**
     * 更新第一条数据
     *
     * @param updateBuilder
     * @param column
     * @param params
     * @param clazz
     * @param <E>
     * @param <R>
     * @param <T>
     */
    public <E, R, T> void updateFirst(UpdateBuilder updateBuilder, SerializableFunction<E, R> column, Object params, String collectionName, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper();
        criteriaAndWrapper.eq(column, params);

        this.updateFirst(criteriaAndWrapper, updateBuilder, collectionName, clazz);
    }

    /**
     * 创建结构为clazz，但自定义名称的集合
     *
     * @param collectionName 自定义名称
     * @param clazz          结构
     * @param <T>            类型
     */
    public <T> void createCollection(String collectionName, Class<T> clazz) {
        // 创建表
        if (!mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.createCollection(collectionName);
            System.out.println("创建了" + collectionName + "表");
        }

        // 创建索引
        IndexOperations indexOps = mongoTemplate.indexOps(collectionName, clazz);
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
        resolver.resolveIndexFor(clazz).forEach(indexOps::ensureIndex);
    }

    /**
     * 删除指定名称的集合
     *
     * @param collectionName
     * @param <T>
     */
    public <T> void dropCollection(String collectionName) {
        mongoTemplate.dropCollection(collectionName);
    }

    /**
     * 插入记录：对插入来说，id为空，不为空，则主键重复异常
     *
     * @param objectToSave
     * @param collectionName
     * @param <T>
     * @return
     */
    public <T> T insert(T objectToSave, String collectionName) {
        return mongoTemplate.insert(objectToSave, collectionName);
    }

    /**
     * 保存一条记录：对保存来说，id为空则插入，不为空，则保持
     *
     * @param objectToSave
     * @param collectionName
     * @param <T>
     * @return
     */
    public <T> T save(T objectToSave, String collectionName) {
        return mongoTemplate.save(objectToSave, collectionName);
    }

    /**
     * 根据ID，批量更新操作：异步操作，需要过一会数据库才能看到更新的内容
     *
     * @param updateEntities 待更新的集合
     * @param clazz          集合名称
     * @param <TD,TE>
     */
    public <TD, TE> void bulkUpdate(Map<String, TD> updateEntities, String collectionName, Class<TE> clazz) {
        List operateList = new ArrayList<>();
        Long time = System.currentTimeMillis();

        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, collectionName);

        int pageSize = 100;
        for (Map.Entry<String, TD> entry : updateEntities.entrySet()) {
            String id = entry.getKey();
            TD entity = entry.getValue();

            // 过滤条件为ID:mongodb大的id字段名称是带下划线的_id
            Query query = new Query(Criteria.where("_" + Constant.ID).is(id));

            // 填写update内容
            Update update = new Update();
            Field[] fields = ReflectUtil.getFields(entity.getClass());
            for (Field field : fields) {
                // 不允许更新ID
                if (field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                // 不允许更新创建时间
                else if (field.isAnnotationPresent(CreateTime.class)) {
                    continue;
                }
                // 修改时间内部指定
                else if (field.isAnnotationPresent(UpdateTime.class)) {
                    update.set(field.getName(), time);
                    continue;
                } else {
                    Object fieldValue = ReflectUtil.getFieldValue(entity, field);
                    update.set(field.getName(), fieldValue);
                    continue;
                }
            }

            // 将过滤条件和更新操作绑定为一对操作
            Pair updatePair = Pair.of(query, update);

            // 加入批量操作列表当中
            operateList.add(updatePair);

            if (operateList.size() >= pageSize) {
                // 提交批量操作
                bulkOps.upsert(operateList);

                // 执行操作
                BulkWriteResult result = bulkOps.execute();
                operateList.clear();
            }
        }
        if (operateList.size() > 0) {
            // 提交批量操作
            bulkOps.upsert(operateList);

            // 执行操作
            BulkWriteResult result = bulkOps.execute();
        }
    }

    /**
     * 批量插入:由于mongodb是后台线程异步插入，所有最好对数据库表根据业务唯一性设置唯一键，
     * 以防止数据多批数据重复插入，否则你很容易看到重复数据。
     *
     * @param insertList
     * @param clazz
     * @param <TD,TE>
     */
    public <TD, TE> void bulkInsert(List<TD> insertList, String collectionName, Class<TE> clazz) {
        List operateList = new ArrayList<>();
        Long time = System.currentTimeMillis();

        BulkOperations bulkOps = this.mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, collectionName);

        for (TD entity : insertList) {

            // 填写update内容
            Update update = new Update();
            Field[] fields = ReflectUtil.getFields(entity.getClass());
            for (Field field : fields) {
                // 不允许更新ID
                if (field.isAnnotationPresent(Id.class)) {
                    ReflectUtil.setFieldValue(entity, field, null);
                    continue;
                }
                // 不允许更新创建时间
                else if (field.isAnnotationPresent(CreateTime.class)) {
                    ReflectUtil.setFieldValue(entity, field, time);
                    continue;
                }
                // 修改时间内部指定
                else if (field.isAnnotationPresent(UpdateTime.class)) {
                    ReflectUtil.setFieldValue(entity, field, time);
                    continue;
                } else {
                    Object fieldValue = ReflectUtil.getFieldValue(entity, field);
                    update.set(field.getName(), fieldValue);
                }
            }

            // 加入批量操作列表当中
            operateList.add(entity);
        }

        // 提交批量操作
        bulkOps.insert(operateList);

        // 执行操作
        BulkWriteResult result = bulkOps.execute();
    }
}
