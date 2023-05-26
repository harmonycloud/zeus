package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @since 2021/6/23 10:46 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "PVC")
public class PersistentVolumeClaim {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("PVC名称")
    private String name;

    @ApiModelProperty("标签")
    private Map<String, String> labels;

    @ApiModelProperty("storageClass名称")
    private String storageClassName;

    @ApiModelProperty("volumeMode")
    private String volumeMode;

    @ApiModelProperty("accessModes")
    private List<String> accessModes;

    @ApiModelProperty("状态")
    private String phase;

    @ApiModelProperty("申请量")
    private Double request;

    @ApiModelProperty("容量")
    private Double capacity;

    @ApiModelProperty("创建时间")
    private Date createTime;
}
