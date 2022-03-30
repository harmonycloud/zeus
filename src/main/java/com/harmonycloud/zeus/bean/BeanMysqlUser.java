package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * mysql用户
 * </p>
 *
 * @author liyinlong
 * @since 2022-03-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mysql_user")
public class BeanMysqlUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * mysql服务限定名
     */
    @TableField("mysql_qualified_name")
    private String mysqlQualifiedName;

    /**
     * 用户名
     */
    @TableField("user")
    private String user;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

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


}
