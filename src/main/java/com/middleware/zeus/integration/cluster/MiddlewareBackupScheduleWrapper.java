package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupSchedule;
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
    public void create(String clusterId, MiddlewareBackupSchedule backupScheduleCr) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackupSchedule, MiddlewareBackupScheduleList,
            Resource<MiddlewareBackupSchedule>> backupScheduleClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupSchedule.class, MiddlewareBackupScheduleList.class);
        // create
        backupScheduleClient.resource(backupScheduleCr).createOrReplace();
    }

    public void createOrReplace(String clusterId, MiddlewareBackupSchedule backupScheduleCr) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackupSchedule, MiddlewareBackupScheduleList,
                Resource<MiddlewareBackupSchedule>> backupScheduleClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupSchedule.class, MiddlewareBackupScheduleList.class);
        MiddlewareBackupSchedule schedule = get(clusterId, backupScheduleCr.getMetadata().getNamespace(), backupScheduleCr.getMetadata().getName());
        if(schedule == null){
            backupScheduleClient.resource(backupScheduleCr).create();
        }else{
            backupScheduleClient.resource(backupScheduleCr).update();
        }
    }

    /**
     * 更新
     * 
     * @param clusterId
     * @param backupScheduleCr
     * @throws IOException
     */
    public void update(String clusterId, MiddlewareBackupSchedule backupScheduleCr) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackupSchedule, MiddlewareBackupScheduleList,
            Resource<MiddlewareBackupSchedule>> backupScheduleClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupSchedule.class, MiddlewareBackupScheduleList.class);
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
        NonNamespaceOperation<MiddlewareBackupSchedule, MiddlewareBackupScheduleList,
            Resource<MiddlewareBackupSchedule>> backupScheduleClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupSchedule.class, MiddlewareBackupScheduleList.class).inNamespace(namespace);
        // create
        backupScheduleClient.withName(name).delete();
    }

    /**
     * 删除
     * @param clusterId
     * @param namespace
     * @param name
     * @param forceDelete
     * @throws IOException
     */
    public void delete(String clusterId, String namespace, String name, Boolean forceDelete) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareBackupSchedule, MiddlewareBackupScheduleList,
                Resource<MiddlewareBackupSchedule>> backupScheduleClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareBackupSchedule.class, MiddlewareBackupScheduleList.class).inNamespace(namespace);
        if (forceDelete) {
            MiddlewareBackupSchedule schedule = get(clusterId, namespace, name);
            schedule.getMetadata().setFinalizers(Collections.emptyList());
            backupScheduleClient.resource(schedule).update();
        } else {
            backupScheduleClient.withName(name).delete();
        }
    }

    /**
     * 获取备份
     * 
     * @param clusterId
     * @param namespace
     * @param name
     * @return
     */
    public MiddlewareBackupSchedule get(String clusterId, String namespace, String name) {
        MiddlewareBackupSchedule middlewareBackupSchedule = null;
        try {
            // init client
            NonNamespaceOperation<MiddlewareBackupSchedule, MiddlewareBackupScheduleList,
                Resource<MiddlewareBackupSchedule>> backupScheduleClient = K8sClient.getClient(clusterId)
                    .resources(MiddlewareBackupSchedule.class, MiddlewareBackupScheduleList.class)
                    .inNamespace(namespace);
            middlewareBackupSchedule = backupScheduleClient.withName(name).get();
        } catch (Exception e) {
            log.error("查询MiddlewareBackupSchedule出错了", e);
            return null;
        }
        return middlewareBackupSchedule;
    }

    public MiddlewareBackupScheduleList list(String clusterId, String namespace, Map<String, String> labels) {
        MiddlewareBackupScheduleList middlewareBackupScheduleList = null;
        try {
            if (CollectionUtils.isEmpty(labels)){
                labels = new HashMap<>();
            }
            // init client
            NonNamespaceOperation<MiddlewareBackupSchedule, MiddlewareBackupScheduleList,
                Resource<MiddlewareBackupSchedule>> backupScheduleClient = K8sClient.getClient(clusterId)
                    .resources(MiddlewareBackupSchedule.class, MiddlewareBackupScheduleList.class);
            // 条件判断
            if (StringUtils.isNotEmpty(namespace) && !"*".equals(namespace)) {
                backupScheduleClient = ((MixedOperation<MiddlewareBackupSchedule, MiddlewareBackupScheduleList,
                        Resource<MiddlewareBackupSchedule>>) backupScheduleClient).inNamespace(namespace);
            }
            // list
            middlewareBackupScheduleList = backupScheduleClient.withLabels(labels).list();
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
