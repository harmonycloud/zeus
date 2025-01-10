package com.middleware.zeus.integration.cluster;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.middleware.zeus.integration.cluster.bean.MysqlReplicateCR;
import com.middleware.zeus.integration.cluster.bean.MysqlReplicateList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 疯转mysql复制的处理
 * 
 * @author liyinlong
 * @date 2021/8/11 2:22 下午
 */
@Slf4j
@Component
public class MysqlReplicateWrapper {

    /**
     * 创建mysql复制
     */
    public void create(String clusterId, MysqlReplicateCR mysqlReplicateCr) throws IOException {
        // init client
        NonNamespaceOperation<MysqlReplicateCR, MysqlReplicateList, Resource<MysqlReplicateCR>> mysqlReplicateClient =
            K8sClient.getClient(clusterId).resources(MysqlReplicateCR.class, MysqlReplicateList.class);
        // create
        mysqlReplicateClient.resource(mysqlReplicateCr).create();
    }

    /**
     * 替换mysql复制
     */
    public void replace(String clusterId, MysqlReplicateCR mysqlReplicateCr) throws IOException {
        // init client
        NonNamespaceOperation<MysqlReplicateCR, MysqlReplicateList, Resource<MysqlReplicateCR>> mysqlReplicateClient =
            K8sClient.getClient(clusterId).resources(MysqlReplicateCR.class, MysqlReplicateList.class);
        // update
        mysqlReplicateClient.resource(mysqlReplicateCr).patch();
    }

    /**
     * 删除mysql复制
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        // init client
        NonNamespaceOperation<MysqlReplicateCR, MysqlReplicateList, Resource<MysqlReplicateCR>> mysqlReplicateClient =
            K8sClient.getClient(clusterId).resources(MysqlReplicateCR.class, MysqlReplicateList.class)
                .inNamespace(namespace);
        // delete
        mysqlReplicateClient.withName(name).delete();
    }

    /**
     * 查询mysql复制
     */
    public MysqlReplicateCR getMysqlReplicate(String clusterId, String namespace, String name) {
        MysqlReplicateCR mysqlReplicateCR = null;
        try {
            // init client
            NonNamespaceOperation<MysqlReplicateCR, MysqlReplicateList,
                Resource<MysqlReplicateCR>> mysqlReplicateClient = K8sClient.getClient(clusterId)
                    .resources(MysqlReplicateCR.class, MysqlReplicateList.class).inNamespace(namespace);
            mysqlReplicateClient.withName(name);
            mysqlReplicateCR = mysqlReplicateClient.withName(name).get();
        } catch (Exception e) {
            log.error("查询mysql复制关系出错了");
            return null;
        }
        return mysqlReplicateCR;
    }

}
