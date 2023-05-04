package com.middleware.zeus.service.k8s;

import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.List;
import java.util.Map;


/**
 * @author tangtx
 * @date 2021/03/26 9:30 AM
 */
public interface ConfigMapService {

    /**
     * 查询配置文件
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name      配置文件名称
     * @return
     */
    ConfigMap get(String clusterId, String namespace, String name);

    /**
     * 查询配置文件
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param configMap 配置文件
     * @return
     */
    void create(String clusterId, String namespace, ConfigMap configMap);

    /**
     * 查询配置文件
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name      配置文件名称
     * @param data      配置文件data数据
     * @return
     */
    void create(String clusterId, String namespace, String name, Map<String, String> data);

    /**
     * 查询配置文件
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name      配置文件名称
     * @return
     */
    void delete(String clusterId, String namespace, String name);

    /**
     * 查询配置文件
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param configMap 配置文件
     * @return
     */
    void update(String clusterId, String namespace, ConfigMap configMap);

    /**
     * 获取配置文件列表
     * @param clusterId 集群id
     * @param namespace 分区
     * @param labels 标签
     * @return
     */
    List<ConfigMap> list(String clusterId, String namespace, Map<String,String> labels);

}
