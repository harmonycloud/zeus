package com.middleware.zeus.service.k8s;

import com.middleware.caas.common.model.middleware.IngressDTO;
import com.middleware.caas.common.model.middleware.IngressRuleDTO;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.ServiceDTO;

import java.util.List;
import java.util.Set;

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
    List<IngressDTO> list(String clusterId, String namespace, String keyword, String projectId);

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
     * @param serviceList 服务列表
     */
    void checkServiceTcpPort(MiddlewareClusterDTO cluster, String ingressClassName, String exposeType, List<ServiceDTO> serviceList);

    /**
     * 获取集群已被使用的端口列表
     * @param cluster
     * @return
     */
    Set<Integer>  getUsedPortSet(MiddlewareClusterDTO cluster, Boolean filter);

    /**
     * 创建中间件对外访问
     *
     * @param cluster     集群
     * @param namespace   命名空间
     * @param serviceList 服务列表
     * @param checkPort   校验端口
     */
//    void createIngressTcp(MiddlewareClusterDTO cluster, String namespace, List<ServiceDTO> serviceList, boolean checkPort);

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
     * 删除中间件对外访问
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param type           中间件类型
     */
    void delete(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 查询单个中间件对外访问
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     * @return
     */
    List<IngressDTO> get(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 查询单个中间件对外访问,过滤不需要显示的服务暴露
     * 信息，例如rocketmq的broker信息
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    List<IngressDTO> getMiddlewareIngress(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 查询中间件ingress列表
     *
     * @param clusterId  集群id
     * @param namespace  命名空间
     * @param helmReleaseName   label键
     * @return
     */
    List<IngressRuleDTO> getHelmIngress(String clusterId, String namespace, String helmReleaseName);

    /**
     *  查询所有ingress
     * @author  liyinlong
     * @since 2021/9/7 5:52 下午
     * @param clusterId
     * @param namespace
     * @param keyword
     * @return
     */
    List<IngressDTO> listAllIngress(String clusterId, String namespace, String keyword, String projectId);

    /**
     * 查询所有中间件ingress(不同于查询所有ingress,此方法会过滤掉不是通过中间件平台创建的服务暴露信息)
     * @param clusterId
     * @param namespace
     * @param keyword
     * @return
     */
    List<IngressDTO> listAllMiddlewareIngress(String clusterId, String namespace, String keyword, String projectId);

    /**
     * 获取一个未被占用的ingress端口
     * @param clusterId
     * @param ingressClassName
     * @return
     */
    int getAvailablePort(String clusterId, String ingressClassName);

    /**
     * 获取暴露用ip
     * @param cluster 集群
     * @param ingressDTO  ingress
     * @return String
     */
    String getExposeIp(MiddlewareClusterDTO cluster, IngressDTO ingressDTO);

    /**
     * 校验服务端口是否可用
     *
     * @param clusterId
     * @param port
     */
    void verifyServicePort(String clusterId, String ingressClassName, String exposeType, Integer port);

    /**
     * 查询ingress ip
     * @param clusterId
     * @param ingressClassName
     * @return
     */
    List<String> listIngressIp(String clusterId, String ingressClassName);

    /**
     * 获取一个可用的ingress ip
     */
    String getIngressIp(String clusterId, String ingressClassName);

    /**
     * 获取hostnetwork访问信息
     * @return
     */
    List<IngressDTO> getHostNetworkAddress(String clusterId, String namespace, String type, String middlewareName);

}
