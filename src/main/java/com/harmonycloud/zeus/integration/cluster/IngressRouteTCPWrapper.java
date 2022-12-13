package com.harmonycloud.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.middleware.tool.collection.MapUtils;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @description
 * @author  liyinlong
 * @since 2022/8/26 3:04 下午
 */
@Slf4j
@Component
public class IngressRouteTCPWrapper {

    /**
     * IngressRouteTCP的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup("traefik.containo.us")
            .withVersion("v1alpha1")
            .withScope(NAMESPACED)
            .withPlural("ingressroutetcps")
            .build();

    /**
     * 创建
     * @param clusterId
     * @param ingressRouteTCPCR
     */
    public void create(String clusterId, IngressRouteTCPCR ingressRouteTCPCR) {
        try {
            K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(ingressRouteTCPCR.getMetadata().getNamespace(),
                    JSONObject.parseObject(JSONObject.toJSONString(ingressRouteTCPCR)));
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
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    public void delete(String clusterId, String namespace, String name) {
        try {
            K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
        } catch (IOException e) {
            log.error("删除ingressroutetcp错误", name);
        }
    }

    /**
     * 更新
     * @param clusterId
     * @param namespace
     * @param ingressRouteTCPCR
     * @throws IOException
     */
    public void update(String clusterId, String namespace, IngressRouteTCPCR ingressRouteTCPCR) throws IOException {
        // 获取所有的集群资源
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(namespace,
                MapUtils.objectToMap(ingressRouteTCPCR));
    }

    /**
     * 查询列表
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    public IngressRouteTCPList list(String clusterId, String namespace, Map<String, String> labels) {
        Map<String, Object> map = null;
        try {
            if (namespace == null) {
                map = K8sClient.getClient(clusterId).customResource(CONTEXT).list();
            } else {
                map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace, labels);
            }
        } catch (Exception e) {
            log.error("查询MiddlewareRestoreList出错了", e);
            return null;
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), IngressRouteTCPList.class);
    }

}
