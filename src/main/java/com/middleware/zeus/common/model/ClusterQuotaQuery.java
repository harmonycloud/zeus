package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/28 4:21 下午
 */
@Accessors(chain = true)
@Data
public class ClusterQuotaQuery {

    @ApiModelProperty("集群id")
    private List<String> clusterIdList;

    @ApiModelProperty("集群id")
    private Boolean detail;

}
