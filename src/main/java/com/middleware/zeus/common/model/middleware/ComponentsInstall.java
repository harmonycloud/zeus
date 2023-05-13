package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/11/4 10:23 上午
 */
@Data
@Accessors(chain = true)
@ApiModel("组件是否安装")
public class ComponentsInstall {

    private Boolean prometheus;

    private Boolean alertManager;

    private Boolean grafana;

    private Boolean minio;

    private Boolean logging;

    private Boolean ingress;

}
