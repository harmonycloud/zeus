package com.middleware.zeus.integration.cluster;

import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.zeus.integration.cluster.bean.MysqlClusterSpec;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @description statefulset
 * @author: liyinlong
 * @date 2021/7/7 3:42 下午
 */
@Component
public class StatefulSetWrapper {

    public StatefulSet get(String clusterId, String namespace, String name) {
        try {
            return K8sClient.getClient(clusterId).apps().statefulSets().inNamespace(namespace).withName(name).get();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) {
                throw new BusinessException(DictEnum.STATEFULSET, name, ErrorMessage.NOT_EXIST);
            }
            throw e;
        }
    }

    public void delete(String clusterId, String namespace, String name) {
        K8sClient.getClient(clusterId).apps().statefulSets().inNamespace(namespace).withName(name).delete();
    }
}
