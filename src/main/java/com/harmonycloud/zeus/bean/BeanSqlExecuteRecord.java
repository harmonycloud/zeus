package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xutianhong
 * @Date 2022/10/21 9:44 下午
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("sql_execute_record")
public class BeanSqlExecuteRecord implements Serializable {

    private static final long serialVersionUID = -67082778931643L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;

    /**
     * 分区名称
     */
    @TableField("namespace")
    private String namespace;

    /**
     * 中间件名称
     */
    @TableField("middleware_name")
    private String middlewareName;

    /**
     * 目标database
     */
    @TableField("target_database")
    private String targetDatabase;

    /**
     * 执行sql
     */
    @TableField("sqlstr")
    private String sqlStr;

    /**
     * 执行状态
     */
    @TableField("status")
    private String status;

    /**
     * 执行时间
     */
    @TableField("exec_date")
    private Date execDate;

    /**
     * 耗时
     */
    @TableField("exec_time")
    private String execTime;

    /**
     * 信息
     */
    @TableField("message")
    private String message;

}
