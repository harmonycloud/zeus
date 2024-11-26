package com.middleware.zeus.integration.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM middleware cr
 */
@Component
@Slf4j
public class MiddlewareWrapper {

    public List<MiddlewareCR> list(String clusterId, String namespace, Map<String, String> labels) {
        try {
            if (CollectionUtils.isEmpty(labels)){
                labels = new HashMap<>();
            }
            // init client
            NonNamespaceOperation<MiddlewareCR, MiddlewareList, Resource<MiddlewareCR>> middlewareClient =
                    K8sClient.getClient(clusterId).resources(MiddlewareCR.class, MiddlewareList.class);
            // 条件判断
            if (StringUtils.isNotEmpty(namespace)) {
                middlewareClient =
                    ((MixedOperation<MiddlewareCR, MiddlewareList, Resource<MiddlewareCR>>)middlewareClient)
                        .inNamespace(namespace);
            }
            // 查询middlewareList
            MiddlewareList middlewareList = middlewareClient.withLabels(labels).list();
            if (middlewareList == null || CollectionUtils.isEmpty(middlewareList.getItems())) {
                return new ArrayList<>(0);
            }
            return middlewareList.getItems();
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains("404")) {
                log.error("middleware controller not install");
                return new ArrayList<>();
            } else {
                throw e;
            }
        }
    }

    public MiddlewareCR get(String clusterId, String namespace, String name) {
        try {
            // init client
            NonNamespaceOperation<MiddlewareCR, MiddlewareList, Resource<MiddlewareCR>> middlewareClient =
                    K8sClient.getClient(clusterId).resources(MiddlewareCR.class, MiddlewareList.class);
            // 条件判断
            if (StringUtils.isNotEmpty(namespace)) {
                middlewareClient =
                        ((MixedOperation<MiddlewareCR, MiddlewareList, Resource<MiddlewareCR>>)middlewareClient)
                                .inNamespace(namespace);
            }
            return middlewareClient.withName(name).get();
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
            // init client
            NonNamespaceOperation<MiddlewareCR, MiddlewareList, Resource<MiddlewareCR>> middlewareClient =
                    K8sClient.getClient(clusterId).resources(MiddlewareCR.class, MiddlewareList.class).inNamespace(namespace);
            middlewareClient.withName(name).get();
            exist = true;
        } catch (KubernetesClientException e) {
            exist = false;
        }
        return exist;
    }
}
