package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.ResourceQuotaDo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
@Accessors(chain = true)
@Data
@ApiModel("命名空间资源配额")
public class ResourceQuotaDTO{

    @ApiModelProperty("命名空间")
    private String namespace;


    @ApiModelProperty("资源配额")
    private ResourceQuotaDo resourceQuotaDo;

}
