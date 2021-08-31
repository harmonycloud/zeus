package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.middleware.IngressDTO;
import com.harmonycloud.caas.common.model.middleware.IngressRuleDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.ServiceDTO;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface IngressService {

    /**
     * 查询中间件对外访问列表
     * @param clusterId
     * @param namespace
     * @param keyword
     * @return
     */
    List<IngressDTO> list(String clusterId, String namespace, String keyword);

    /**
     * 创建中间件对外访问
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param ingressDTO     对外访问
     */
    void create(String clusterId, String namespace, String middlewareName, IngressDTO ingressDTO);

    /**
     * 校验中间件对外访问端口
     *
     * @param cluster     集群
     * @param namespace   命名空间
     * @param serviceList 服务列表
     */
    void checkIngressTcpPort(MiddlewareClusterDTO cluster, String namespace, List<ServiceDTO> serviceList);

    /**
     * 创建中间件对外访问
     *
     * @param cluster     集群
     * @param namespace   命名空间
     * @param serviceList 服务列表
     * @param checkPort   校验端口
     */
    void createIngressTcp(MiddlewareClusterDTO cluster, String namespace, List<ServiceDTO> serviceList, boolean checkPort);

    /**
     * 删除中间件对外访问
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param name           对外访问名称
     * @param ingressDTO     对外路由实体
     */
    void delete(String clusterId, String namespace, String middlewareName, String name, IngressDTO ingressDTO);

    /**
     * 查询单个中间件对外访问
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    List<IngressDTO> get(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 查询单个中间件对外访问
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @param name           对外访问名称
     * @param exposeType     对外暴露方式
     * @param protocol       协议
     * @return
     */
    IngressDTO get(String clusterId, String namespace, String type, String middlewareName, String name, String exposeType, String protocol);

    /**
     * 查询中间件ingress列表
     *
     * @param clusterId  集群id
     * @param namespace  命名空间
     * @param helmReleaseName   label键
     * @return
     */
    List<IngressRuleDTO> getHelmIngress(String clusterId, String namespace, String helmReleaseName);

}
