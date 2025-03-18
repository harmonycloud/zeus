package com.middleware.zeus.integration.cluster;

import static com.middleware.zeus.common.constants.NameConstant.FOUR_ZERO_FOUR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.Maintenance;
import com.middleware.zeus.integration.cluster.bean.MaintenanceList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/1/10 3:55 下午
 */
@Component
@Slf4j
public class MaintenanceWrapper {

    /**
     * 根据labels查询运维组件
     *
     * @param clusterId
     * @param namespace
     * @param labels
     */
    public List<Maintenance> listByLabels(String clusterId, String namespace, Map<String, String> labels) {
        MaintenanceList maintenanceList;
        try {
            if (CollectionUtils.isEmpty(labels)){
                labels = new HashMap<>();
            }
            // init client
            NonNamespaceOperation<Maintenance, MaintenanceList, Resource<Maintenance>> maintenanceClient =
                K8sClient.getClient(clusterId).resources(Maintenance.class, MaintenanceList.class);
            if (StringUtils.isNotEmpty(namespace)) {
                maintenanceClient =
                    ((MixedOperation<Maintenance, MaintenanceList, Resource<Maintenance>>)maintenanceClient)
                        .inNamespace(namespace);
            }
            maintenanceList = maintenanceClient.withLabels(labels).list();
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains(FOUR_ZERO_FOUR)) {
                log.error("Maintenance crd未部署");
            } else {
                throw e;
            }
            return null;
        }
        if (CollectionUtils.isEmpty(maintenanceList.getItems())) {
            return new ArrayList<>();
        }
        return maintenanceList.getItems();
    }

    public List<Maintenance> listByLabels(String clusterId, String namespace, Map<String, String> labels,
        String action) {
        List<Maintenance> maintenanceList = listByLabels(clusterId, namespace, labels);
        return maintenanceList.stream().filter(mt -> mt.getSpec().getAction().equals(action))
            .collect(Collectors.toList());
    }

    /**
     * 创建运维组件
     * 
     * @param clusterId
     * @param maintenance
     * @throws IOException
     */
    public void create(String clusterId, Maintenance maintenance) throws IOException {
        // init client
        NonNamespaceOperation<Maintenance, MaintenanceList, Resource<Maintenance>> maintenanceClient =
            K8sClient.getClient(clusterId).resources(Maintenance.class, MaintenanceList.class);
        // create
        maintenanceClient.resource(maintenance).create();
    }

    public List<Maintenance> list(String clusterId, String namespace) {
        try {
            // init client
            NonNamespaceOperation<Maintenance, MaintenanceList, Resource<Maintenance>> maintenanceClient =
                K8sClient.getClient(clusterId).resources(Maintenance.class, MaintenanceList.class);
            if (StringUtils.isNotEmpty(namespace)) {
                maintenanceClient =
                    ((MixedOperation<Maintenance, MaintenanceList, Resource<Maintenance>>)maintenanceClient)
                        .inNamespace(namespace);
            }
            MaintenanceList maintenanceList = maintenanceClient.list();
            if (maintenanceList == null || CollectionUtils.isEmpty(maintenanceList.getItems())) {
                return new ArrayList<>();
            }
            return maintenanceList.getItems();
        } catch (Exception e) {
            log.error("查询Maintenance失败", e);
        }
        return new ArrayList<>();
    }

    public void delete(String clusterId, String namespace, String name) throws IOException {
        // init client
        NonNamespaceOperation<Maintenance, MaintenanceList, Resource<Maintenance>> maintenanceClient =
            K8sClient.getClient(clusterId).resources(Maintenance.class, MaintenanceList.class).inNamespace(namespace);
        // delete
        maintenanceClient.withName(name).delete();
    }

    public void update(String clusterId, String namespace, Maintenance mt) throws IOException {
        // init client
        NonNamespaceOperation<Maintenance, MaintenanceList, Resource<Maintenance>> maintenanceClient =
            K8sClient.getClient(clusterId).resources(Maintenance.class, MaintenanceList.class).inNamespace(namespace);
        // delete
        maintenanceClient.resource(mt).patch();
    }

}
