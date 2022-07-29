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
 * @date 2021/11/11 下午3:53
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mail_to_user")
public class BeanMailToUser {

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Integer userId;
    /**
     * 告警设置ID
     */
    @TableField("alert_setting_id")
    private Integer alertSettingId;

}
