package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.MysqlScheduleBackupCR;
import com.middleware.zeus.integration.cluster.bean.ScheduleBackupList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/4/2 3:14 下午
 */
@Slf4j
@Component
public class MysqlScheduleBackupWrapper {

    /**
     * 获取定时备份列表
     */
    public List<MysqlScheduleBackupCR> list(String clusterId, String namespace) {
        ScheduleBackupList scheduleBackupList = null;
        try {
            // init client
            NonNamespaceOperation<MysqlScheduleBackupCR, ScheduleBackupList,
                Resource<MysqlScheduleBackupCR>> mysqlScheduleClient =
                    K8sClient.getClient(clusterId).resources(MysqlScheduleBackupCR.class, ScheduleBackupList.class);
            // 条件判断
            if (StringUtils.isNotEmpty(namespace)) {
                mysqlScheduleClient = ((MixedOperation<MysqlScheduleBackupCR, ScheduleBackupList,
                    Resource<MysqlScheduleBackupCR>>)mysqlScheduleClient).inNamespace(namespace);
            }
            scheduleBackupList = mysqlScheduleClient.list();
        } catch (Exception e) {
            return null;
        }
        if (scheduleBackupList == null || CollectionUtils.isEmpty(scheduleBackupList.getItems())) {
            return null;
        }
        return scheduleBackupList.getItems();
    }

    /**
     * 创建定时备份
     */
    public void create(String clusterId, MysqlScheduleBackupCR mysqlScheduleBackupCR) throws IOException {
        // init client
        NonNamespaceOperation<MysqlScheduleBackupCR, ScheduleBackupList,
            Resource<MysqlScheduleBackupCR>> mysqlScheduleClient =
                K8sClient.getClient(clusterId).resources(MysqlScheduleBackupCR.class, ScheduleBackupList.class);
        // create
        mysqlScheduleClient.resource(mysqlScheduleBackupCR).create();
    }

    /**
     * 删除定时备份
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        // init client
        NonNamespaceOperation<MysqlScheduleBackupCR, ScheduleBackupList,
            Resource<MysqlScheduleBackupCR>> mysqlScheduleClient = K8sClient.getClient(clusterId)
                .resources(MysqlScheduleBackupCR.class, ScheduleBackupList.class).inNamespace(namespace);
        // delete
        mysqlScheduleClient.withName(name).delete();
    }

    /**
     * 更新定时备份
     * 
     * @param clusterId
     * @param mysqlScheduleBackupCR
     * @throws IOException
     */
    public void update(String clusterId, MysqlScheduleBackupCR mysqlScheduleBackupCR) throws IOException {
        // init client
        NonNamespaceOperation<MysqlScheduleBackupCR, ScheduleBackupList,
            Resource<MysqlScheduleBackupCR>> mysqlScheduleClient =
                K8sClient.getClient(clusterId).resources(MysqlScheduleBackupCR.class, ScheduleBackupList.class);
        // update
        mysqlScheduleClient.resource(mysqlScheduleBackupCR).patch();
    }

    /**
     * 查询备份规则
     * 
     * @param clusterId
     * @param namespace
     * @param backupScheduleName
     * @return
     */
    public MysqlScheduleBackupCR get(String clusterId, String namespace, String backupScheduleName) {
        // init client
        NonNamespaceOperation<MysqlScheduleBackupCR, ScheduleBackupList,
            Resource<MysqlScheduleBackupCR>> mysqlScheduleClient = K8sClient.getClient(clusterId)
                .resources(MysqlScheduleBackupCR.class, ScheduleBackupList.class).inNamespace(namespace);
        // get
        return mysqlScheduleClient.withName(backupScheduleName).get();
    }

}
