package com.middleware.zeus.service.k8s;

import com.middleware.caas.common.model.ActiveAreaClusterDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/8/13 1:44 下午
 */
public interface ActiveAreaClusterService {

    /**
     * 查询集群可用区信息列表
     *
     * @return List<ActiveAreaClusterDto>
     */
    List<ActiveAreaClusterDto> list();

    /**
     * 开启/关闭集群双活
     *
     * @param clusterId 集群id
     */
    void update(String clusterId, Boolean active);

}
