package com.middleware.zeus.service.k8s;

import com.middleware.zeus.common.model.Secret;

import java.util.List;
import java.util.Map;

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
    List<Secret> list(String clusterId, String namespace, Map<String, String> labels);

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

    /**
     * 获取secret
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param secretName  secret名称
     * @return String
     */
    String getUserConf(String clusterId, String namespace, String secretName);

    /**
     * 创建secret
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param conf  user conf内容
     */
    void genericSecretWithConf(String clusterId, String namespace, String name, String contentName, String conf);

    /**
     * 创建secret
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name 名称
     * @param username 用户名
     * @param password 密码
     */
    void genericSecretWithUsername(String clusterId, String namespace, String name, String username, String password);


}
