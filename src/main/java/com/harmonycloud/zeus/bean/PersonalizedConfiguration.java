package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author yushuaikang
 * @date 2021/11/3 下午1:57
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("personal_config")
public class PersonalizedConfiguration {

    /**
     * 自增id
     */
    @TableId (value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 背景图
     */
    @TableField (value = "background_image")
    private String backgroundImage;

    /**
     * 背景图地址
     */
    @TableField (value = "background_image_path")
    private String backgroundPath;

    /**
     * 登录页logo
     */
    @TableField (value = "login_logo")
    private String loginLogo;

    /**
     * 登录页logo地址
     */
    @TableField (value = "login_logo_path")
    private String loginLogoPath;

    /**
     * 主页logo
     */
    @TableField (value = "home_logo")
    private String homeLogo;

    /**
     * 主页logo地址
     */
    @TableField (value = "home_logo_path")
    private String homeLogoPath;

    /**
     * 平台名称
     */
    @TableField (value = "platform_name")
    private String platformName;

    /**
     * 标语
     */
    @TableField (value = "slogan")
    private String slogan;

    /**
     * 版权声明
     */
    @TableField (value = "copyright_notice")
    private String copyrightNotice;

    /**
     * tab标题
     */
    @TableField (value = "title")
    private String title;

    /**
     * 创建时间
     */
    @TableField (value = "create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField (value = "update_time")
    private Date updateTime;

    /**
     * 是否默认
     */
    @TableField (value = "status")
    private String status;
}
