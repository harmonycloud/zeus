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
 * @date 2021/11/9 下午8:05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ding_robot_info")
public class DingRobotInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "webhook")
    private String webhook;

    @TableField(value = "secret_key")
    private String secretKey;

    @TableField(value = "creat_time")
    private Date time;


}
