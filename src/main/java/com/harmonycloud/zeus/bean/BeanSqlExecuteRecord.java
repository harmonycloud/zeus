package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xutianhong
 * @Date 2022/10/21 9:44 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sql_execute_record")
public class BeanSqlExecuteRecord implements Serializable {

    private static final long serialVersionUID = -67082778931643L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 目标database
     */
    @TableField("database")
    private String database;

    /**
     * 执行sql
     */
    @TableField("sql")
    private String sql;

    /**
     * 执行状态
     */
    @TableField("status")
    private String status;

    /**
     * 行数
     */
    @TableField("line")
    private String line;

    /**
     * 执行时间
     */
    @TableField("date")
    private Date date;

    /**
     * 耗时
     */
    @TableField("time")
    private String time;

    /**
     * 信息
     */
    @TableField("message")
    private String message;

}
