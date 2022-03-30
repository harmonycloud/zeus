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
 * mysql 操作权限
 * </p>
 *
 * @author liyinlong
 * @since 2022-03-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mysql_privilege")
public class BeanMysqlPrivilege implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * mysql操作类型 例如UPDATE,SELECT
     */
    @TableField("priv")
    private String priv;

    /**
     * 权限类型：1：读写，2：只读，3：仅DDL，4：仅DML
     */
    @TableField("role")
    private Integer role;


}
