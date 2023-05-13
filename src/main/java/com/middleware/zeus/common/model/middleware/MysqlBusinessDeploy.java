package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yushuaikang
 * @date 2021/10/29 下午3:13
 */
@Data
@Accessors(chain = true)
@ApiModel("mysql业务数据库")
public class MysqlBusinessDeploy {

    @ApiModelProperty("业务数据库名称")
    private String database;

    @ApiModelProperty("业务数据库密码")
    private String pwd;

    @ApiModelProperty("业务用户")
    private String user;

}