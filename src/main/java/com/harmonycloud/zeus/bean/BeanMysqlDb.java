package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * mysql数据库
 * </p>
 *
 * @author liyinlong
 * @since 2022-03-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mysql_db")
public class BeanMysqlDb implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value="id",type= IdType.AUTO)
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
     * 创建时间
     */
    @TableField("createtime")
    private LocalDateTime createtime;

    /**
     * 备注
     */
    @TableField("description")
    private String description;

    /**
     * 字符集
     */
    @TableField("charset")
    private String charset;


}
