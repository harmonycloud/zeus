package com.middleware.zeus.service.k8s;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/12/1 8:31 下午
 */
public interface ServiceAccountService {

    ServiceAccount get(String clusterId, String namespace, String name);

    void bindImagePullSecret(String clusterId, String namespace, ServiceAccount sa, List<Secret> secrets);

}
