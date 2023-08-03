package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2023/8/3 3:47 下午
 */
@Slf4j
@Component
public class ClusterRoleWrapper {

    public void create(String clusterId, ClusterRole clusterRole){
        K8sClient.getClient(clusterId).rbac().clusterRoles().resource(clusterRole).create();
    }

}
