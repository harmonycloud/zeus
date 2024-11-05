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
 * @Date 2024/10/25 10:43 AM
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_resource_translate_config")
public class BeanSysResourceTranslateConfig implements Serializable {

    private static final long serialVersionUID = -656728193543L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("group_name")
    private String groupName;
    @TableField("unique_value")
    private String uniqueValue;
    @TableField("property")
    private String property;
    @TableField("language_code")
    private String languageCode;
    @TableField("translation")
    private String translation;

}
