package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.Secret;

import java.util.List;

/**
 * @author xutianhong
 * @since 2021/6/23 10:55 上午
 */
public interface SecretService {

    /**
     * 获取secret列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @return List<Secret>
     */
    List<Secret> list(String clusterId, String namespace);

    /**
     * 获取secret
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param secretName  secret名称
     */
    Secret get(String clusterId, String namespace, String secretName);

    /**
     * 创建secret
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param secret  secret对象
     */
    void create(String clusterId, String namespace, Secret secret);

    /**
     * 创建或更新secret
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param secret  secret对象
     */
    void createOrReplace(String clusterId, String namespace, Secret secret);

    /**
     * 创建或更新secret
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param secret  secret对象
     */
    void createOrReplace(String clusterId, String namespace, io.fabric8.kubernetes.api.model.Secret secret);
}
