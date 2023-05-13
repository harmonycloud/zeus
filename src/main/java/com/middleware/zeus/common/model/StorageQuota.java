package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/1/5 7:38 下午
 */
@Data
@ApiModel("存储资源")
@NoArgsConstructor
@Accessors(chain = true)
public class StorageQuota {

    @ApiModelProperty("存储名称")
    public String name;

    @ApiModelProperty("存储id")
    public String storageId;

    @ApiModelProperty("存储名称")
    public List<String> storageClass;

    @ApiModelProperty("存储类型")
    public List<String> storageType;

    @ApiModelProperty("存储资源")
    public QuotaBase storage;

}
