package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author tangtx
 * @since 2021/3/25 7:55 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("service port detail")
public class ServicePortDTO {

    @ApiModelProperty("服务名")
    private String serviceName;
    @ApiModelProperty("服务")
    private List<PortDetailDTO> portDetailDtoList;
    @ApiModelProperty("集群ip")
    private String clusterIP;

    /**
     * 服务用途
     */
    private String servicePurpose;

    /**
     * 集群内服务访问地址
     */
    private String internalAddress;

    /**
     * 中间件图片
     */
    private String imagePath;

    /**
     * selector
     */
    private Map<String, String> selector;

}