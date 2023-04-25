package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupScheduleCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupScheduleList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @description 中间件备份
 * @author liyinlong
 * @since 2021/9/14 10:52 上午
 */
@Slf4j
@Component
public class MiddlewareBackupScheduleWrapper {

    /**
     * 创建备份
     * 
     * @param clusterId
     * @param backupScheduleCr
     * @throws IOException
     */
    public void create(String clusterId, MiddlewareBackupScheduleCR backupScheduleCr) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackupScheduleCR, MiddlewareBackupScheduleList,
            Resource<MiddlewareBackupScheduleCR>> backupScheduleClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupScheduleCR.class, MiddlewareBackupScheduleList.class);
        // create
        backupScheduleClient.resource(backupScheduleCr).create();
    }

    /**
     * 更新
     * 
     * @param clusterId
     * @param backupScheduleCr
     * @throws IOException
     */
    public void update(String clusterId, MiddlewareBackupScheduleCR backupScheduleCr) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackupScheduleCR, MiddlewareBackupScheduleList,
            Resource<MiddlewareBackupScheduleCR>> backupScheduleClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupScheduleCR.class, MiddlewareBackupScheduleList.class);
        // create
        backupScheduleClient.resource(backupScheduleCr).update();
    }

    /**
     * 删除
     * 
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackupScheduleCR, MiddlewareBackupScheduleList,
            Resource<MiddlewareBackupScheduleCR>> backupScheduleClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupScheduleCR.class, MiddlewareBackupScheduleList.class).inNamespace(namespace);
        // create
        backupScheduleClient.withName(name).delete();
    }

    /**
     * 获取备份
     * 
     * @param clusterId
     * @param namespace
     * @param name
     * @return
     */
    public MiddlewareBackupScheduleCR get(String clusterId, String namespace, String name) {
        MiddlewareBackupScheduleCR middlewareBackupScheduleCR = null;
        try {
            // init client
            NonNamespaceOperation<MiddlewareBackupScheduleCR, MiddlewareBackupScheduleList,
                Resource<MiddlewareBackupScheduleCR>> backupScheduleClient = K8sClient.getClient(clusterId)
                    .resources(MiddlewareBackupScheduleCR.class, MiddlewareBackupScheduleList.class)
                    .inNamespace(namespace);
            middlewareBackupScheduleCR = backupScheduleClient.withName(name).get();
        } catch (Exception e) {
            log.error("查询MiddlewareBackupSchedule出错了", e);
            return null;
        }
        return middlewareBackupScheduleCR;
    }

    public MiddlewareBackupScheduleList list(String clusterId, String namespace, Map<String, String> labels) {
        MiddlewareBackupScheduleList middlewareBackupScheduleList = null;
        try {
            // init client
            NonNamespaceOperation<MiddlewareBackupScheduleCR, MiddlewareBackupScheduleList,
                Resource<MiddlewareBackupScheduleCR>> backupScheduleClient = K8sClient.getClient(clusterId)
                    .resources(MiddlewareBackupScheduleCR.class, MiddlewareBackupScheduleList.class)
                    .inNamespace(namespace);
            // 条件判断
            if (StringUtils.isNotEmpty(namespace)) {
                backupScheduleClient = ((MixedOperation<MiddlewareBackupScheduleCR, MiddlewareBackupScheduleList,
                    Resource<MiddlewareBackupScheduleCR>>)backupScheduleClient).inNamespace(namespace);
            }
            if (!CollectionUtils.isEmpty(labels)) {
                backupScheduleClient.withLabels(labels);
            }
            middlewareBackupScheduleList = backupScheduleClient.list();
        } catch (Exception e) {
            log.error("查询MiddlewareBackupScheduleList出错了");
            return null;
        }
        if (CollectionUtils.isEmpty(middlewareBackupScheduleList.getItems())) {
            return null;
        }
        return middlewareBackupScheduleList;
    }
}
