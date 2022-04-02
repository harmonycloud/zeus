package com.harmonycloud.zeus.integration.cluster;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_CLUSTER_GROUP;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_CLUSTER_VERSION;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_PLURAL;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.NAMESPACED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareList;
import com.harmonycloud.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM
 * middleware cr
 */
@Component
@Slf4j
public class MiddlewareWrapper {

    /**
     * crd的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MIDDLEWARE_CLUSTER_GROUP)
            .withVersion(MIDDLEWARE_CLUSTER_VERSION)
            .withScope(NAMESPACED)
            .withPlural(MIDDLEWARE_PLURAL)
            .build();

    public List<MiddlewareCR> list(String clusterId, String namespace, Map<String, String> labels) {
        if (StringUtils.isBlank(namespace)) {
            namespace = null;
        }
        if (CollectionUtils.isEmpty(labels)) {
            labels = null;
        }
        // 获取所有的集群资源
        try {
            Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace, labels);
            MiddlewareList middlewareList = JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareList.class);
            if (middlewareList == null || CollectionUtils.isEmpty(middlewareList.getItems())) {
                return new ArrayList<>(0);
            }
            return middlewareList.getItems();
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains("404")) {
                throw new BusinessException(ErrorMessage.MIDDLEWARE_CONTROLLER_NOT_INSTALL);
            } else {
                throw e;
            }
        }
    }

    public MiddlewareCR get(String clusterId, String namespace, String name) {
        try {
            Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
            return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareCR.class);
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) {
                throw new BusinessException(DictEnum.MIDDLEWARE, name, ErrorMessage.NOT_EXIST);
            }
            throw e;
        }
    }

    public boolean checkIfExist(String clusterId, String namespace, String name) {
        boolean exist;
        try {
            Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
            exist = true;
        } catch (KubernetesClientException e) {
            exist = false;
        }
        return exist;
    }
}
