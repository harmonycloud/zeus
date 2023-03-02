package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import org.springframework.stereotype.Component;

/**
 * @author dengyulong
 * @date 2021/03/30
 */
@Component
public class RbacWrapper {

    public ClusterRoleBinding get(String clusterId, String name) {
        return K8sClient.getClient(clusterId).rbac().clusterRoleBindings().withName(name).get();
    }
    
    public ClusterRoleBinding create(String clusterId, ClusterRoleBinding clusterRoleBinding) {
        return K8sClient.getClient(clusterId).rbac().clusterRoleBindings().create(clusterRoleBinding);
    }
    
    public ClusterRoleBinding update(String clusterId, ClusterRoleBinding clusterRoleBinding) {
        return K8sClient.getClient(clusterId).rbac().clusterRoleBindings().createOrReplace(clusterRoleBinding);
    }

}
