package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.BackupCR;
import com.middleware.zeus.integration.cluster.bean.BackupList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/4/6 4:41 下午
 */
@Component
@Slf4j
public class BackupWrapper {

    /**
     * 获取备份
     */
    public List<BackupCR> list(String clusterId, String namespace) {
        try {
            // init client
            NonNamespaceOperation<BackupCR, BackupList, Resource<BackupCR>> backupClient =
                K8sClient.getClient(clusterId).resources(BackupCR.class, BackupList.class);
            if ("*".equals(namespace)) {
                backupClient =
                    ((MixedOperation<BackupCR, BackupList, Resource<BackupCR>>)backupClient).inNamespace(namespace);
            }
            BackupList backupList = backupClient.list();
            if (backupList == null || CollectionUtils.isEmpty(backupList.getItems())) {
                return new ArrayList<>();
            }
            return backupList.getItems();
        } catch (Exception e) {
            log.error("查询mysql备份失败", e);
        }
        return new ArrayList<>();
    }


    /**
     * 创建备份
     */
    public void create(String clusterId, BackupCR backupCr) throws IOException {
        // init client
        NonNamespaceOperation<BackupCR, BackupList, Resource<BackupCR>> backupClient =
                K8sClient.getClient(clusterId).resources(BackupCR.class, BackupList.class);
        backupClient.resource(backupCr).create();
    }

    /**
     * 删除备份
     */
    public void delete(String clusterId, String namespace, String name) throws Exception {
        // init client
        NonNamespaceOperation<BackupCR, BackupList, Resource<BackupCR>> backupClient =
                K8sClient.getClient(clusterId).resources(BackupCR.class, BackupList.class).inNamespace(namespace);
        backupClient.withName(name).delete();
    }


}
