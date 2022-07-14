package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

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

}
