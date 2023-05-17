package com.middleware.zeus.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @auther wangpenglei
 * @date 2023/5/17 10:26
 */
@Accessors(chain = true)
@Data
public class CustomConfigHistoryDo {

    private Integer id;

    private String clusterId;

    private String namespace;

    private String name;

    private String item;

    private String last;

    private String after;

    private Boolean restart;

    private Boolean status;

    private Date date;

    private String role;

}
