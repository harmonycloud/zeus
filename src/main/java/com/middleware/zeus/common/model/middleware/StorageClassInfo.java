package com.middleware.zeus.common.model.middleware;

import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@ApiModel
@Accessors(chain = true)
@Data
public class StorageClassInfo {

    @ApiModelProperty("存储名称")
    private String name;
    @ApiModelProperty("存储类型")
    private String volumeType;
    @ApiModelProperty("存储使用量")
    private String storageUsed;
    @ApiModelProperty("存储配额")
    private String storageQuota;
//    @ApiModelProperty("存储总量")
//    private String storageLimit;
    @ApiModelProperty("标签")
    private Map<String, String> labels;
    @ApiModelProperty("存储供应者")
    private String provisioner;
    @ApiModelProperty("存储释放策略")
    private String reclaimPolicy;
    @ApiModelProperty("卷绑定模式")
    private String volumeBindingMode;
//    @ApiModelProperty("是否允许卷扩容")
//    private boolean allowVolumeExpansion;
    @ApiModelProperty("参数")
    private Map<String, String> parameters;
    @ApiModelProperty("创建时间")
    private String createTime;
    @ApiModelProperty("存储卷状态")
    private Integer status;
    @ApiModelProperty("vg名称")
    private String vgName;
    @ApiModelProperty("fs类型")
    private String fsType;
    @ApiModelProperty("双活所属分区")
    private String activeZone;
    @ApiModelProperty("存储id")
    private String storageId;

}
