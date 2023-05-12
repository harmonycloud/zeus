package com.middleware.zeus.integration.cluster;

import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.springframework.stereotype.Component;

/**
 * @auther wangpenglei
 * @date 2023/5/12 15:25
 */
@Component
public class DeploymenentWrapper {

    public Deployment get(String clusterId, String namespace, String name) {
        try {
            return K8sClient.getClient(clusterId).apps().deployments().inNamespace(namespace).withName(name).get();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) {
                throw new BusinessException(DictEnum.STATEFULSET, name, ErrorMessage.NOT_EXIST);
            }
            throw e;
        }
    }

    public void delete(String clusterId, String namespace, String name) {
        K8sClient.getClient(clusterId).apps().deployments().inNamespace(namespace).withName(name).delete();
    }
}
