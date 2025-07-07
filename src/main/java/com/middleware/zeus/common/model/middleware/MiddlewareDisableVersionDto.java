package com.middleware.zeus.common.model.middleware;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2025/4/28 上午9:51
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiddlewareDisableVersionDto {

    @ApiModelProperty("chart名称")
    private String chartName;

    @ApiModelProperty("chart版本")
    private String chartVersion;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("版本列表")
    private List<Version> versionList;

    @Data
    @Accessors(chain = true)
    public static class Version {
        @ApiModelProperty("版本")
        private String version;

        @ApiModelProperty("是否可用")
        private Boolean enable;
    }

}
