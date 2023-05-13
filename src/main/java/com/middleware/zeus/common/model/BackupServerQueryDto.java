package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/14 3:35 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("备份服务列表查询dto")
public class BackupServerQueryDto {

    @ApiModelProperty("集群列表")
    private List<String> clusterIds;

    @ApiModelProperty("搜索关键词")
    private String keyword;

    @ApiModelProperty("是否查询详细信息")
    private Boolean withDetail;

}
