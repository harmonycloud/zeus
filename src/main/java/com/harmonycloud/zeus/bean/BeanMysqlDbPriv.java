package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * Mysql数据库授权
 * </p>
 *
 * @author liyinlong
 * @since 2022-03-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mysql_db_priv")
public class BeanMysqlDbPriv implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * mysql服务限定名
     */
    @TableField("mysql_qualified_name")
    private String mysqlQualifiedName;

    /**
     * 数据库名
     */
    @TableField("db")
    private String db;

    /**
     * 用户名
     */
    @TableField("user")
    private String user;

    /**
     * 权限：1：读写，2：只读，3：仅DDL，4：仅DML
     */
    @TableField("authority")
    private Integer authority;


}
