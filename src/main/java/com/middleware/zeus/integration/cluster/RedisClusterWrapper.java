package com.middleware.zeus.integration.cluster;

import org.springframework.stereotype.Component;

import com.middleware.zeus.integration.cluster.bean.RedisCluster;
import com.middleware.zeus.integration.cluster.bean.RedisClusterList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liyinlong
 * @since 2023/3/17 5:29 下午
 */
@Component
@Slf4j
public class RedisClusterWrapper {

    public RedisCluster get(String clusterId, String namespace, String name) {
        // init client
        NonNamespaceOperation<RedisCluster, RedisClusterList, Resource<RedisCluster>> redisClusterClient =
            K8sClient.getClient(clusterId).resources(RedisCluster.class, RedisClusterList.class).inNamespace(namespace);
        // get
        return redisClusterClient.withName(name).get();
    }

    public void update(String clusterId, String namespace, RedisCluster redisCluster) {
        // init client
        NonNamespaceOperation<RedisCluster, RedisClusterList, Resource<RedisCluster>> redisClusterClient =
            K8sClient.getClient(clusterId).resources(RedisCluster.class, RedisClusterList.class).inNamespace(namespace);
        // get
        redisClusterClient.resource(redisCluster).patch();
    }

}
