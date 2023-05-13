package com.middleware.zeus.common.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * mysql用户详细信息
 * @author liyinlong
 * @since 2022/3/28 3:13 下午
 */
@Data
public class MysqlUserDetail {

    /**
     * 平台保存的用户id
     */
    private int id;

    /**
     * mysql用户名
     */
    private String user;

    /**
     * 数据库用户关联列表
     */
    private List<MysqlDbPrivilege> dbs;

    /**
     * 用户描述
     */
    private String description;

    /**
     * 用户创建时间
     */
    private Date createTime;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 密码是否可用
     */
    private boolean passwordCheck;

    /**
     * mysql中保存的用户名
     */
    private String User;

    public void setUser(String user) {
        User = user;
        this.user = User;
    }

}
