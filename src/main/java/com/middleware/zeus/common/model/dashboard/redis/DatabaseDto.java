package com.middleware.zeus.common.model.dashboard.redis;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/10/27 11:20 上午
 */
@ApiModel("redis db对象")
@Accessors(chain = true)
@Data
public class DatabaseDto {

    @ApiModelProperty("数据库名称(0、1...)")
    private Integer db;

    @ApiModelProperty("key数量")
    private Integer size;

}
