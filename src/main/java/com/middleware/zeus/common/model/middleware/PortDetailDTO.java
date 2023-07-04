package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author tangtx
 * @since 2021/3/25 7:55 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("service port detail")
public class PortDetailDTO {
    private String name;
    private String port;
    private String protocol;
    private String targetPort;
    private String nodePort;
}