package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.*;

import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @description 中间件备份记录
 * @author liyinlong
 * @since 2021/9/14 10:52 上午
 */
@Slf4j
@Component
public class MiddlewareBackupWrapper {

    /**
     * 创建备份(立即备份)
     * 
     * @param clusterId
     * @param middlewareBackupCR
     * @throws IOException
     */
    public void create(String clusterId, MiddlewareBackupCR middlewareBackupCR) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackupCR, MiddlewareBackupList,
            Resource<MiddlewareBackupCR>> middlewareBackupClient =
                K8sClient.getClient(clusterId).resources(MiddlewareBackupCR.class, MiddlewareBackupList.class);
        // create
        middlewareBackupClient.resource(middlewareBackupCR).create();
    }

    /**
     * 删除
     * 
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    public void delete(String clusterId, String namespace, String name) {
        // init client
        NonNamespaceOperation<MiddlewareBackupCR, MiddlewareBackupList,
            Resource<MiddlewareBackupCR>> middlewareBackupClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupCR.class, MiddlewareBackupList.class).inNamespace(namespace);
        // delete
        middlewareBackupClient.withName(name).delete();
    }

    /**
     * 查询备份记录列表
     * 
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    public List<MiddlewareBackupCR> list(String clusterId, String namespace, Map<String, String> labels) {
        MiddlewareBackupList middlewareBackupList = null;
        try {
            if (CollectionUtils.isEmpty(labels)){
                labels = new HashMap<>();
            }
            // init client
            NonNamespaceOperation<MiddlewareBackupCR, MiddlewareBackupList,
                Resource<MiddlewareBackupCR>> middlewareBackupClient =
                    K8sClient.getClient(clusterId).resources(MiddlewareBackupCR.class, MiddlewareBackupList.class);
            if (StringUtils.isNotEmpty(namespace)) {
                middlewareBackupClient = ((MixedOperation<MiddlewareBackupCR, MiddlewareBackupList,
                    Resource<MiddlewareBackupCR>>)middlewareBackupClient).inNamespace(namespace);
            }
            // list
            middlewareBackupList = middlewareBackupClient.withLabels(labels).list();
        } catch (Exception e) {
            log.error("查询MiddlewareBackupList出错了", e);
            return new ArrayList<>();
        }
        if (!CollectionUtils.isEmpty(middlewareBackupList.getItems())) {
            return middlewareBackupList.getItems();
        }
        return Collections.emptyList();
    }

    public MiddlewareBackupCR get(String clusterId, String namespace, String name) {
        MiddlewareBackupCR middlewareBackupCR = null;
        try {
            // init client
            NonNamespaceOperation<MiddlewareBackupCR, MiddlewareBackupList,
                Resource<MiddlewareBackupCR>> middlewareBackupClient = K8sClient.getClient(clusterId)
                    .resources(MiddlewareBackupCR.class, MiddlewareBackupList.class).inNamespace(namespace);
            middlewareBackupCR = middlewareBackupClient.withName(name).get();
        } catch (Exception e) {
            log.error("查询middlewareBackup出错了", e);
        }
        return middlewareBackupCR;
    }

}
