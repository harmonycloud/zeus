package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.IngressDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareValues;
import com.harmonycloud.caas.common.model.middleware.PodInfo;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/11/22 5:54 下午
 */
public interface IngressComponentService {

    /**
     * 安装集群ingress组件
     *
     * @param ingressComponentDto 集群ingress组件信息
     */
    void install(IngressComponentDto ingressComponentDto);

    /**
     * 接入集群ingress组件
     *
     * @param ingressComponentDto ingress组件对象
     */
    void integrate(IngressComponentDto ingressComponentDto);

    /**
     * 更新集群ingress组件信息
     *
     * @param ingressComponentDto ingress组件对象
     */
    void update(IngressComponentDto ingressComponentDto);

    /**
     * 获取集群ingress组件列表
     *
     * @param clusterId 集群id
     */
    List<IngressComponentDto> list(String clusterId);

    /**
     * 获取集群ingress组件列表
     *
     * @param clusterId 集群id
     */
    List<IngressComponentDto> list(String clusterId, boolean filterUnavailable);

    /**
     * 获取集群指定类型的ingress组件列表
     * @param clusterId
     * @param type
     * @return
     */
    List<IngressComponentDto> list(String clusterId, String type);

    /**
     * 获取集群指定类型的ingress组件列表
     * @param clusterId
     * @param type
     * @return
     */
    List<IngressComponentDto> list(String clusterId, String type, boolean filterUnavailable);

    /**
     * 获取集群ingress信息
     *
     * @param clusterId 集群id
     * @param ingressClassName  ingress名称
     */
    IngressComponentDto get(String clusterId, String ingressClassName);

    /**
     * 删除集群ingress组件
     *
     * @param clusterId 集群id
     * @param ingressClassName 名称
     */
    void delete(String clusterId, String ingressClassName);

    /**
     * 删除集群绑定关系
     *
     * @param clusterId 集群id
     */
    void delete(String clusterId);

    /**
     * 查询vip列表
     *
     * @param clusterId 集群id
     */
    List<String> vipList(String clusterId);

    /**
     * 查询ingress详情
     * @param clusterId
     * @param ingressClassName
     * @return
     */
    IngressComponentDto detail(String clusterId,String ingressClassName);

    /**
     * 查询pod列表
     * @param clusterId
     * @param ingressClassName
     * @return
     */
    List<PodInfo> pods(String clusterId, String ingressClassName);

    /**
     * 查询port列表
     * @param clusterId
     * @param ingressClassName
     * @return
     */
    List<IngressDTO> ports(String clusterId, String ingressClassName);

    /**
     * 重启pod
     * @param clusterId
     * @param ingressClassName
     * @param podName
     */
    void restartPod(String clusterId,String ingressClassName,String podName);

    /**
     * 查看pod yaml
     * @param clusterId
     * @param ingressClassName
     * @param podName
     * @return
     */
    String podYaml(String clusterId,String ingressClassName,String podName);

    /**
     * 查看ingress values
     * @param clusterId
     * @param ingressClassName
     * @return
     */
    String values(String clusterId, String ingressClassName);

    /**
     * 更新ingress valules
     * @param middlewareValues
     */
    void upgrade(MiddlewareValues middlewareValues);

    /**
     * 端口校验
     * @param startPort
     * @return
     */
    List<Integer> portCheck(String clusterId, String startPort);

}
