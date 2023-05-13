package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@ApiModel("存储信息")
@Accessors(chain = true)
@Data
public class MiddlewareClusterStorage implements Serializable {

    private static final long serialVersionUID = 7612378875631276351L;

    @ApiModelProperty("备份相关")
    private Map<String, Object> backup;
    @ApiModelProperty("支持存储配额，存储类型:配额")
    private List<MiddlewareClusterStorageSupport> support;

}
