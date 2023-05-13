package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xutianhong
 * @Date 2022/2/24 8:31 下午
 */
@Data
@ApiModel("资源统计方式")
@NoArgsConstructor
public class MonitorResourceQuotaBase {

    private Double total;

    private Double used;

    private Double usable;

    private Double usage;

    private Double request;

}
