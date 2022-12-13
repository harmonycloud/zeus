package com.middleware.zeus.bean.user;

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
 * @Date 2022/3/24 8:30 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("project")
public class BeanProject implements Serializable {

    private static final long serialVersionUID = -685617284133433L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 项目id
     */
    @TableField("project_id")
    private String projectId;
    /**
     * 项目名称
     */
    @TableField("name")
    private String name;
    /**
     * 别名
     */
    @TableField("alias_name")
    private String aliasName;
    /**
     * 描述
     */
    @TableField("description")
    private String description;
    /**
     * 项目成员，以逗号分隔
     */
    @TableField("user")
    private String user;
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

}
