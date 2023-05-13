package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/1/5 5:01 下午
 */
@Data
@ApiModel("资源")
@Accessors(chain = true)
public class Quota {

    @ApiModelProperty("cpu资源")
    private QuotaBase cpu;

    @ApiModelProperty("内存资源")
    private QuotaBase memory;

    public Quota() {
        this.cpu = new QuotaBase();
        this.memory = new QuotaBase();
    }

}
