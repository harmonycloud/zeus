package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author xutianhong
 * @Date 2022/2/24 8:29 下午
 */
@Data
@ApiModel("监控资源")
@AllArgsConstructor

public class MonitorResourceQuota extends Quota {

    @ApiModelProperty("存储资源")
    private QuotaBase storage;

    public MonitorResourceQuota() {
        this.storage = new QuotaBase();
    }

}
