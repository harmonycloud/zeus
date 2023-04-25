package com.middleware.zeus.integration.cluster;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.middleware.zeus.integration.cluster.bean.MysqlCluster;
import com.middleware.zeus.integration.cluster.bean.MysqlClusterList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Component
public class MysqlClusterWrapper {

    @Autowired
    private K8sClient k8sClient;

    /**
     * crd的context
     */
    private static final ResourceDefinitionContext CONTEXT =
        new ResourceDefinitionContext.Builder().withGroup(MYSQL_CLUSTER_GROUP).withVersion(MYSQL_CLUSTER_VERSION)
            .withNamespaced(true).withPlural(MYSQL_CLUSTER_PLURAL).build();

    public MysqlCluster get(String clusterId, String namespace, String name) {
        // init client
        NonNamespaceOperation<MysqlCluster, MysqlClusterList, Resource<MysqlCluster>> mysqlClusterClient =
            K8sClient.getClient(clusterId).resources(MysqlCluster.class, MysqlClusterList.class).inNamespace(namespace);
        // get
        return mysqlClusterClient.withName(name).get();
    }

    public void update(String clusterId, String namespace, MysqlCluster mysqlCluster) throws IOException {
        // init client
        NonNamespaceOperation<MysqlCluster, MysqlClusterList, Resource<MysqlCluster>> mysqlClusterClient =
            K8sClient.getClient(clusterId).resources(MysqlCluster.class, MysqlClusterList.class).inNamespace(namespace);
        // update
        mysqlClusterClient.resource(mysqlCluster).update();
    }

    public MysqlCluster get(String namespace, String name) {
        // init client
        NonNamespaceOperation<MysqlCluster, MysqlClusterList, Resource<MysqlCluster>> mysqlClusterClient =
            k8sClient.getDefaultClient().resources(MysqlCluster.class, MysqlClusterList.class).inNamespace(namespace);
        // get
        return mysqlClusterClient.withName(name).get();
    }

}
