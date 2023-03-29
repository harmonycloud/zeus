package com.middleware.zeus.service.k8s;

import com.middleware.caas.common.model.ClusterComponentsDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/29 11:19 上午
 */
public interface AlertManagerInfoService {

    List<ClusterComponentsDto> list(String clusterId);

}
