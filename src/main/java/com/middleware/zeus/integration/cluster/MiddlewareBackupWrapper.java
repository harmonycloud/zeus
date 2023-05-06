package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.*;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.dsl.internal.OperationContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.MiddlewareBackup;
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
     * @param middlewareBackup
     * @throws IOException
     */
    public void create(String clusterId, MiddlewareBackup middlewareBackup) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackup, MiddlewareBackupList,
            Resource<MiddlewareBackup>> middlewareBackupClient =
                K8sClient.getClient(clusterId).resources(MiddlewareBackup.class, MiddlewareBackupList.class);
        // create
        middlewareBackupClient.resource(middlewareBackup).create();
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
        delete(clusterId, namespace, name, false);
    }

    public void delete(String clusterId, String namespace, String name, Boolean forceDelete) {
        // init client
        NonNamespaceOperation<MiddlewareBackup, MiddlewareBackupList,
                Resource<MiddlewareBackup>> middlewareBackupClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackup.class, MiddlewareBackupList.class).inNamespace(namespace);
        // delete
        if (forceDelete) {
            middlewareBackupClient.withName(name).delete();
        } else {
            middlewareBackupClient.withPropagationPolicy(DeletionPropagation.FOREGROUND).withGracePeriod(0).delete();
        }
    }

    /**
     * 查询备份记录列表
     * 
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    public List<MiddlewareBackup> list(String clusterId, String namespace, Map<String, String> labels) {
        MiddlewareBackupList middlewareBackupList = null;
        try {
            if (CollectionUtils.isEmpty(labels)){
                labels = new HashMap<>();
            }
            // init client
            NonNamespaceOperation<MiddlewareBackup, MiddlewareBackupList,
                Resource<MiddlewareBackup>> middlewareBackupClient =
                    K8sClient.getClient(clusterId).resources(MiddlewareBackup.class, MiddlewareBackupList.class);
            if (StringUtils.isNotEmpty(namespace)) {
                middlewareBackupClient = ((MixedOperation<MiddlewareBackup, MiddlewareBackupList,
                    Resource<MiddlewareBackup>>)middlewareBackupClient).inNamespace(namespace);
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

    public MiddlewareBackup get(String clusterId, String namespace, String name) {
        MiddlewareBackup middlewareBackup = null;
        try {
            // init client
            NonNamespaceOperation<MiddlewareBackup, MiddlewareBackupList,
                Resource<MiddlewareBackup>> middlewareBackupClient = K8sClient.getClient(clusterId)
                    .resources(MiddlewareBackup.class, MiddlewareBackupList.class).inNamespace(namespace);
            middlewareBackup = middlewareBackupClient.withName(name).get();
        } catch (Exception e) {
            log.error("查询middlewareBackup出错了", e);
        }
        return middlewareBackup;
    }

}
