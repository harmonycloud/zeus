package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
@ApiModel("es集群")
public class EsDTO extends Middleware {

    private static final long serialVersionUID = 3939614290663633587L;

}
