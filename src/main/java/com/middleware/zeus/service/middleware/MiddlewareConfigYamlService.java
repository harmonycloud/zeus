package com.middleware.zeus.service.middleware;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/12/23 2:23 下午
 */
public interface MiddlewareConfigYamlService {

    /**
     * 获取配置文件名称列表
     *
     * @param clusterId 集群
     * @param namespace 分区
     * @param name  名称
     * @param type 类型
     * @param chartVersion 版本
     */
    List<String> nameList(String clusterId, String namespace, String name, String type, String chartVersion);

    /**
     * 获取配置文件yaml
     *
     * @param clusterId 集群
     * @param namespace 分区
     * @param configMapName 配置文件名称
     */
    String yaml(String clusterId, String namespace, String configMapName);

    /**
     * 更新配置文件
     *
     * @param clusterId 集群
     * @param namespace 分区
     * @param configMapName 配置文件名称
     * @param config 配置文件
     */
    void update(String clusterId, String namespace, String configMapName, String config);

}
