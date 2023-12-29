package cn.foxtech.cloud.repo.comp.script.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter(value = AccessLevel.PUBLIC)
@Setter(value = AccessLevel.PUBLIC)
public class RepoCompScriptOperateEntity {
    /**
     * 手动生成的UUID
     */
    private String operateId;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 更新时间
     */
    private Long updateTime;
    /**
     * 操作命令
     */
    private String operateName;
    /**
     * 业务类型：device、channel
     */
    private String serviceType;
    /**
     * 操作模式: exchange/publish/report
     */
    private String operateMode;
    /**
     * 返回的数据类型：状态/记录
     */
    private String dataType;
    /**
     * 引擎类型：两种引擎，一种是Java的Jar，一种是JavaScript的jsp，默认是JAVA
     */
    private String engineType;
    /**
     * 通信超时
     */
    private Integer timeout = 2000;
    /**
     * 该操作是否需要被轮询调度
     */
    private Boolean polling = false;
    /**
     * 引擎参数：真正的操作内容
     */
    private Map<String, Object> engineParam;

    /**
     * 除了operateId/createTime/updateTime之外的其他字段
     *
     * @return
     */
    public List<Object> makeServiceValue() {
        List<Object> values = new ArrayList<>();

        values.add(this.operateName);
        values.add(this.serviceType);
        values.add(this.operateMode);
        values.add(this.engineType);
        values.add(this.timeout);
        values.add(this.polling);
        values.add(this.engineParam);

        return values;
    }
}
