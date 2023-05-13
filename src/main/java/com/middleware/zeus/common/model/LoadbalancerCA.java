package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author chwetion
 * @since 2021/1/8 1:32 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "负载均衡证书")
public class LoadbalancerCA {
    @ApiModelProperty("证书名称")
    private String name;
    @ApiModelProperty("证书key")
    private String key;
    @ApiModelProperty("证书crt")
    private String crt;
    @ApiModelProperty("证书签发机构(查询时返回)")
    private String organ;
    @ApiModelProperty("截止日期")
    private String deadTime;
    @ApiModelProperty("关联服务-域名")
    private List<String> relatedServiceNameAndDomain;
}
