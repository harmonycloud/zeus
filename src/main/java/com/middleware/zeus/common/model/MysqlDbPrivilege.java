package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author liyinlong
 * @since 2022/3/25 4:42 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("mysql数据库用户权限关联")
public class MysqlDbPrivilege {

    @ApiModelProperty("关联id")
    private int id;

    @ApiModelProperty("用户名")
    private String user;

    @ApiModelProperty("数据库操作权限 1：只读，2：读写，3：仅DDL，4：仅DML")
    private int authority;

    @ApiModelProperty("数据库名")
    private String db;

    /**
     * mysql中的用户字段
     */
    private String User;

    /**
     * mysql中的数据库字段
     */
    private String Db;

    public void setUser(String user) {
        User = user;
        this.user = user;
    }

    public void setDb(String db) {
        Db = db;
        this.db = db;
    }
}
