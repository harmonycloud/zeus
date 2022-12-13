package com.middleware.zeus.service.k8s;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
public interface NodeLabelService {

    /**
     * 查询节点列表
     *
     * @param clusterId 集群id
     * @return
     */
    List<String> list(String clusterId);

}
