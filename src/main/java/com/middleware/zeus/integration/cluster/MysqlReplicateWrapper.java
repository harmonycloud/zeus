package com.middleware.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.integration.cluster.bean.MysqlReplicateCR;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * 疯转mysql复制的处理
 * @author liyinlong
 * @date 2021/8/11 2:22 下午
 */
@Slf4j
@Component
public class MysqlReplicateWrapper {

    /**
     * MysqlReplicate的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MIDDLEWARE_MYSQL_GROUP)
            .withVersion(MIDDLEWARE_INCLUDE_VERSION)
            .withScope(NAMESPACED)
            .withPlural(MYSQLREPLICATES)
            .build();

    /**
     * 创建mysql复制
     */
    public void create(String clusterId, MysqlReplicateCR mysqlReplicateCR) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).create(mysqlReplicateCR.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(mysqlReplicateCR)));
    }

    /**
     * 替换mysql复制
     */
    public void replace(String clusterId, MysqlReplicateCR mysqlReplicateCR) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(mysqlReplicateCR.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(mysqlReplicateCR)));
    }

    /**
     * 删除mysql复制
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
    }

    /**
     * 查询mysql复制
     */
    public MysqlReplicateCR getMysqlReplicate(String clusterId, String namespace, String name){
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
        } catch (Exception e) {
            log.error("查询mysql复制关系出错了");
            return null;
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), MysqlReplicateCR.class);
    }

}
