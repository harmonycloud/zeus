package com.middleware.zeus.service.k8s;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/12/1 8:31 下午
 */
public interface ServiceAccountService {

    /**
     * 获取ServiceAccount
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name 名称
     * @return ServiceAccount
     */
    ServiceAccount get(String clusterId, String namespace, String name);

    /**
     * 创建ServiceAccount
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name ServiceAccount名称
     * @param secrets Secret列表
     */
    void create(String clusterId, String namespace, String name, List<String> secrets);

    void bindImagePullSecret(String clusterId, String namespace, ServiceAccount sa, List<Secret> secrets);

}
