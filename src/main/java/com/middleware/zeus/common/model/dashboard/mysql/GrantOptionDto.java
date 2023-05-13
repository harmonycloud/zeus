package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/10/21 9:07 上午
 */
@ApiModel("授权详情")
@Accessors(chain = true)
@Data
public class GrantOptionDto {

    @ApiModelProperty("序号（前端不用传）")
    private Integer id;

    @ApiModelProperty("mysql用户")
    private String username;

    @ApiModelProperty("允许用户登陆MySQL所使用的IP地址")
    private String host;

    @ApiModelProperty("数据库名称")
    private String db;

    @ApiModelProperty("表名称")
    private String table;

    @ApiModelProperty("权限(一般为字符串,多个权限用逗号分隔)")
    private String privilege;

    @ApiModelProperty("授权类型(1：只读，2：管理，3：读写)")
    private int privilegeType;

    @ApiModelProperty("能否传递权限")
    private Boolean grantAble;
}
