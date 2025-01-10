package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.IngressRouteTcp;
import com.middleware.zeus.integration.cluster.bean.IngressRouteTcpList;
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
     * @param ingressRouteTcp
     */
    public void createOrUpdate(String clusterId, IngressRouteTcp ingressRouteTcp) {
        try {
            // init client
            NonNamespaceOperation<IngressRouteTcp, IngressRouteTcpList, Resource<IngressRouteTcp>> ingressRouteClient =
                    K8sClient.getClient(clusterId).resources(IngressRouteTcp.class, IngressRouteTcpList.class);
            IngressRouteTcp routeTcp = get(clusterId, ingressRouteTcp.getMetadata().getNamespace(), ingressRouteTcp.getMetadata().getName());
            if (routeTcp != null) {
                // update
                ingressRouteClient.resource(ingressRouteTcp).patch();
            } else {
                // create
                ingressRouteClient.resource(ingressRouteTcp).create();
            }
        } catch (Exception e) {
            log.error("创建IngressRouteTCP出错了", e);
        }
    }

    /**
     *
     * @param clusterId
     * @param ingressRouteTcpList
     */
    public void benchCreate(String clusterId, List<IngressRouteTcp> ingressRouteTcpList) {
        try {
            for (IngressRouteTcp ingressRouteTcp : ingressRouteTcpList) {
                createOrUpdate(clusterId, ingressRouteTcp);
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
        NonNamespaceOperation<IngressRouteTcp, IngressRouteTcpList, Resource<IngressRouteTcp>> ingressRouteClient =
                K8sClient.getClient(clusterId).resources(IngressRouteTcp.class, IngressRouteTcpList.class).inNamespace(namespace);
        // delete
        ingressRouteClient.withName(name).delete();
    }

    /**
     * 更新
     * 
     * @param clusterId
     * @param namespace
     * @param ingressRouteTcp
     * @throws IOException
     */
    public void update(String clusterId, String namespace, IngressRouteTcp ingressRouteTcp) throws IOException {
        // init client
        NonNamespaceOperation<IngressRouteTcp, IngressRouteTcpList, Resource<IngressRouteTcp>> ingressRouteClient =
                K8sClient.getClient(clusterId).resources(IngressRouteTcp.class, IngressRouteTcpList.class).inNamespace(namespace);
        // update
        ingressRouteClient.resource(ingressRouteTcp).patch();
    }

    /**
     * 查询i
     *
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    public IngressRouteTcp get(String clusterId, String namespace, String name) throws IOException {
        // init client
        NonNamespaceOperation<IngressRouteTcp, IngressRouteTcpList, Resource<IngressRouteTcp>> ingressRouteClient =
                K8sClient.getClient(clusterId).resources(IngressRouteTcp.class, IngressRouteTcpList.class).inNamespace(namespace);
        // update
        return ingressRouteClient.withName(name).get();
    }

    /**
     * 查询列表
     * 
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    public IngressRouteTcpList list(String clusterId, String namespace, Map<String, String> labels) {
        IngressRouteTcpList ingressRouteTCPList;
        try {
            if (CollectionUtils.isEmpty(labels)){
                labels = new HashMap<>();
            }
            // init client
            NonNamespaceOperation<IngressRouteTcp, IngressRouteTcpList,
                Resource<IngressRouteTcp>> ingressRouteClient =
                    K8sClient.getClient(clusterId).resources(IngressRouteTcp.class, IngressRouteTcpList.class);
            if (StringUtils.isNotEmpty(namespace)) {
                ingressRouteClient = ((MixedOperation<IngressRouteTcp, IngressRouteTcpList,
                    Resource<IngressRouteTcp>>)ingressRouteClient).inNamespace(namespace);
            }
            ingressRouteTCPList = ingressRouteClient.withLabels(labels).list();
        } catch (Exception e) {
            log.error("查询MiddlewareRestoreList出错了", e);
            return null;
        }
        return ingressRouteTCPList;
    }

}
