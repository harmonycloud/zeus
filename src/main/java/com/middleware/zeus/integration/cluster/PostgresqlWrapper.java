package com.middleware.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.constants.PostgresqlConstant;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareList;
import com.middleware.zeus.integration.cluster.bean.Postgresql;
import com.middleware.zeus.integration.cluster.bean.PostgresqlList;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.NAMESPACED;

/**
 * @author liyinlong
 * @since 2023/3/17 5:29 下午
 */
@Component
@Slf4j
public class PostgresqlWrapper {

    public Postgresql get(String clusterId, String namespace, String name) {
        // init client
        NonNamespaceOperation<Postgresql, PostgresqlList, Resource<Postgresql>> postgresqlClient =
            K8sClient.getClient(clusterId).resources(Postgresql.class, PostgresqlList.class).inNamespace(namespace);
        // get
        return postgresqlClient.withName(name).get();
    }

    public void update(String clusterId, String namespace, Postgresql postgresql) {
        // init client
        NonNamespaceOperation<Postgresql, PostgresqlList, Resource<Postgresql>> postgresqlClient =
            K8sClient.getClient(clusterId).resources(Postgresql.class, PostgresqlList.class).inNamespace(namespace);
        // update
        postgresqlClient.resource(postgresql).update();
    }

}
