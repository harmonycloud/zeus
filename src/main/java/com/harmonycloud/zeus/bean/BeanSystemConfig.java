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
 * 系统配置表
 * </p>
 *
 * @author liyinlong
 * @since 2022-03-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("system_config")
public class BeanSystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 配置名
     */
    @TableField("config_name")
    private String configName;

    /**
     * 配置值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 创建人
     */
    @TableField("create_user")
    private String createUser;

    /**
     * 修改人
     */
    @TableField("update_user")
    private String updateUser;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;


}
