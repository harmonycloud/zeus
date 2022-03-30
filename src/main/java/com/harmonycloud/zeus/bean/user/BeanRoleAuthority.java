package com.harmonycloud.zeus.bean.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2022/3/28 4:02 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("role_authority")
public class BeanRoleAuthority implements Serializable {

    private static final long serialVersionUID = -4612389176982342L;

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
     * 类型
     */
    @TableField("type")
    private String type;
    /**
     * 能力
     */
    @TableField("power")
    private String power;

}
