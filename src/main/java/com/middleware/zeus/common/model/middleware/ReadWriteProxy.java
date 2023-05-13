package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/7/13 4:22 下午
 */

@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("读写分离模式")
public class ReadWriteProxy {

    @ApiModelProperty("是否开启")
    private Boolean enabled;

    @ApiModelProperty("实例数")
    private Integer replicas;

    @ApiModelProperty("节点资源")
    private MiddlewareQuota quota;

}
