package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description 数据库对象
 * @author  liyinlong
 * @since 2022/10/19 5:54 下午
 */
@ApiModel("mysql数据库对象")
@Accessors(chain = true)
@Data
public class DatabaseDto {

    @ApiModelProperty("数据库名称")
    private String db;

    @ApiModelProperty("字符集")
    private String character;

    @ApiModelProperty("校验规则")
    private String collate;

}
