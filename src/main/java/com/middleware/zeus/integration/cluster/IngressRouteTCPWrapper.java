package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.IngressRouteTCPCR;
import com.middleware.zeus.integration.cluster.bean.IngressRouteTCPList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @description
 * @author liyinlong
 * @since 2022/8/26 3:04 下午
 */
@Slf4j
@Component
public class IngressRouteTCPWrapper {

    /**
     * 创建
     * 
     * @param clusterId
     * @param ingressRouteTCPCR
     */
    public void create(String clusterId, IngressRouteTCPCR ingressRouteTCPCR) {
        try {
            // init client
            NonNamespaceOperation<IngressRouteTCPCR, IngressRouteTCPList, Resource<IngressRouteTCPCR>> ingressRouteClient =
                    K8sClient.getClient(clusterId).resources(IngressRouteTCPCR.class, IngressRouteTCPList.class);
            // create
            ingressRouteClient.resource(ingressRouteTCPCR).create();
        } catch (Exception e) {
            log.error("创建IngressRouteTCP出错了", e);
        }
    }

    /**
     *
     * @param clusterId
     * @param ingressRouteTCPCRList
     */
    public void benchCreate(String clusterId, List<IngressRouteTCPCR> ingressRouteTCPCRList) {
        try {
            for (IngressRouteTCPCR ingressRouteTCPCR : ingressRouteTCPCRList) {
                create(clusterId, ingressRouteTCPCR);
            }
        } catch (Exception e) {
            log.error("创建IngressRouteTCP出错了", e);
        }
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
        NonNamespaceOperation<IngressRouteTCPCR, IngressRouteTCPList, Resource<IngressRouteTCPCR>> ingressRouteClient =
                K8sClient.getClient(clusterId).resources(IngressRouteTCPCR.class, IngressRouteTCPList.class).inNamespace(namespace);
        // delete
        ingressRouteClient.withName(name).delete();
    }

    /**
     * 更新
     * 
     * @param clusterId
     * @param namespace
     * @param ingressRouteTCPCR
     * @throws IOException
     */
    public void update(String clusterId, String namespace, IngressRouteTCPCR ingressRouteTCPCR) throws IOException {
        // init client
        NonNamespaceOperation<IngressRouteTCPCR, IngressRouteTCPList, Resource<IngressRouteTCPCR>> ingressRouteClient =
                K8sClient.getClient(clusterId).resources(IngressRouteTCPCR.class, IngressRouteTCPList.class).inNamespace(namespace);
        // update
        ingressRouteClient.resource(ingressRouteTCPCR).update();
    }

    /**
     * 查询列表
     * 
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    public IngressRouteTCPList list(String clusterId, String namespace, Map<String, String> labels) {
        IngressRouteTCPList ingressRouteTCPList;
        try {
            if (CollectionUtils.isEmpty(labels)){
                labels = new HashMap<>();
            }
            // init client
            NonNamespaceOperation<IngressRouteTCPCR, IngressRouteTCPList,
                Resource<IngressRouteTCPCR>> ingressRouteClient =
                    K8sClient.getClient(clusterId).resources(IngressRouteTCPCR.class, IngressRouteTCPList.class);
            if (StringUtils.isNotEmpty(namespace)) {
                ingressRouteClient = ((MixedOperation<IngressRouteTCPCR, IngressRouteTCPList,
                    Resource<IngressRouteTCPCR>>)ingressRouteClient).inNamespace(namespace);
            }
            ingressRouteTCPList = ingressRouteClient.withLabels(labels).list();
        } catch (Exception e) {
            log.error("查询MiddlewareRestoreList出错了", e);
            return null;
        }
        return ingressRouteTCPList;
    }

}
