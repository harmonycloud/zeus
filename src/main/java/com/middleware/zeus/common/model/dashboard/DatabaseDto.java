package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/10/13 4:45 下午
 */
@ApiModel("中间件数据库对象")
@Accessors(chain = true)
@Data
public class DatabaseDto {

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("数据库名称")
    private String databaseName;

    @ApiModelProperty("表空间")
    private String tablespace;

    @ApiModelProperty("字符集")
    private String encoding;

    @ApiModelProperty("校验规则")
    private String collate;

    @ApiModelProperty("owner")
    private String owner;

    @ApiModelProperty("备注")
    private String comment;


}
