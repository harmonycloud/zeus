package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/3/28 1:54 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("mysql用户信息")
public class MysqlUserDTO {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群分区名称")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("数据库用户名")
    private String user;

    @ApiModelProperty("用户密码")
    private String password;

    @ApiModelProperty("用户二次确认密码")
    private String confirmPassword;

    @ApiModelProperty("用户描述")
    private String description;

    @ApiModelProperty("用户数据库关联列表")
    private List<MysqlDbPrivilege> privilegeList;

}