package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;

import java.util.List;
import java.util.Map;

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
     * 查询pod列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param key            关键词过滤
     * @return
     */
    List<PodInfo> list(String clusterId, String namespace, String key);

    /**
     * 查询pod列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param labels         标签
     * @return
     */
    List<PodInfo> list(String clusterId, String namespace, Map<String, String> labels);

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

    /**
     * 查询中间件pod列表
     *
     * @param mw             中间件cr
     * @param clusterId      集群id
     * @param namespace      分区
     * @param middlewareName 中间件名称
     * @param type           中间件类型
     * @return
     */
    Middleware listPods(MiddlewareCR mw, String clusterId, String namespace, String middlewareName, String type);
    /**
     * 查询pod yaml
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param type           中间件类型
     * @param podName        pod名称
     */
    String yaml(String clusterId, String namespace, String middlewareName, String type, String podName);

}
