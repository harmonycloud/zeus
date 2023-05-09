package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.integration.cluster.bean.MiddlewareRestoreCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareRestoreList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 中间件恢复
 * 
 * @author liyinlong
 * @since 2021/9/15 5:16 下午
 */
@Slf4j
@Component
public class MiddlewareRestoreWrapper {

    public MiddlewareRestoreCR get(String clusterId, String namespace,String restoreName) {
        // init client
        NonNamespaceOperation<MiddlewareRestoreCR, MiddlewareRestoreList,
                Resource<MiddlewareRestoreCR>> middlewareRestoreClient =
                K8sClient.getClient(clusterId).resources(MiddlewareRestoreCR.class, MiddlewareRestoreList.class).inNamespace(namespace);
        // get
        return middlewareRestoreClient.withName(restoreName).get();
    }

    /**
     * 创建恢复
     * 
     * @param clusterId
     * @param middlewareRestoreCr
     * @throws IOException
     */
    public void create(String clusterId, MiddlewareRestoreCR middlewareRestoreCr) throws IOException {
        // init client
        NonNamespaceOperation<MiddlewareRestoreCR, MiddlewareRestoreList,
            Resource<MiddlewareRestoreCR>> middlewareRestoreClient =
                K8sClient.getClient(clusterId).resources(MiddlewareRestoreCR.class, MiddlewareRestoreList.class);
        // create
        middlewareRestoreClient.resource(middlewareRestoreCr).create();
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
        NonNamespaceOperation<MiddlewareRestoreCR, MiddlewareRestoreList,
            Resource<MiddlewareRestoreCR>> middlewareRestoreClient = K8sClient.getClient(clusterId)
                .resources(MiddlewareRestoreCR.class, MiddlewareRestoreList.class).inNamespace(namespace);
        // delete
        middlewareRestoreClient.withName(name).delete();
    }

    public MiddlewareRestoreList list(String clusterId, String namespace, Map<String, String> labels) {
        MiddlewareRestoreList middlewareRestoreList = null;
        try {
            if (CollectionUtils.isEmpty(labels)){
                labels = new HashMap<>();
            }
            // init client
            NonNamespaceOperation<MiddlewareRestoreCR, MiddlewareRestoreList,
                Resource<MiddlewareRestoreCR>> middlewareRestoreClient =
                    K8sClient.getClient(clusterId).resources(MiddlewareRestoreCR.class, MiddlewareRestoreList.class);
            if (StringUtils.isNotEmpty(namespace)) {
                middlewareRestoreClient = ((MixedOperation<MiddlewareRestoreCR, MiddlewareRestoreList,
                    Resource<MiddlewareRestoreCR>>)middlewareRestoreClient).inNamespace(namespace);
            }
            middlewareRestoreList = middlewareRestoreClient.withLabels(labels).list();
        } catch (Exception e) {
            log.error("查询MiddlewareRestoreList出错了", e);
            return null;
        }
        if (CollectionUtils.isEmpty(middlewareRestoreList.getItems())) {
            return null;
        }
        return middlewareRestoreList;
    }
}
