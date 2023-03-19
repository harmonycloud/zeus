package com.middleware.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.constants.RedisConstant;
import com.middleware.tool.collection.MapUtils;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.NAMESPACED;

/**
 * @author liyinlong
 * @since 2023/3/17 5:29 下午
 */
@Component
@Slf4j
public class RedisClusterWrapper {

    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(RedisConstant.REDISCLUSTER_GROUP)
            .withVersion(RedisConstant.REDISCLUSTER_VERSION)
            .withScope(NAMESPACED)
            .withPlural(RedisConstant.REDISCLUSTER_PLURAL)
            .build();

    public RedisCluster get(String clusterId, String namespace, String name) {
        // 获取所有的集群资源
        Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        JSONObject resObj = JSONObject.parseObject(JSONObject.toJSONString(map));
        return JSONObject.parseObject(JSONObject.toJSONString(resObj), RedisCluster.class);
    }

    public void update(String clusterId, String namespace, RedisCluster redisCluster) {
        // 获取所有的集群资源
        try {
            K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(namespace,
                    MapUtils.objectToMap(redisCluster));
        } catch (IOException e) {
            log.error("更新pg cr失败", e);
        }
    }

}
