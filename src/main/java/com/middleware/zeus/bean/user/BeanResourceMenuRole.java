package com.middleware.zeus.bean.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xutianhong
 * @Date 2021/7/28 5:39 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("resource_menu_role")
public class BeanResourceMenuRole  {

    private static final long serialVersionUID = -6897323133L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 角色id
     */
    @TableField("role_id")
    private Integer roleId;
    /**
     * 菜单栏id
     */
    @TableField("resource_menu_id")
    private Integer resourceMenuId;
    /**
     * 是否可用
     */
    @TableField("available")
    private Boolean available;

}
