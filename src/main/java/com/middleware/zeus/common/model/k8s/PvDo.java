package com.middleware.zeus.common.model.k8s;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/1/10 11:24 上午
 */
@Data
@Accessors(chain = true)
public class PvDo {

    @ApiModelProperty("pv名称")
    private String pvName;

    @ApiModelProperty("pvc名称")
    private String pvcName;

    @ApiModelProperty("回收策略")
    private String reclaimPolicy;

    @ApiModelProperty("状态")
    private String status;

}
