package com.harmonycloud.zeus.integration.cluster;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_CLUSTER_GROUP;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_CLUSTER_PLURAL;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_CLUSTER_VERSION;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.NAMESPACED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareClusterList;
import com.harmonycloud.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 封装集群的处理
 */
@Component
public class ClusterWrapper {

    @Autowired
    private K8sClient k8sClient;

    /**
     * crd的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MIDDLEWARE_CLUSTER_GROUP)
            .withVersion(MIDDLEWARE_CLUSTER_VERSION)
            .withScope(NAMESPACED)
            .withPlural(MIDDLEWARE_CLUSTER_PLURAL)
            .build();

    /**
     * 查询集群列表
     */
    public List<MiddlewareCluster> listClusters() {
        Map<String, Object> map = k8sClient.getDefaultClient().customResource(CONTEXT).list();
        MiddlewareClusterList middlewareClusterList =
            JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareClusterList.class);
        if (middlewareClusterList != null && !CollectionUtils.isEmpty(middlewareClusterList.getItems())) {
            return middlewareClusterList.getItems();
        }
        return new ArrayList<>(0);
    }

    /**
     * 查询集群
     */
    public MiddlewareCluster get(String namespace, String name) {
        Map<String, Object> map = k8sClient.getDefaultClient().customResource(CONTEXT).get(namespace, name);
        return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareCluster.class);
    }

    /**
     * 创建集群
     */
    public MiddlewareCluster create(MiddlewareCluster cluster) throws IOException {
        Map<String, Object> c = k8sClient.getDefaultClient().customResource(CONTEXT).create(cluster.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(cluster)));
        return JSONObject.parseObject(JSONObject.toJSONString(c), MiddlewareCluster.class);
    }

    /**
     * 修改集群
     */
    public void update(MiddlewareCluster cluster) throws IOException {
        k8sClient.getDefaultClient().customResource(CONTEXT).createOrReplace(cluster.getMetadata().getNamespace(),
            JSONObject.parseObject(JSONObject.toJSONString(cluster)));
    }

    /**
     * 删除集群
     */
    public void delete(String namespace, String name) throws IOException {
        k8sClient.getDefaultClient().customResource(CONTEXT).delete(namespace, name);
    }



}
