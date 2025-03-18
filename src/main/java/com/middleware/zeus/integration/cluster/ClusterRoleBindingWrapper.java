package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2023/8/2 4:51 下午
 */
@Slf4j
@Component
public class ClusterRoleBindingWrapper {

    public ClusterRoleBinding get(String clusterId, String name){
        return K8sClient.getClient(clusterId).rbac().clusterRoleBindings().withName(name).get();
    }

    public void update(String clusterId, ClusterRoleBinding clusterRoleBinding){
        K8sClient.getClient(clusterId).rbac().clusterRoleBindings().resource(clusterRoleBinding).patch();
    }

    public void create(String clusterId, ClusterRoleBinding clusterRoleBinding){
        K8sClient.getClient(clusterId).rbac().clusterRoleBindings().resource(clusterRoleBinding).create();
    }

}
