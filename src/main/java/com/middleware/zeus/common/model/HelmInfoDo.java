package com.middleware.zeus.common.model;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2023/10/11 10:57 上午
 */
@Accessors(chain = true)
@NoArgsConstructor
@Data
@ApiModel("helm release info")
public class HelmInfoDo {

    @ApiModelProperty("集群id")
    private String clusterId;
    @ApiModelProperty("app版本")
    private String namespace;
    @ApiModelProperty("名称")
    private String name;
    @ApiModelProperty("更新时间")
    private Date updateTime;
    @ApiModelProperty("chart包名称")
    private String chartName;
    @ApiModelProperty("chart包版本")
    private String chartVersion;
    @ApiModelProperty("release版本")
    private String releaseVersion;
    @ApiModelProperty("values.yaml")
    private JSONObject values;

}
