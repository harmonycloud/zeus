package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import org.springframework.stereotype.Component;

/**
 * @author dengyulong
 * @date 2021/03/30
 */
@Component
public class ServiceAccountWrapper {

    public ServiceAccount get(String clusterId, String namespace, String name) {
        return K8sClient.getClient(clusterId).serviceAccounts().inNamespace(namespace).withName(name).get();
    }

    public ServiceAccount create(String clusterId, ServiceAccount serviceAccount) {
        return K8sClient.getClient(clusterId).serviceAccounts().inNamespace(serviceAccount.getMetadata().getNamespace())
            .create(serviceAccount);
    }

    public ServiceAccount createOrReplace(String clusterId, ServiceAccount serviceAccount) {
        return K8sClient.getClient(clusterId).serviceAccounts().inNamespace(serviceAccount.getMetadata().getNamespace())
                .createOrReplace(serviceAccount);
    }

}
