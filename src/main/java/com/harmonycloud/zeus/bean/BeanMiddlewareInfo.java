package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;

import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 中间件表
 * </p>
 *
 * @author skyview
 * @since 2021-03-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("middleware_info")
public class BeanMiddlewareInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 中间件名称
     */
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 类型
     */
    @TableField("type")
    private String type;

    /**
     * 版本
     */
    @TableField("version")
    private String version;

    /**
     * 图片
     */
    @TableField("image")
    private byte[] image;

    /**
     * 图片地址
     */
    @TableField("image_path")
    private String imagePath;

    /**
     * chart包名称
     */
    @TableField("chart_name")
    private String chartName;

    /**
     * chart包版本
     */
    @TableField("chart_version")
    private String chartVersion;

    /**
     * operator名称
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * grafanna的id
     */
    @TableField("grafana_id")
    private String grafanaId;

    /**
     * 创建人
     */
    @TableField("creator")
    private String creator;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 修改人
     */
    @TableField("modifier")
    private String modifier;

    /**
     * 创建时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 是否可用：0-非官方 1-官方
     */
    @TableField("official")
    private Boolean official;

    /**
     * 是否可用：0-非官方 1-官方
     */
    @TableField("chart")
    private byte[] chart;

    /**
     * 修改人
     */
    @TableField("compatible_versions")
    private String compatibleVersions;
}
