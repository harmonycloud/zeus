package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.middleware.StorageClassInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/6/6 4:24 下午
 */
@Data
@Accessors(chain = true)
@ApiModel("存储管理业务对象")
public class StorageDto {

    @ApiModelProperty("存储服务id")
    private String storageId;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群中文名")
    private String clusterAliasName;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("中文别名")
    private String aliasName;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("存储总额")
    private Double totalStorage;

    @ApiModelProperty("是否双活")
    private Boolean isActiveActive;

    @ApiModelProperty("是否双活")
    private QuotaBase quota;

    @ApiModelProperty("绑定的sc")
    private List<StorageClassInfo> storageClassList = new ArrayList<>();

}
