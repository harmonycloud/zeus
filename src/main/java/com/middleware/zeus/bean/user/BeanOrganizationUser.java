package com.middleware.zeus.bean.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2023/3/7 9:31 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("organization_user")
public class BeanOrganizationUser implements Serializable {

    private static final long serialVersionUID = -681023676423L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 组织id
     */
    @TableField("organ_id")
    private String organId;
    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 角色id
     */
    @TableField("role_id")
    private Integer roleId;

}
