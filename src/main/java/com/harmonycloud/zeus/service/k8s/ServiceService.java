package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.middleware.ServicePortDTO;

import java.util.List;

/**
 * @author tangtx
 * @date 4/2/21 4:48 PM
 */
public interface ServiceService {

    /**
     * 查询服务和端口列表
     * @param clusterId
     * @param namespace
     * @param name
     * @return
     */
    List<ServicePortDTO> list(String clusterId, String namespace, String name, String type);
}
