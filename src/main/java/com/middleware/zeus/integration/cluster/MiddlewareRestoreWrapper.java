package com.middleware.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.integration.cluster.bean.*;
import com.middleware.zeus.integration.cluster.bean.MiddlewareRestoreCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareRestoreList;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * 中间件恢复
 * @author  liyinlong
 * @since 2021/9/15 5:16 下午
 */
@Slf4j
@Component
public class MiddlewareRestoreWrapper {

    /**
     * MiddlewareRestore的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(CR_GROUP)
            .withVersion(V1)
            .withScope(NAMESPACED)
            .withPlural(MIDDLEWARERESTORES)
            .build();

    /**
     * 创建恢复
     * @param clusterId
     * @param middlewareBackupCRD
     * @throws IOException
     */
    public void create(String clusterId, MiddlewareRestoreCR middlewareBackupCRD) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).create(middlewareBackupCRD.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(middlewareBackupCRD)));
    }

    /**
     * 删除
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
    }

    public MiddlewareRestoreList list(String clusterId, String namespace, Map<String,String> labels){
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace, labels);
        } catch (Exception e) {
            log.error("查询MiddlewareRestoreList出错了", e);
            return null;
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareRestoreList.class);
    }
}
