package com.harmonycloud.zeus.service.k8s;

import java.util.List;
import java.util.Map;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;

/**
 * @author xutianhong
 * @Date 2021/4/1 5:06 下午
 */
public interface MiddlewareCRService {

    /**
     * 查询中间件列表
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param type      中间件类型
     * @param detail    是否返回详细信息
     * @return List<Middleware>
     */
    List<Middleware> list(String clusterId, String namespace, String type, Boolean detail);

    /**
     * 查询中间件列表
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param label     标签
     * @return List<MiddlewareCRD>
     */
    List<MiddlewareCR> listCR(String clusterId, String namespace, Map<String, String> label);

    /**
     * 查询中间件简单详情
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param type      中间件类型
     * @param name      中间件名称
     * @return
     */
    Middleware simpleDetail(String clusterId, String namespace, String type, String name);

    /**
     * 查询中间件cr
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param type      中间件类型
     * @param name      中间件名称
     * @return
     */
    MiddlewareCR getCR(String clusterId, String namespace, String type, String name);

    /**
     * 获取中间件pvc名称
     *
     * @param mw
     * @return
     */
    List<String> getPvc(MiddlewareCR mw);

    /**
     * 获取中间件pvc名称
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param type      中间件类型
     * @param name      中间件名称
     * @return
     */
    List<String> getPvc(String clusterId, String namespace, String type, String name);

    /**
     * 查询中间件cr并且检验是否在运行中
     *
     * @param middleware 中间件参数，clusterId/namespace/type/name
     * @return
     */
    MiddlewareCR getCRAndCheckRunning(Middleware middleware);

    /**
     * crd简单转middleware
     *
     * @param mw crd
     * @return
     */
    Middleware simpleConvert(MiddlewareCR mw);

    /**
     * 检查中间件是否存在
     * @param clusterId  集群ID
     * @param namespace  命名空间
     * @param type       类型
     * @param middlewareName 中间件名称
     * @return boolean
     */
    boolean checkIfExist(String clusterId, String namespace, String type, String middlewareName);

}
