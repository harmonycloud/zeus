package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author dengyulong
 * @date 2021/04/16
 */
@ApiModel("集群的ingress信息")
@Accessors(chain = true)
@Data
public class MiddlewareClusterIngress implements Serializable {

    private static final long serialVersionUID = 7722113559659628626L;

    @ApiModelProperty("ingressController的地址")
    private String address;
    @ApiModelProperty("ingressController的tcp配置")
    private IngressConfig tcp;
    @ApiModelProperty("ingressController的tcp配置")
    private String ingressClassName;

    @ApiModel("ingressController的配置")
    @Accessors(chain = true)
    @Data
    public static class IngressConfig implements Serializable {

        private static final long serialVersionUID = 2764829627747304955L;

        @ApiModelProperty("是否开启")
        private boolean enabled;
        @ApiModelProperty("配置名称")
        private String configMapName;
        @ApiModelProperty("配置名称")
        private String namespace;
    }

}

