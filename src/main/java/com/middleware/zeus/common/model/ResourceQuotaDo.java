package com.middleware.zeus.common.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/1/5 7:35 下午
 */
@Data
@ApiModel("资源")
@Accessors(chain = true)
@AllArgsConstructor
public class ResourceQuotaDo extends Quota {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群别名")
    private String clusterNickName;

    @ApiModelProperty("是否开启可用域")
    private boolean availableDomain;

    @ApiModelProperty("存储配额")
    private List<StorageQuota> storageList;

    public ResourceQuotaDo(){
        this.storageList = new ArrayList<>();
    }

    public boolean isEmpty(){
        return this.getCpu() == null || this.getMemory() == null || CollectionUtils.isEmpty(this.getStorageList());
    }
}
