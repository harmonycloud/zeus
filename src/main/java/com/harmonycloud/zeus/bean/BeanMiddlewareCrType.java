package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2022/6/9 2:31 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("middleware_cr_type")
public class BeanMiddlewareCrType implements Serializable {

    private static final long serialVersionUID = -67082364796432L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 中间件类型
     */
    @TableField("chart_name")
    private String chartName;

    /**
     * cr类型
     */
    @TableField("cr_type")
    private String crType;

}
