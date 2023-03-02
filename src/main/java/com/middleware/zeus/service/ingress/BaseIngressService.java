package com.middleware.zeus.service.ingress;

import com.middleware.caas.common.model.IngressComponentDto;
import com.middleware.caas.common.model.middleware.MiddlewareValues;
import com.middleware.zeus.bean.BeanIngressComponents;

import java.util.List;

/**
 * @description ingress
 * @author  liyinlong
 * @since 2022/8/22 5:04 下午
 */
public interface BaseIngressService {

    /**
     * 确定对应service
     *
     * @param name 组件名称
     * @return boolean
     */
    boolean support(String name);

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

    /**
     * 查询ingress详情
     * @param ingressComponents
     * @return
     */
    IngressComponentDto detail(BeanIngressComponents ingressComponents);

    /**
     * 更新values
     * @param middlewareValues
     */
    void upgrade(MiddlewareValues middlewareValues, String ingressName);


}
