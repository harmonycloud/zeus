package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.middleware.MiddlewareInfoDTO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/11/16 3:25 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "批量安装集群组件dto")
public class MultipleComponentsInstallDto {

    private List<ClusterComponentsDto> clusterComponentsDtoList;

    private List<MiddlewareInfoDTO> middlewareInfoDTOList;

}
