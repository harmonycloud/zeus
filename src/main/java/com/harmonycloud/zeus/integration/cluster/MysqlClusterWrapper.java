package com.harmonycloud.zeus.integration.cluster;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MYSQL_CLUSTER_GROUP;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MYSQL_CLUSTER_PLURAL;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MYSQL_CLUSTER_VERSION;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.NAMESPACED;

import java.io.IOException;
import java.util.Map;

import com.harmonycloud.zeus.integration.cluster.bean.MysqlCluster;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.util.K8sClient;
import com.harmonycloud.tool.collection.MapUtils;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Component
public class MysqlClusterWrapper {

    /**
     * crd的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MYSQL_CLUSTER_GROUP)
            .withVersion(MYSQL_CLUSTER_VERSION)
            .withScope(NAMESPACED)
            .withPlural(MYSQL_CLUSTER_PLURAL)
            .build();

    public MysqlCluster get(String clusterId, String namespace, String name) {
        // 获取所有的集群资源
        Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        JSONObject resObj = JSONObject.parseObject(JSONObject.toJSONString(map));
        System.out.println(JSONObject.toJSONString(resObj));
        return JSONObject.parseObject(JSONObject.toJSONString(resObj), MysqlCluster.class);
    }

    public void update(String clusterId, String namespace, MysqlCluster mysqlCluster) throws IOException {
        // 获取所有的集群资源
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(namespace,
            MapUtils.objectToMap(mysqlCluster));
    }

}
