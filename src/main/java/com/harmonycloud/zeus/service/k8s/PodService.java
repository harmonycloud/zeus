package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.PodInfo;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface PodService {

    /**
     * 查询pod列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param type           中间件类型
     * @return
     */
    Middleware list(String clusterId, String namespace, String middlewareName, String type);

    /**
     * 查询pod列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @return
     */
    List<PodInfo> list(String clusterId, String namespace);

    /**
     * 重启pod
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param type           中间件类型
     * @param podName        pod名称
     */
    void restart(String clusterId, String namespace, String middlewareName, String type, String podName);

}
