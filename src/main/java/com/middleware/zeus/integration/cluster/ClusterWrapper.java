package com.middleware.zeus.integration.cluster;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCluster;
import com.middleware.zeus.integration.cluster.bean.MiddlewareClusterList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * @author dengyulong
 * @date 2021/03/23 封装集群的处理
 */
@Component
@Deprecated
public class ClusterWrapper {

    @Autowired
    private K8sClient k8sClient;

    /**
     * crd的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
        .withGroup(MIDDLEWARE_CLUSTER_GROUP).withVersion(MIDDLEWARE_CLUSTER_VERSION).withScope(NAMESPACED)
        .withPlural(MIDDLEWARE_CLUSTER_PLURAL).build();

    /**
     * 查询集群列表
     */
    public List<MiddlewareCluster> listClusters() {
        GenericKubernetesResourceList resourceList =
            k8sClient.getDefaultClient().genericKubernetesResources(CONTEXT).list();
        MiddlewareClusterList middlewareClusterList =
            JSONObject.parseObject(JSONObject.toJSONString(resourceList), MiddlewareClusterList.class);
        if (middlewareClusterList != null && !CollectionUtils.isEmpty(middlewareClusterList.getItems())) {
            return middlewareClusterList.getItems();
        }
        return new ArrayList<>(0);
    }

    /**
     * 查询集群
     */
    public MiddlewareCluster get(String namespace, String name) {
        GenericKubernetesResource resource = k8sClient.getDefaultClient().genericKubernetesResources(CONTEXT)
            .inNamespace(namespace).withName(name).get();
        return JSONObject.parseObject(JSONObject.toJSONString(resource), MiddlewareCluster.class);
    }

    /**
     * 创建集群
     */
    public MiddlewareCluster create(MiddlewareCluster cluster) throws IOException {
        k8sClient.getDefaultClient().genericKubernetesResources(CONTEXT)
            .resource(JSONObject.parseObject(JSONObject.toJSONString(cluster), GenericKubernetesResource.class))
            .create();
        return null;
    }

    /**
     * 修改集群
     */
    public void update(MiddlewareCluster cluster) throws IOException {
        k8sClient.getDefaultClient().genericKubernetesResources(CONTEXT)
            .resource(JSONObject.parseObject(JSONObject.toJSONString(cluster), GenericKubernetesResource.class))
            .update();
    }

    /**
     * 删除集群
     */
    public void delete(String namespace, String name) throws IOException {
        k8sClient.getDefaultClient().genericKubernetesResources(CONTEXT).inNamespace(namespace).withName(name).delete();
    }

}
