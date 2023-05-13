package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/2/24 8:31 下午
 */
@Data
@ApiModel("资源统计方式")
@NoArgsConstructor
@Accessors(chain = true)
public class QuotaBase {

    private Double total;

    private Double used;

    private Double usable;

    private Double usage;

    private Double request;

    private Double allocatable;

    private Double occupy;

}
