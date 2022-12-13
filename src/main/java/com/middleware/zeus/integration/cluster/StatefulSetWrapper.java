package com.middleware.zeus.integration.cluster;

import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.zeus.util.K8sClient;
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

    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(APPS)
            .withVersion(V1)
            .withScope(NAMESPACED)
            .withPlural(STATEFULSETS)
            .build();

    public List<LinkedHashMap> get(String clusterId, String namespace, String name) {
        try {
            KubernetesClient client = K8sClient.getClient(clusterId);
            Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);

            LinkedHashMap spec = (LinkedHashMap) map.get("spec");
            LinkedHashMap template = (LinkedHashMap) spec.get("template");
            LinkedHashMap templateSpec = (LinkedHashMap) template.get("spec");
            List<LinkedHashMap> containerList = (List<LinkedHashMap>) templateSpec.get("containers");
            return containerList;
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) {
                throw new BusinessException(DictEnum.MIDDLEWARE, name, ErrorMessage.NOT_EXIST);
            }
            throw e;
        }
    }
}
